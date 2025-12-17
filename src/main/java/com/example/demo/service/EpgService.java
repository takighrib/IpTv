package com.example.demo.service;

import com.example.demo.config.UserXtreamConfig;
import com.example.demo.model.Epg;
import com.example.demo.repository.EpgRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class EpgService {

    private final EpgRepository epgRepository;
    private final WebClient webClient;
    private final UserContextService userContextService;

    /**
     * Récupère l'EPG pour un stream spécifique pour un utilisateur
     */
    public List<Map<String, Object>> fetchEpgForStreamForUser(String userId, Integer streamId) {
        UserXtreamConfig config = userContextService.getUserXtreamConfigOrThrow(userId);
        return fetchEpgForStream(config, streamId);
    }

    /**
     * Récupère l'EPG pour un stream spécifique
     */
    private List<Map<String, Object>> fetchEpgForStream(UserXtreamConfig config, Integer streamId) {
        try {
            List<Map<String, Object>> response = webClient.get()
                    .uri(config.getEpgUrl(streamId))
                    .retrieve()
                    .bodyToMono(List.class)
                    .block();

            System.out.println("✅ Récupéré EPG pour stream " + streamId + ": " +
                    (response != null ? response.size() : 0) + " programmes");
            return response != null ? response : new ArrayList<>();

        } catch (Exception e) {
            System.err.println("❌ Erreur fetch EPG pour stream " + streamId + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Récupère l'EPG complet pour un utilisateur
     */
    public List<Map<String, Object>> fetchFullEpgForUser(String userId) {
        UserXtreamConfig config = userContextService.getUserXtreamConfigOrThrow(userId);
        return fetchFullEpg(config);
    }

    /**
     * Récupère l'EPG complet pour toutes les chaînes
     */
    private List<Map<String, Object>> fetchFullEpg(UserXtreamConfig config) {
        try {
            List<Map<String, Object>> response = webClient.get()
                    .uri(config.getFullEpgUrl())
                    .retrieve()
                    .bodyToMono(List.class)
                    .block();

            System.out.println("✅ Récupéré EPG complet: " +
                    (response != null ? response.size() : 0) + " programmes");
            return response != null ? response : new ArrayList<>();

        } catch (Exception e) {
            System.err.println("❌ Erreur fetch EPG complet: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Sauvegarde l'EPG pour un stream avec validation et parsing amélioré
     */
    public void saveEpgForStream(Integer streamId, List<Map<String, Object>> epgList) {
        if (epgList == null || epgList.isEmpty()) {
            return;
        }

        int savedCount = 0;
        int errorCount = 0;

        for (Map<String, Object> e : epgList) {
            try {
                // Validation des données essentielles
                String title = (String) e.get("title");
                String start = (String) e.get("start");
                String end = (String) e.get("end");

                if (title == null || title.trim().isEmpty()) {
                    continue; // Ignore les entrées sans titre
                }

                Epg epg = Epg.builder()
                        .streamId(streamId)
                        .title(title.trim())
                        .start(start)
                        .end(end)
                        .description(getStringSafely(e, "description", "desc"))
                        .build();

                epgRepository.save(epg);
                savedCount++;

            } catch (Exception ex) {
                errorCount++;
                System.err.println("❌ Erreur sauvegarde EPG: " + ex.getMessage());
            }
        }

        System.out.println("📺 EPG Stream " + streamId + ": " + savedCount + " sauvegardés, " + errorCount + " erreurs");
    }

    /**
     * Synchronise l'EPG pour un stream pour un utilisateur
     */
    public EpgSyncResult syncEpgForStreamForUser(String userId, Integer streamId) {
        try {
            List<Map<String, Object>> epgList = fetchEpgForStreamForUser(userId, streamId);

            if (epgList.isEmpty()) {
                return new EpgSyncResult(streamId, 0, "Aucune donnée EPG disponible");
            }

            saveEpgForStream(streamId, epgList);

            return new EpgSyncResult(streamId, epgList.size(), "✅ EPG synchronisé avec succès");

        } catch (Exception e) {
            return new EpgSyncResult(streamId, 0, "❌ Erreur: " + e.getMessage());
        }
    }

    /**
     * Synchronise l'EPG pour plusieurs streams en lot pour un utilisateur
     */
    public List<EpgSyncResult> syncEpgForMultipleStreamsForUser(String userId, List<Integer> streamIds) {
        List<EpgSyncResult> results = new ArrayList<>();

        System.out.println("📺 Synchronisation EPG pour " + streamIds.size() + " streams...");

        for (int i = 0; i < streamIds.size(); i++) {
            Integer streamId = streamIds.get(i);

            try {
                EpgSyncResult result = syncEpgForStreamForUser(userId, streamId);
                results.add(result);

                // Progression tous les 20 streams
                if ((i + 1) % 20 == 0) {
                    System.out.println("📊 Progression EPG: " + (i + 1) + "/" + streamIds.size());
                }

                // Pause pour éviter de surcharger l'API
                Thread.sleep(100);

            } catch (Exception e) {
                results.add(new EpgSyncResult(streamId, 0, "❌ Erreur: " + e.getMessage()));
            }
        }

        return results;
    }

    /**
     * Nettoie les anciennes entrées EPG (plus anciennes que X jours)
     */
    public int cleanOldEpgEntries(int daysOld) {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);

            // TODO: Ajouter cette méthode dans EpgRepository
            // int deletedCount = epgRepository.deleteByEndTimeBefore(cutoffDate);

            int deletedCount = 0; // Placeholder
            System.out.println("🗑️ Nettoyage EPG: " + deletedCount + " anciennes entrées supprimées");

            return deletedCount;

        } catch (Exception e) {
            System.err.println("❌ Erreur nettoyage EPG: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Obtient l'EPG actuel pour un stream
     */
    public Map<String, Object> getCurrentProgramForStream(Integer streamId) {
        try {
            // TODO: Ajouter cette méthode dans EpgRepository
            // Optional<Epg> currentProgram = epgRepository.findCurrentProgramByStreamId(streamId);

            Map<String, Object> result = new HashMap<>();
            result.put("stream_id", streamId);
            // result.put("current_program", currentProgram.orElse(null));
            result.put("message", "Programme actuel récupéré");

            return result;

        } catch (Exception e) {
            return Map.of(
                    "stream_id", streamId,
                    "error", "❌ Erreur récupération programme actuel: " + e.getMessage()
            );
        }
    }

    /**
     * Obtient les prochains programmes pour un stream
     */
    public List<Map<String, Object>> getUpcomingProgramsForStream(Integer streamId, int hours) {
        try {
            // TODO: Ajouter cette méthode dans EpgRepository
            // LocalDateTime endTime = LocalDateTime.now().plusHours(hours);
            // List<Epg> upcomingPrograms = epgRepository.findUpcomingProgramsByStreamId(streamId, endTime);

            List<Map<String, Object>> result = new ArrayList<>();
            // Conversion des entités Epg en Map

            return result;

        } catch (Exception e) {
            System.err.println("❌ Erreur récupération programmes à venir: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Statistiques EPG
     */
    public Map<String, Object> getEpgStatistics() {
        Map<String, Object> stats = new HashMap<>();

        try {
            // TODO: Ajouter ces méthodes dans EpgRepository
            // long totalEntries = epgRepository.count();
            // long todayEntries = epgRepository.countTodayEntries();
            // long streamsWithEpg = epgRepository.countDistinctStreamsWithEpg();

            stats.put("total_entries", 0); // totalEntries
            stats.put("today_entries", 0); // todayEntries
            stats.put("streams_with_epg", 0); // streamsWithEpg
            stats.put("last_update", LocalDateTime.now());

        } catch (Exception e) {
            stats.put("error", "Erreur récupération statistiques EPG");
        }

        return stats;
    }

    // Méthodes utilitaires
    private String getStringSafely(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null && !value.toString().trim().isEmpty()) {
                return value.toString().trim();
            }
        }
        return "";
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }

        try {
            // Essaie différents formats de date
            DateTimeFormatter[] formatters = {
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME
            };

            for (DateTimeFormatter formatter : formatters) {
                try {
                    return LocalDateTime.parse(dateTimeStr.trim(), formatter);
                } catch (DateTimeParseException ignored) {
                    // Continue avec le format suivant
                }
            }

            // Si aucun format ne fonctionne, essaie de parser comme timestamp
            long timestamp = Long.parseLong(dateTimeStr);
            return LocalDateTime.ofEpochSecond(timestamp, 0, java.time.ZoneOffset.UTC);

        } catch (Exception e) {
            System.err.println("❌ Impossible de parser la date: " + dateTimeStr);
            return null;
        }
    }

    /**
     * Classe pour encapsuler les résultats de synchronisation EPG
     */
    public static class EpgSyncResult {
        private final Integer streamId;
        private final int entriesCount;
        private final String message;

        public EpgSyncResult(Integer streamId, int entriesCount, String message) {
            this.streamId = streamId;
            this.entriesCount = entriesCount;
            this.message = message;
        }

        // Getters
        public Integer getStreamId() { return streamId; }
        public int getEntriesCount() { return entriesCount; }
        public String getMessage() { return message; }

        @Override
        public String toString() {
            return String.format("EpgSyncResult{streamId=%d, entries=%d, message='%s'}",
                    streamId, entriesCount, message);
        }
    }
}