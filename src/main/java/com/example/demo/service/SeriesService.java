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
public class SeriesService {

    private final WebClient webClient;
    private final UserContextService userContextService;

    public List<Map<String, Object>> fetchSeriesStreamsForUser(String userId) {
        UserXtreamConfig config = userContextService.getUserXtreamConfigOrThrow(userId);
        return fetchSeriesStreams(config);
    }

    private List<Map<String, Object>> fetchSeriesStreams(UserXtreamConfig config) {
        try {
            List<Map<String, Object>> series = webClient.get()
                    .uri(config.getSeriesUrl())
                    .retrieve()
                    .bodyToMono(List.class)
                    .block();

            log.info("✅ Récupéré {} séries depuis Xtream API",
                    series != null ? series.size() : 0);

            if (series != null && !series.isEmpty()) {
                for (Map<String, Object> serie : series) {
                    Integer seriesId = StreamUtils.parseIntOrZero(serie.get("series_id"));
                    String extension = StreamUtils.getStringSafely(serie, "container_extension");
                    if (seriesId > 0 && !extension.isEmpty()) {
                        serie.put("stream_url", config.getSeriesStreamUrl(seriesId, extension));
                    }
                }
                return series;
            }

            log.warn("⚠️ API Xtream vide, tentative de fallback M3U...");
            return fetchSeriesFromM3U(config);

        } catch (Exception e) {
            log.error("❌ Erreur API Séries : {}, tentative de fallback M3U...", e.getMessage());
            return fetchSeriesFromM3U(config);
        }
    }

    private List<Map<String, Object>> fetchSeriesFromM3U(UserXtreamConfig config) {
        try {
            log.info("📡 Tentative de récupération séries via M3U...");

            String m3uContent = webClient.get()
                    .uri(config.getM3uUrl())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            List<Map<String, Object>> series = parseSeriesFromM3U(m3uContent);
            log.info("✅ Récupéré {} épisodes de séries depuis M3U", series.size());
            return series;

        } catch (Exception ex) {
            log.error("❌ Échec du fallback M3U: {}", ex.getMessage());
            throw new RuntimeException("Impossible de récupérer les séries", ex);
        }
    }

    private List<Map<String, Object>> parseSeriesFromM3U(String m3uContent) {
        List<Map<String, Object>> series = new ArrayList<>();
        if (m3uContent == null) return series;

        String[] lines = m3uContent.split("\n");
        Map<String, Object> currentSeries = null;

        for (String line : lines) {
            if (line.startsWith("#EXTINF:")) {
                if (isSeriesContent(line)) {
                    currentSeries = new HashMap<>();
                    String[] parts = line.split(",", 2);
                    if (parts.length == 2) {
                        String title = parts[1].trim();
                        currentSeries.put("name", title);
                        extractSeriesMetadata(title, line, currentSeries);
                    }
                } else {
                    currentSeries = null;
                }
            } else if (line.startsWith("http") && currentSeries != null) {
                if (isValidSeriesUrl(line.trim())) {
                    currentSeries.put("stream_url", line.trim());
                    currentSeries.put("series_id", StreamUtils.generateStreamId(line.trim()));
                    currentSeries.put("category_id", StreamUtils.getCategoryId((String) currentSeries.get("group_title")));
                    currentSeries.put("category_name",
                            currentSeries.getOrDefault("group_title", "TV Series").toString());
                    series.add(currentSeries);
                }
                currentSeries = null;
            }
        }

        return series;
    }

    private boolean isSeriesContent(String extinf) {
        if (extinf == null) return false;

        String line = extinf.toLowerCase();
        String[] seriesPatterns = {
                "group-title=\"series\"", "group-title=\"tv shows\"",
                "group-title=\"shows\"", "group-title=\"serie\"",
                "group-title=\"tv series\"", "season", "episode",
                "s\\d{1,2}e\\d{1,3}", "s\\d{2}e\\d{2}",
                "saison", "épisode", "ep\\d+", "parte"
        };

        for (String pattern : seriesPatterns) {
            if (Pattern.compile(pattern).matcher(line).find()) {
                return true;
            }
        }

        return false;
    }

    private boolean isValidSeriesUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        String lowerUrl = url.toLowerCase();
        return lowerUrl.contains("/series/") ||
                lowerUrl.contains("/episode/") ||
                lowerUrl.matches(".*s\\d+e\\d+.*");
    }

    private void extractSeriesMetadata(String title, String extinf, Map<String, Object> series) {
        String groupTitle = StreamUtils.extractAttribute(extinf, "group-title");
        if (groupTitle != null) {
            series.put("group_title", groupTitle);
        }

        String logo = StreamUtils.extractAttribute(extinf, "tvg-logo");
        if (logo != null) series.put("stream_icon", logo);
    }

    public List<Map<String, Object>> searchSeriesByName(String userId, String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> allSeries = fetchSeriesStreamsForUser(userId);
        String lowerSearch = searchTerm.toLowerCase();

        return allSeries.stream()
                .filter(serie -> {
                    String name = StreamUtils.getStringSafely(serie, "name");
                    return name.toLowerCase().contains(lowerSearch);
                })
                .toList();
    }

    public List<Map<String, Object>> getSeriesByCategory(String userId, String categoryName) {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> allSeries = fetchSeriesStreamsForUser(userId);

        return allSeries.stream()
                .filter(serie -> {
                    String category = StreamUtils.getStringSafely(serie, "category_name");
                    return category.equalsIgnoreCase(categoryName);
                })
                .toList();
    }

    public List<String> getAvailableCategories(String userId) {
        List<Map<String, Object>> allSeries = fetchSeriesStreamsForUser(userId);

        return allSeries.stream()
                .map(serie -> StreamUtils.getStringSafely(serie, "category_name"))
                .filter(category -> !category.isEmpty())
                .distinct()
                .sorted()
                .toList();
    }

    public List<String> getAllSeriesNames(String userId) {
        List<Map<String, Object>> allSeries = fetchSeriesStreamsForUser(userId);

        return allSeries.stream()
                .map(serie -> StreamUtils.getStringSafely(serie, "name"))
                .filter(name -> !name.isEmpty())
                .distinct()
                .sorted()
                .toList();
    }

    public Map<String, Object> getSeriesDetails(String userId, Integer seriesId) {
        List<Map<String, Object>> allSeries = fetchSeriesStreamsForUser(userId);

        return allSeries.stream()
                .filter(serie -> {
                    Integer id = StreamUtils.parseIntOrZero(serie.get("series_id"));
                    return id != null && id.equals(seriesId);
                })
                .findFirst()
                .orElse(null);
    }
}