package com.example.demo.service;

import com.example.demo.config.UserXtreamConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

import com.example.demo.config.UserXtreamConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EpgService {

    private final WebClient webClient;
    private final UserContextService userContextService;

    public List<Map<String, Object>> fetchEpgForStreamForUser(String userId, Integer streamId) {
        UserXtreamConfig config = userContextService.getUserXtreamConfigOrThrow(userId);
        return fetchEpgForStream(config, streamId);
    }

    private List<Map<String, Object>> fetchEpgForStream(UserXtreamConfig config, Integer streamId) {
        try {
            List<Map<String, Object>> response = webClient.get()
                    .uri(config.getEpgUrl(streamId))
                    .retrieve()
                    .bodyToMono(List.class)
                    .block();

            log.info("✅ Récupéré {} programmes EPG pour stream {}",
                    response != null ? response.size() : 0, streamId);

            return response != null ? response : new ArrayList<>();

        } catch (Exception e) {
            log.error("❌ Erreur fetch EPG pour stream {}: {}", streamId, e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Map<String, Object>> fetchFullEpgForUser(String userId) {
        UserXtreamConfig config = userContextService.getUserXtreamConfigOrThrow(userId);
        return fetchFullEpg(config);
    }

    private List<Map<String, Object>> fetchFullEpg(UserXtreamConfig config) {
        try {
            List<Map<String, Object>> response = webClient.get()
                    .uri(config.getFullEpgUrl())
                    .retrieve()
                    .bodyToMono(List.class)
                    .block();

            log.info("✅ Récupéré {} programmes EPG complet",
                    response != null ? response.size() : 0);

            return response != null ? response : new ArrayList<>();

        } catch (Exception e) {
            log.error("❌ Erreur fetch EPG complet: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public Map<String, Object> getCurrentProgramForStream(String userId, Integer streamId) {
        List<Map<String, Object>> epgList = fetchEpgForStreamForUser(userId, streamId);

        if (epgList.isEmpty()) {
            return null;
        }

        return epgList.get(0);
    }

    public static class EpgSyncResult {
        private final Integer streamId;
        private final int entriesCount;
        private final String message;

        public EpgSyncResult(Integer streamId, int entriesCount, String message) {
            this.streamId = streamId;
            this.entriesCount = entriesCount;
            this.message = message;
        }

        public Integer getStreamId() { return streamId; }
        public int getEntriesCount() { return entriesCount; }
        public String getMessage() { return message; }

        @Override
        public String toString() {
            return String.format("EpgSyncResult{streamId=%d, entries=%d, message='%s'}",
                    streamId, entriesCount, message);
        }
    }

    public EpgSyncResult syncEpgForStreamForUser(String userId, Integer streamId) {
        try {
            List<Map<String, Object>> epgList = fetchEpgForStreamForUser(userId, streamId);

            if (epgList.isEmpty()) {
                return new EpgSyncResult(streamId, 0, "Aucune donnée EPG disponible");
            }

            return new EpgSyncResult(streamId, epgList.size(), "✅ EPG récupéré avec succès");

        } catch (Exception e) {
            return new EpgSyncResult(streamId, 0, "❌ Erreur: " + e.getMessage());
        }
    }
}
