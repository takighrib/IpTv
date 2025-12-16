package com.example.demo.service;


import com.example.demo.config.XtreamConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service principal qui coordonne tous vos services existants
 * et ajoute la fonctionnalité de parsing M3U intelligent
 */
@Service
@RequiredArgsConstructor
public class IntegratedContentSyncService {

    // Vos services existants
    private final LiveStreamService liveStreamService;
    private final VodService vodService;
    private final SeriesService seriesService;
    private final EpgService epgService;
    private final M3UFallbackService m3uFallbackService;
    private final XtreamConfig xtreamConfig; // ✅ Injecter la config


    /**
     * Synchronise TOUT le contenu depuis Xtream avec fallback M3U intelligent
     */
    @Transactional
    public ContentSyncResult syncAllContent() {
        ContentSyncResult result = new ContentSyncResult();

        try {
            // 1. Essaie les APIs Xtream existantes
            System.out.println("🚀 Début de la synchronisation complète...");

            result.addLiveStreams(syncLiveStreamsFromXtream());
            result.addVodContent(syncVodFromXtream());
            result.addSeries(syncSeriesFromXtream());

            // 2. Synchronise les EPG pour les live streams
            syncEpgForAllStreams(result.getLiveStreams());

            System.out.println("✅ Synchronisation Xtream terminée");

        } catch (Exception e) {
            System.err.println("⚠ Erreur APIs Xtream, fallback vers M3U : " + e.getMessage());

            // 3. Fallback intelligent avec parsing M3U
            result = syncFromM3UWithIntelligentSeparation();
        }

        return result;
    }
    /**
     * Synchronise les Live Streams via votre service existant
     */
    public List<Map<String, Object>> syncLiveStreamsFromXtream() {
        try {
            List<Map<String, Object>> streams = liveStreamService.fetchLiveStreamsFromXtream();
            liveStreamService.saveLiveStreams(streams);
            System.out.println("✅ " + streams.size() + " live streams synchronisés");
            return streams;
        } catch (Exception e) {
            System.err.println("❌ Erreur sync live streams: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Synchronise les VOD via votre service existant
     */
    public List<Map<String, Object>> syncVodFromXtream() {
        try {
            List<Map<String, Object>> vods = vodService.fetchVodStreams();
            vodService.saveVodStreams(vods);
            System.out.println("✅ " + vods.size() + " VOD synchronisés");
            return vods;
        } catch (Exception e) {
            System.err.println("❌ Erreur sync VOD: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Synchronise les Séries via votre service existant
     */
    public List<Map<String, Object>> syncSeriesFromXtream() {
        try {
            List<Map<String, Object>> series = seriesService.fetchSeriesStreams();
            seriesService.saveSeriesStreams(series);
            System.out.println("✅ " + series.size() + " séries synchronisées");
            return series;
        } catch (Exception e) {
            System.err.println("❌ Erreur sync séries: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Synchronise les EPG pour tous les live streams
     */
    public void syncEpgForAllStreams(List<Map<String, Object>> liveStreams) {
        if (liveStreams == null || liveStreams.isEmpty()) {
            System.out.println("⚠ Aucun live stream pour synchroniser les EPG");
            return;
        }

        int successCount = 0;
        int errorCount = 0;

        System.out.println("📺 Synchronisation des EPG pour " + liveStreams.size() + " chaînes...");

        for (Map<String, Object> stream : liveStreams) {
            try {
                Integer streamId = parseIntSafely(stream.get("stream_id"));
                if (streamId != null && streamId > 0) {
                    epgService.syncEpgForStream(streamId);
                    successCount++;

                    // Log de progression tous les 50 streams
                    if (successCount % 50 == 0) {
                        System.out.println("📊 EPG synchronisés: " + successCount + "/" + liveStreams.size());
                    }
                }
            } catch (Exception e) {
                errorCount++;
                System.err.println("❌ Erreur EPG pour stream " + stream.get("stream_id") + ": " + e.getMessage());
            }
        }

        System.out.println("📺 EPG terminé: " + successCount + " réussis, " + errorCount + " erreurs");
    }

    /**
     * Fallback intelligent : Parse le M3U et sépare par type
     */
    public ContentSyncResult syncFromM3UWithIntelligentSeparation() {
        String m3uUrl = xtreamConfig.getM3uUrl();

        ContentSyncResult result = new ContentSyncResult();

        try {
            System.out.println("🔄 Parsing intelligent du fichier M3U...");

            List<M3UFallbackService.ContentItem> items = m3uFallbackService
                    .fetchM3UContent(m3uUrl)
                    .collectList()
                    .block();

            if (items == null) items = new ArrayList<>();

            // Sépare intelligemment par type
            Map<M3UFallbackService.ContentType, List<M3UFallbackService.ContentItem>> grouped =
                    items.stream().collect(Collectors.groupingBy(M3UFallbackService.ContentItem::getType));

            // Convertit et sauvegarde via vos services existants
            List<Map<String, Object>> liveStreams = convertAndSaveLiveStreams(
                    grouped.getOrDefault(M3UFallbackService.ContentType.LIVE_STREAM, new ArrayList<>()));

            List<Map<String, Object>> vodContent = convertAndSaveVod(
                    grouped.getOrDefault(M3UFallbackService.ContentType.VOD, new ArrayList<>()));

            List<Map<String, Object>> series = convertAndSaveSeries(
                    grouped.getOrDefault(M3UFallbackService.ContentType.SERIES, new ArrayList<>()));

            result.addLiveStreams(liveStreams);
            result.addVodContent(vodContent);
            result.addSeries(series);

            // Synchronise les EPG pour les live streams du M3U
            if (!liveStreams.isEmpty()) {
                syncEpgForM3UStreams(liveStreams);
            }

            System.out.println("📊 Parsing M3U terminé :");
            System.out.println("  - Live Streams: " + liveStreams.size());
            System.out.println("  - VOD: " + vodContent.size());
            System.out.println("  - Séries: " + series.size());

        } catch (Exception ex) {
            throw new RuntimeException("Impossible de parser le M3U intelligemment", ex);
        }

        return result;
    }

    /**
     * Convertit les ContentItem en Live Streams et les sauvegarde
     */
    private List<Map<String, Object>> convertAndSaveLiveStreams(List<M3UFallbackService.ContentItem> items) {
        List<Map<String, Object>> streams = items.stream().map(item -> {
            Map<String, Object> stream = new HashMap<>();
            stream.put("stream_id", generateStreamId(item.getUrl()));
            stream.put("name", item.getName());
            stream.put("stream_url", item.getUrl());
            stream.put("stream_icon", item.getTvgLogo());
            stream.put("category_name", item.getGroupTitle() != null ? item.getGroupTitle() : "Live TV");
            stream.put("category_id", getCategoryId(item.getGroupTitle()));
            stream.put("tvg_id", item.getTvgId());
            stream.put("country", item.getTvgCountry());
            stream.put("language", item.getTvgLanguage());
            return stream;
        }).collect(Collectors.toList());

        // Sauvegarde via votre service existant
        liveStreamService.saveLiveStreams(streams);

        return streams;
    }

    /**
     * Convertit les ContentItem en VOD et les sauvegarde
     */
    private List<Map<String, Object>> convertAndSaveVod(List<M3UFallbackService.ContentItem> items) {
        List<Map<String, Object>> vods = items.stream().map(item -> {
            Map<String, Object> vod = new HashMap<>();
            vod.put("stream_id", generateStreamId(item.getUrl()));
            vod.put("name", item.getName());
            vod.put("stream_url", item.getUrl());
            vod.put("stream_icon", item.getTvgLogo());
            vod.put("category_name", item.getGroupTitle() != null ? item.getGroupTitle() : "Movies");
            vod.put("category_id", getCategoryId(item.getGroupTitle()));
            return vod;
        }).collect(Collectors.toList());

        // Sauvegarde via votre service existant
        vodService.saveVodStreams(vods);

        return vods;
    }

    /**
     * Convertit les ContentItem en Séries et les sauvegarde
     */
    private List<Map<String, Object>> convertAndSaveSeries(List<M3UFallbackService.ContentItem> items) {
        List<Map<String, Object>> series = items.stream().map(item -> {
            Map<String, Object> serie = new HashMap<>();
            serie.put("series_id", generateStreamId(item.getUrl()));
            serie.put("name", item.getName());
            serie.put("stream_url", item.getUrl());
            serie.put("stream_icon", item.getTvgLogo());
            serie.put("category_name", item.getGroupTitle() != null ? item.getGroupTitle() : "TV Series");
            serie.put("category_id", getCategoryId(item.getGroupTitle()));
            return serie;
        }).collect(Collectors.toList());

        // Sauvegarde via votre service existant
        seriesService.saveSeriesStreams(series);

        return series;
    }

    /**
     * Synchronise les EPG spécifiquement pour les streams venant du M3U
     * (Plus complexe car les IDs peuvent être différents)
     */
    private void syncEpgForM3UStreams(List<Map<String, Object>> liveStreams) {
        System.out.println("📺 Tentative de synchronisation EPG pour streams M3U...");

        // Pour les streams M3U, on essaie de trouver les EPG en utilisant les TVG-ID
        int attempted = 0;
        int successful = 0;

        for (Map<String, Object> stream : liveStreams) {
            try {
                Integer streamId = parseIntSafely(stream.get("stream_id"));
                String tvgId = (String) stream.get("tvg_id");

                if (streamId != null && streamId > 0) {
                    attempted++;

                    // Essaie de récupérer l'EPG avec l'ID généré
                    List<Map<String, Object>> epgData = epgService.fetchEpgForStream(streamId);

                    if (epgData != null && !epgData.isEmpty()) {
                        epgService.saveEpgForStream(streamId, epgData);
                        successful++;
                    }
                }

                // Limite pour éviter de surcharger l'API
                if (attempted >= 100) {
                    System.out.println("⚠ Limite de 100 tentatives EPG atteinte pour les streams M3U");
                    break;
                }

            } catch (Exception e) {
                // Continue silencieusement car les EPG M3U sont souvent indisponibles
            }
        }

        System.out.println("📺 EPG M3U: " + successful + " réussis sur " + attempted + " tentatives");
    }

    /**
     * Synchronise uniquement les EPG pour un stream spécifique
     */
    public Map<String, Object> syncEpgForSingleStream(Integer streamId) {
        try {
            List<Map<String, Object>> epgData = epgService.fetchEpgForStream(streamId);
            epgService.saveEpgForStream(streamId, epgData);

            return Map.of(
                    "message", "✅ EPG synchronisé pour le stream " + streamId,
                    "stream_id", streamId,
                    "epg_count", epgData != null ? epgData.size() : 0
            );

        } catch (Exception e) {
            return Map.of(
                    "error", "❌ Erreur sync EPG pour stream " + streamId,
                    "details", e.getMessage()
            );
        }
    }

    /**
     * Obtient des statistiques complètes
     */
    public Map<String, Object> getComprehensiveStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            // TODO: Ajouter des méthodes count() dans vos repositories
            stats.put("message", "📊 Statistiques complètes du contenu");
            stats.put("services_status", Map.of(
                    "live_stream_service", "✅ Actif",
                    "vod_service", "✅ Actif",
                    "series_service", "✅ Actif",
                    "epg_service", "✅ Actif",
                    "m3u_fallback", "✅ Actif"
            ));

            stats.put("data_sources", Map.of(
                    "primary", "Xtream Codes API",
                    "fallback", "M3U Parsing avec classification intelligente"
            ));

            // stats.put("counts", Map.of(
            //     "live_streams", liveStreamRepository.count(),
            //     "vod_content", vodRepository.count(),
            //     "series", seriesRepository.count(),
            //     "epg_entries", epgRepository.count()
            // ));

        } catch (Exception e) {
            stats.put("error", "Erreur lors de la récupération des statistiques");
        }

        return stats;
    }

    // Méthodes utilitaires
    private Integer parseIntSafely(Object value) {
        if (value == null) return null;
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int generateStreamId(String url) {
        return Math.abs(url.hashCode());
    }

    private int getCategoryId(String groupTitle) {
        if (groupTitle == null) return 0;
        return Math.abs(groupTitle.hashCode()) % 1000;
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