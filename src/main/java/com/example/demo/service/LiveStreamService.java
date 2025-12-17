package com.example.demo.service;

import com.example.demo.utils.StreamUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import com.example.demo.config.UserXtreamConfig;

import com.example.demo.config.UserXtreamConfig;
import com.example.demo.utils.StreamUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

/**
 * Service pour récupérer les live streams en temps réel
 * Avec API Xtream + Fallback M3U parsing
 * SANS STOCKAGE EN BASE DE DONNÉES
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LiveStreamService {

    private final WebClient webClient;
    private final UserContextService userContextService;

    /**
     * Récupère les live streams pour un utilisateur spécifique
     */
    public List<Map<String, Object>> fetchLiveStreamsForUser(String userId) {
        UserXtreamConfig config = userContextService.getUserXtreamConfigOrThrow(userId);
        return fetchLiveStreamsFromXtream(config);
    }

    /**
     * MÉTHODE 1 : Récupère depuis l'API Xtream
     */
    private List<Map<String, Object>> fetchLiveStreamsFromXtream(UserXtreamConfig config) {
        try {
            List<Map<String, Object>> streams = webClient.get()
                    .uri(config.getLiveStreamsUrl())
                    .retrieve()
                    .bodyToMono(List.class)
                    .block();

            log.info("✅ Récupéré {} live streams depuis Xtream API",
                    streams != null ? streams.size() : 0);

            if (streams != null && !streams.isEmpty()) {
                for (Map<String, Object> stream : streams) {
                    Integer streamId = StreamUtils.parseIntOrZero(stream.get("stream_id"));
                    if (streamId > 0) {
                        stream.put("stream_url", config.getLiveStreamUrl(streamId));
                    }
                }
                return streams;
            }

            log.warn("⚠️ API Xtream vide, tentative de fallback M3U...");
            return fetchFromM3U(config);

        } catch (Exception e) {
            log.error("❌ Erreur API Live Streams : {}, tentative de fallback M3U...", e.getMessage());
            return fetchFromM3U(config);
        }
    }

    /**
     * MÉTHODE 2 : Fallback M3U parsing
     */
    private List<Map<String, Object>> fetchFromM3U(UserXtreamConfig config) {
        try {
            log.info("📡 Tentative de récupération via M3U...");

            String m3uContent = webClient.get()
                    .uri(config.getM3uUrl())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            List<Map<String, Object>> streams = parseM3U(m3uContent);
            log.info("✅ Récupéré {} live streams depuis M3U", streams.size());
            return streams;

        } catch (Exception ex) {
            log.error("❌ Échec du fallback M3U: {}", ex.getMessage());
            throw new RuntimeException("Impossible de récupérer les live streams", ex);
        }
    }

    private List<Map<String, Object>> parseM3U(String m3uContent) {
        List<Map<String, Object>> streams = new ArrayList<>();
        if (m3uContent == null) return streams;

        String[] lines = m3uContent.split("\n");
        Map<String, Object> currentStream = null;

        for (String line : lines) {
            if (line.startsWith("#EXTINF:")) {
                if (isLiveStream(line)) {
                    currentStream = new HashMap<>();
                    String[] parts = line.split(",", 2);
                    if (parts.length == 2) {
                        currentStream.put("name", parts[1].trim());
                    }
                    extractM3UMetadata(line, currentStream);
                } else {
                    currentStream = null;
                }
            } else if (line.startsWith("http") && currentStream != null) {
                if (isValidLiveStreamUrl(line.trim())) {
                    currentStream.put("stream_url", line.trim());
                    currentStream.put("stream_id", StreamUtils.generateStreamId(line.trim()));
                    currentStream.put("category_id", StreamUtils.getCategoryId((String) currentStream.get("group_title")));
                    currentStream.put("category_name", currentStream.getOrDefault("group_title", "Live TV").toString());
                    streams.add(currentStream);
                }
                currentStream = null;
            }
        }

        return streams;
    }

    private void extractM3UMetadata(String extinf, Map<String, Object> stream) {
        String tvgId = StreamUtils.extractAttribute(extinf, "tvg-id");
        if (tvgId != null) stream.put("tvg_id", tvgId);

        String tvgLogo = StreamUtils.extractAttribute(extinf, "tvg-logo");
        if (tvgLogo != null) stream.put("stream_icon", tvgLogo);

        String groupTitle = StreamUtils.extractAttribute(extinf, "group-title");
        if (groupTitle != null) stream.put("group_title", groupTitle);

        String country = StreamUtils.extractAttribute(extinf, "tvg-country");
        if (country != null) stream.put("country", country);

        String language = StreamUtils.extractAttribute(extinf, "tvg-language");
        if (language != null) stream.put("language", language);
    }

    private boolean isLiveStream(String extinf) {
        if (extinf == null) return false;

        String line = extinf.toLowerCase();
        String[] vodPatterns = {
                "group-title=\"movies\"", "group-title=\"vod\"",
                "group-title=\"series\"", "group-title=\"tv shows\"",
                "group-title=\"films\"", "group-title=\"cinema\"",
                "season", "episode", "s01e", "s02e",
                "720p", "1080p", "4k", "bluray", "webrip"
        };

        for (String pattern : vodPatterns) {
            if (line.contains(pattern)) {
                return false;
            }
        }

        return true;
    }

    private boolean isValidLiveStreamUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        String lowerUrl = url.toLowerCase();
        String[] videoExtensions = {
                ".mp4", ".mkv", ".avi", ".mov", ".wmv",
                ".flv", ".webm", ".m4v", ".3gp"
        };

        for (String ext : videoExtensions) {
            if (lowerUrl.endsWith(ext)) {
                return false;
            }
        }

        return true;
    }

    public List<Map<String, Object>> searchLiveStreamsByName(String userId, String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> allStreams = fetchLiveStreamsForUser(userId);
        String lowerSearch = searchTerm.toLowerCase();

        return allStreams.stream()
                .filter(stream -> {
                    String name = StreamUtils.getStringSafely(stream, "name");
                    return name.toLowerCase().contains(lowerSearch);
                })
                .toList();
    }

    public List<Map<String, Object>> getLiveStreamsByCategory(String userId, String categoryName) {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> allStreams = fetchLiveStreamsForUser(userId);

        return allStreams.stream()
                .filter(stream -> {
                    String category = StreamUtils.getStringSafely(stream, "category_name");
                    return category.equalsIgnoreCase(categoryName);
                })
                .toList();
    }

    public List<String> getAvailableCategories(String userId) {
        List<Map<String, Object>> allStreams = fetchLiveStreamsForUser(userId);

        return allStreams.stream()
                .map(stream -> StreamUtils.getStringSafely(stream, "category_name"))
                .filter(category -> !category.isEmpty())
                .distinct()
                .sorted()
                .toList();
    }
}

