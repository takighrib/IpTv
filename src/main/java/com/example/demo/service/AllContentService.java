package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service pour récupérer TOUT le contenu IPTV en une seule requête
 * Combine Live Streams, VOD et Séries
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AllContentService {

    private final LiveStreamService liveStreamService;
    private final VodService vodService;
    private final SeriesService seriesService;

    /**
     * Récupère TOUT le contenu pour un utilisateur
     */
    public Map<String, Object> getAllContentForUser(String userId) {
        log.info("🔄 Récupération de TOUT le contenu pour userId: {}", userId);

        long startTime = System.currentTimeMillis();

        Map<String, Object> result = new LinkedHashMap<>();

        try {
            // 1. Récupérer les Live Streams
            log.info("📺 Récupération des Live Streams...");
            List<Map<String, Object>> liveStreams = liveStreamService.fetchLiveStreamsForUser(userId);
            result.put("liveStreams", liveStreams);
            result.put("liveStreamsCount", liveStreams.size());
            log.info("✅ {} Live Streams récupérés", liveStreams.size());

        } catch (Exception e) {
            log.error("❌ Erreur Live Streams: {}", e.getMessage());
            result.put("liveStreams", new ArrayList<>());
            result.put("liveStreamsCount", 0);
            result.put("liveStreamsError", e.getMessage());
        }

        try {
            // 2. Récupérer les VOD
            log.info("🎬 Récupération des VOD...");
            List<Map<String, Object>> vods = vodService.fetchVodStreamsForUser(userId);
            result.put("vods", vods);
            result.put("vodsCount", vods.size());
            log.info("✅ {} VOD récupérés", vods.size());

        } catch (Exception e) {
            log.error("❌ Erreur VOD: {}", e.getMessage());
            result.put("vods", new ArrayList<>());
            result.put("vodsCount", 0);
            result.put("vodsError", e.getMessage());
        }

        try {
            // 3. Récupérer les Séries
            log.info("📺 Récupération des Séries...");
            List<Map<String, Object>> series = seriesService.fetchSeriesStreamsForUser(userId);
            result.put("series", series);
            result.put("seriesCount", series.size());
            log.info("✅ {} épisodes de séries récupérés", series.size());

        } catch (Exception e) {
            log.error("❌ Erreur Séries: {}", e.getMessage());
            result.put("series", new ArrayList<>());
            result.put("seriesCount", 0);
            result.put("seriesError", e.getMessage());
        }

        // 4. Statistiques globales
        int totalCount =
                (int) result.getOrDefault("liveStreamsCount", 0) +
                        (int) result.getOrDefault("vodsCount", 0) +
                        (int) result.getOrDefault("seriesCount", 0);

        result.put("totalCount", totalCount);

        long duration = System.currentTimeMillis() - startTime;
        result.put("fetchDurationMs", duration);

        log.info("✅ Tout le contenu récupéré en {}ms - Total: {} items", duration, totalCount);

        return result;
    }


    /**
     * Recherche dans TOUT le contenu
     */
    public Map<String, Object> searchAllContent(String userId, String query) {
        log.info("🔍 Recherche globale: '{}' pour userId: {}", query, userId);

        Map<String, Object> results = new LinkedHashMap<>();

        if (query == null || query.trim().isEmpty()) {
            results.put("error", "Le terme de recherche est vide");
            results.put("totalResults", 0);
            return results;
        }

        String lowerQuery = query.toLowerCase();

        try {
            // Recherche dans Live Streams
            List<Map<String, Object>> liveResults = liveStreamService.searchLiveStreamsByName(userId, lowerQuery);
            results.put("liveStreams", liveResults);
            results.put("liveStreamsCount", liveResults.size());
            log.info("✅ {} Live Streams trouvés", liveResults.size());
        } catch (Exception e) {
            log.error("❌ Erreur recherche Live: {}", e.getMessage());
            results.put("liveStreams", new ArrayList<>());
            results.put("liveStreamsCount", 0);
        }

        try {
            // Recherche dans VOD
            List<Map<String, Object>> vodResults = vodService.searchVodByTitle(userId, lowerQuery);
            results.put("vods", vodResults);
            results.put("vodsCount", vodResults.size());
            log.info("✅ {} VOD trouvés", vodResults.size());
        } catch (Exception e) {
            log.error("❌ Erreur recherche VOD: {}", e.getMessage());
            results.put("vods", new ArrayList<>());
            results.put("vodsCount", 0);
        }

        try {
            // Recherche dans Séries
            List<Map<String, Object>> seriesResults = seriesService.searchSeriesByName(userId, lowerQuery);
            results.put("series", seriesResults);
            results.put("seriesCount", seriesResults.size());
            log.info("✅ {} épisodes de séries trouvés", seriesResults.size());
        } catch (Exception e) {
            log.error("❌ Erreur recherche Séries: {}", e.getMessage());
            results.put("series", new ArrayList<>());
            results.put("seriesCount", 0);
        }

        int totalResults =
                (int) results.getOrDefault("liveStreamsCount", 0) +
                        (int) results.getOrDefault("vodsCount", 0) +
                        (int) results.getOrDefault("seriesCount", 0);

        results.put("query", query);
        results.put("totalResults", totalResults);

        log.info("✅ Recherche terminée: {} résultats au total", totalResults);

        return results;
    }

    /**
     * Récupère toutes les catégories disponibles
     */
    public Map<String, Object> getAllCategories(String userId) {
        log.info("📂 Récupération de toutes les catégories pour userId: {}", userId);

        Map<String, Object> categories = new LinkedHashMap<>();

        try {
            List<String> liveCategories = liveStreamService.getAvailableCategories(userId);
            categories.put("liveCategories", liveCategories);
            categories.put("liveCategoriesCount", liveCategories.size());
        } catch (Exception e) {
            categories.put("liveCategories", new ArrayList<>());
            categories.put("liveCategoriesCount", 0);
        }

        try {
            List<String> vodCategories = vodService.getAvailableCategories(userId);
            categories.put("vodCategories", vodCategories);
            categories.put("vodCategoriesCount", vodCategories.size());
        } catch (Exception e) {
            categories.put("vodCategories", new ArrayList<>());
            categories.put("vodCategoriesCount", 0);
        }

        try {
            List<String> seriesCategories = seriesService.getAvailableCategories(userId);
            categories.put("seriesCategories", seriesCategories);
            categories.put("seriesCategoriesCount", seriesCategories.size());
        } catch (Exception e) {
            categories.put("seriesCategories", new ArrayList<>());
            categories.put("seriesCategoriesCount", 0);
        }

        return categories;
    }
}