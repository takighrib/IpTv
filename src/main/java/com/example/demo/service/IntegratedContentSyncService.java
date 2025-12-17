package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service principal qui coordonne tous les services de synchronisation
 * avec support multi-utilisateur
 */
@Service
@RequiredArgsConstructor
public class IntegratedContentSyncService {

    private final LiveStreamService liveStreamService;
    private final VodService vodService;
    private final SeriesService seriesService;
    private final EpgService epgService;
    private final UserContextService userContextService;

    /**
     * Synchronise TOUT le contenu pour un utilisateur spécifique
     */
    @Transactional
    public ContentSyncResult syncAllContentForUser(String userId) {
        ContentSyncResult result = new ContentSyncResult();

        try {
            // Vérifier que l'utilisateur a une config Xtream valide
            if (!userContextService.hasValidXtreamConfig(userId)) {
                throw new RuntimeException("Configuration Xtream non trouvée ou invalide pour cet utilisateur");
            }

            System.out.println("🚀 Début de la synchronisation complète pour l'utilisateur " + userId);

            // 1. Synchroniser les Live Streams
            result.addLiveStreams(syncLiveStreamsForUser(userId));

            // 2. Synchroniser les VOD
            result.addVodContent(syncVodForUser(userId));

            // 3. Synchroniser les Séries
            result.addSeries(syncSeriesForUser(userId));

            // 4. Synchroniser les EPG pour les live streams (optionnel)
            if (!result.getLiveStreams().isEmpty()) {
                syncEpgForUserStreams(userId, result.getLiveStreams());
            }

            System.out.println("✅ Synchronisation complète terminée pour l'utilisateur " + userId);

        } catch (Exception e) {
            System.err.println("❌ Erreur synchronisation complète: " + e.getMessage());
            throw new RuntimeException("Échec de la synchronisation: " + e.getMessage(), e);
        }

        return result;
    }

    /**
     * Synchronise les Live Streams pour un utilisateur
     */
    public List<Map<String, Object>> syncLiveStreamsForUser(String userId) {
        try {
            List<Map<String, Object>> streams = liveStreamService.syncAndSaveLiveStreamsForUser(userId);
            System.out.println("✅ " + streams.size() + " live streams synchronisés pour l'utilisateur " + userId);
            return streams;
        } catch (Exception e) {
            System.err.println("❌ Erreur sync live streams: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Synchronise les VOD pour un utilisateur
     */
    public List<Map<String, Object>> syncVodForUser(String userId) {
        try {
            List<Map<String, Object>> vods = vodService.syncAndSaveVodStreamsForUser(userId);
            System.out.println("✅ " + vods.size() + " VOD synchronisés pour l'utilisateur " + userId);
            return vods;
        } catch (Exception e) {
            System.err.println("❌ Erreur sync VOD: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Synchronise les Séries pour un utilisateur
     */
    public List<Map<String, Object>> syncSeriesForUser(String userId) {
        try {
            List<Map<String, Object>> series = seriesService.syncAndSaveSeriesStreamsForUser(userId);
            System.out.println("✅ " + series.size() + " séries synchronisées pour l'utilisateur " + userId);
            return series;
        } catch (Exception e) {
            System.err.println("❌ Erreur sync séries: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Synchronise les EPG pour tous les live streams d'un utilisateur
     */
    public void syncEpgForUserStreams(String userId, List<Map<String, Object>> liveStreams) {
        if (liveStreams == null || liveStreams.isEmpty()) {
            System.out.println("⚠ Aucun live stream pour synchroniser les EPG");
            return;
        }

        int successCount = 0;
        int errorCount = 0;

        System.out.println("📺 Synchronisation des EPG pour " + liveStreams.size() + " chaînes...");

        // Limiter à 50 streams pour éviter de surcharger
        int maxStreamsToSync = Math.min(liveStreams.size(), 50);

        for (int i = 0; i < maxStreamsToSync; i++) {
            Map<String, Object> stream = liveStreams.get(i);
            try {
                Integer streamId = parseIntSafely(stream.get("stream_id"));
                if (streamId != null && streamId > 0) {
                    epgService.syncEpgForStreamForUser(userId, streamId);
                    successCount++;

                    // Log de progression tous les 10 streams
                    if (successCount % 10 == 0) {
                        System.out.println("📊 EPG synchronisés: " + successCount + "/" + maxStreamsToSync);
                    }
                }
            } catch (Exception e) {
                errorCount++;
                System.err.println("❌ Erreur EPG pour stream " + stream.get("stream_id") + ": " + e.getMessage());
            }
        }

        System.out.println("📺 EPG terminé: " + successCount + " réussis, " + errorCount + " erreurs");

        if (liveStreams.size() > maxStreamsToSync) {
            System.out.println("⚠ Limité à " + maxStreamsToSync + " streams sur " + liveStreams.size() + " disponibles");
        }
    }

    /**
     * Synchronise l'EPG pour un stream spécifique
     */
    public Map<String, Object> syncEpgForSingleStream(String userId, Integer streamId) {
        try {
            EpgService.EpgSyncResult result = epgService.syncEpgForStreamForUser(userId, streamId);

            return Map.of(
                    "success", true,
                    "message", result.getMessage(),
                    "stream_id", result.getStreamId(),
                    "epg_count", result.getEntriesCount()
            );

        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "error", "❌ Erreur sync EPG pour stream " + streamId,
                    "details", e.getMessage()
            );
        }
    }

    /**
     * Obtient des statistiques complètes pour un utilisateur
     */
    public Map<String, Object> getComprehensiveStatsForUser(String userId) {
        Map<String, Object> stats = new HashMap<>();

        try {
            stats.put("message", "📊 Statistiques complètes du contenu");
            stats.put("user_id", userId);
            stats.put("has_xtream_config", userContextService.hasValidXtreamConfig(userId));

            stats.put("services_status", Map.of(
                    "live_stream_service", "✅ Actif",
                    "vod_service", "✅ Actif",
                    "series_service", "✅ Actif",
                    "epg_service", "✅ Actif"
            ));

            stats.put("data_sources", Map.of(
                    "primary", "Xtream Codes API (utilisateur)",
                    "fallback", "M3U Parsing avec classification intelligente"
            ));

            // Vous pouvez ajouter des compteurs ici si vous avez des repositories avec count()
            // stats.put("counts", Map.of(
            //     "live_streams", liveStreamRepository.count(),
            //     "vod_content", vodRepository.count(),
            //     "series", seriesRepository.count(),
            //     "epg_entries", epgRepository.count()
            // ));

        } catch (Exception e) {
            stats.put("error", "Erreur lors de la récupération des statistiques");
            stats.put("details", e.getMessage());
        }

        return stats;
    }

    /**
     * Synchronise seulement les Live Streams (méthode rapide)
     */
    public ContentSyncResult syncLiveStreamsOnly(String userId) {
        ContentSyncResult result = new ContentSyncResult();
        result.addLiveStreams(syncLiveStreamsForUser(userId));
        return result;
    }

    /**
     * Synchronise seulement les VOD (méthode rapide)
     */
    public ContentSyncResult syncVodOnly(String userId) {
        ContentSyncResult result = new ContentSyncResult();
        result.addVodContent(syncVodForUser(userId));
        return result;
    }

    /**
     * Synchronise seulement les Séries (méthode rapide)
     */
    public ContentSyncResult syncSeriesOnly(String userId) {
        ContentSyncResult result = new ContentSyncResult();
        result.addSeries(syncSeriesForUser(userId));
        return result;
    }

    /**
     * Vérifie si un utilisateur peut synchroniser du contenu
     */
    public boolean canUserSync(String userId) {
        return userContextService.hasValidXtreamConfig(userId);
    }

    // Méthode utilitaire
    private Integer parseIntSafely(Object value) {
        if (value == null) return null;
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Classe pour encapsuler les résultats de synchronisation
     */
    public static class ContentSyncResult {
        private List<Map<String, Object>> liveStreams = new ArrayList<>();
        private List<Map<String, Object>> vodContent = new ArrayList<>();
        private List<Map<String, Object>> series = new ArrayList<>();
        private int epgEntriesCount = 0;

        // Getters
        public List<Map<String, Object>> getLiveStreams() { return liveStreams; }
        public List<Map<String, Object>> getVodContent() { return vodContent; }
        public List<Map<String, Object>> getSeries() { return series; }
        public int getEpgEntriesCount() { return epgEntriesCount; }

        // Setters
        public void addLiveStreams(List<Map<String, Object>> streams) {
            if (streams != null) this.liveStreams.addAll(streams);
        }

        public void addVodContent(List<Map<String, Object>> vod) {
            if (vod != null) this.vodContent.addAll(vod);
        }

        public void addSeries(List<Map<String, Object>> series) {
            if (series != null) this.series.addAll(series);
        }

        public void setEpgEntriesCount(int count) {
            this.epgEntriesCount = count;
        }

        // Statistiques
        public int getTotalContentCount() {
            return liveStreams.size() + vodContent.size() + series.size();
        }

        public Map<String, Object> getStatistics() {
            Map<String, Object> stats = new HashMap<>();
            stats.put("live_streams", liveStreams.size());
            stats.put("vod_content", vodContent.size());
            stats.put("series", series.size());
            stats.put("epg_entries", epgEntriesCount);
            stats.put("total_content", getTotalContentCount());
            return stats;
        }

        @Override
        public String toString() {
            return String.format("ContentSyncResult{live=%d, vod=%d, series=%d, epg=%d}",
                    liveStreams.size(), vodContent.size(), series.size(), epgEntriesCount);
        }
    }
}