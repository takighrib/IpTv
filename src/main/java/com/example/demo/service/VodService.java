package com.example.demo.service;

import com.example.demo.config.UserXtreamConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.demo.config.UserXtreamConfig;
import com.example.demo.utils.StreamUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class VodService {

    private final WebClient webClient;
    private final UserContextService userContextService;

    public List<Map<String, Object>> fetchVodStreamsForUser(String userId) {
        UserXtreamConfig config = userContextService.getUserXtreamConfigOrThrow(userId);
        return fetchVodStreams(config);
    }

    private List<Map<String, Object>> fetchVodStreams(UserXtreamConfig config) {
        try {
            List<Map<String, Object>> vods = webClient.get()
                    .uri(config.getVodStreamsUrl())
                    .retrieve()
                    .bodyToMono(List.class)
                    .block();

            log.info("✅ Récupéré {} VOD depuis Xtream API",
                    vods != null ? vods.size() : 0);

            if (vods != null && !vods.isEmpty()) {
                for (Map<String, Object> vod : vods) {
                    Integer vodId = StreamUtils.parseIntOrZero(vod.get("stream_id"));
                    String extension = StreamUtils.getStringSafely(vod, "container_extension");
                    if (vodId > 0 && !extension.isEmpty()) {
                        vod.put("stream_url", config.getVodStreamUrl(vodId, extension));
                    }
                }
                return vods;
            }

            log.warn("⚠️ API Xtream vide, tentative de fallback M3U...");
            return fetchVodFromM3U(config);

        } catch (Exception e) {
            log.error("❌ Erreur API VOD : {}, tentative de fallback M3U...", e.getMessage());
            return fetchVodFromM3U(config);
        }
    }

    private List<Map<String, Object>> fetchVodFromM3U(UserXtreamConfig config) {
        try {
            log.info("📡 Tentative de récupération VOD via M3U...");

            String m3uContent = webClient.get()
                    .uri(config.getM3uUrl())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            List<Map<String, Object>> vods = parseVodFromM3U(m3uContent);
            log.info("✅ Récupéré {} VOD depuis M3U", vods.size());
            return vods;

        } catch (Exception ex) {
            log.error("❌ Échec du fallback M3U: {}", ex.getMessage());
            throw new RuntimeException("Impossible de récupérer les VOD", ex);
        }
    }

    private List<Map<String, Object>> parseVodFromM3U(String m3uContent) {
        List<Map<String, Object>> vods = new ArrayList<>();
        if (m3uContent == null) return vods;

        String[] lines = m3uContent.split("\n");
        Map<String, Object> currentVod = null;

        for (String line : lines) {
            if (line.startsWith("#EXTINF:")) {
                if (isVodContent(line)) {
                    currentVod = new HashMap<>();
                    String[] parts = line.split(",", 2);
                    if (parts.length == 2) {
                        String title = parts[1].trim();
                        currentVod.put("name", title);
                        extractVodMetadata(title, line, currentVod);
                    }
                } else {
                    currentVod = null;
                }
            } else if (line.startsWith("http") && currentVod != null) {
                if (isValidVodUrl(line.trim())) {
                    currentVod.put("stream_url", line.trim());
                    currentVod.put("stream_id", StreamUtils.generateStreamId(line.trim()));
                    currentVod.put("category_id", StreamUtils.getCategoryId((String) currentVod.get("group_title")));
                    currentVod.put("category_name", currentVod.getOrDefault("group_title", "Movies").toString());
                    vods.add(currentVod);
                }
                currentVod = null;
            }
        }

        return vods;
    }

    private boolean isVodContent(String extinf) {
        if (extinf == null) return false;

        String line = extinf.toLowerCase();
        String[] vodPatterns = {
                "group-title=\"movies\"", "group-title=\"vod\"",
                "group-title=\"films\"", "group-title=\"cinema\"",
                "group-title=\"movie\"", "group-title=\"film\"",
                "720p", "1080p", "4k", "bluray", "webrip", "dvdrip",
                "hdtv", "web-dl", "brrip", "\\(\\d{4}\\)"
        };

        for (String pattern : vodPatterns) {
            if (Pattern.compile(pattern).matcher(line).find()) {
                return true;
            }
        }

        return false;
    }

    private boolean isValidVodUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        String lowerUrl = url.toLowerCase();
        String[] vodExtensions = {
                ".mp4", ".mkv", ".avi", ".mov", ".wmv",
                ".flv", ".webm", ".m4v"
        };

        for (String ext : vodExtensions) {
            if (lowerUrl.endsWith(ext)) {
                return true;
            }
        }

        return lowerUrl.contains("/movie/") || lowerUrl.contains("/vod/");
    }

    private void extractVodMetadata(String title, String extinf, Map<String, Object> vod) {
        String groupTitle = StreamUtils.extractAttribute(extinf, "group-title");
        if (groupTitle != null) {
            vod.put("group_title", groupTitle);
        }

        String logo = StreamUtils.extractAttribute(extinf, "tvg-logo");
        if (logo != null) vod.put("stream_icon", logo);
    }

    public List<Map<String, Object>> searchVodByTitle(String userId, String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> allVods = fetchVodStreamsForUser(userId);
        String lowerSearch = searchTerm.toLowerCase();

        return allVods.stream()
                .filter(vod -> {
                    String name = StreamUtils.getStringSafely(vod, "name");
                    return name.toLowerCase().contains(lowerSearch);
                })
                .toList();
    }

    public List<Map<String, Object>> getVodByCategory(String userId, String categoryName) {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> allVods = fetchVodStreamsForUser(userId);

        return allVods.stream()
                .filter(vod -> {
                    String category = StreamUtils.getStringSafely(vod, "category_name");
                    return category.equalsIgnoreCase(categoryName);
                })
                .toList();
    }

    public List<String> getAvailableCategories(String userId) {
        List<Map<String, Object>> allVods = fetchVodStreamsForUser(userId);

        return allVods.stream()
                .map(vod -> StreamUtils.getStringSafely(vod, "category_name"))
                .filter(category -> !category.isEmpty())
                .distinct()
                .sorted()
                .toList();
    }

    public Map<String, Object> getVodDetails(String userId, Integer vodId) {
        List<Map<String, Object>> allVods = fetchVodStreamsForUser(userId);

        return allVods.stream()
                .filter(vod -> {
                    Integer id = StreamUtils.parseIntOrZero(vod.get("stream_id"));
                    return id != null && id.equals(vodId);
                })
                .findFirst()
                .orElse(null);
    }
}

