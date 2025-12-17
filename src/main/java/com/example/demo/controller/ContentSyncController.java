package com.example.demo.controller;

import com.example.demo.service.IntegratedContentSyncService;
import com.example.demo.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class ContentSyncController {

    private final IntegratedContentSyncService syncService;
    private final JwtUtil jwtUtil;

    /**
     * Synchronise tout le contenu (Live, VOD, Series, EPG)
     */
    @PostMapping("/all")
    public ResponseEntity<?> syncAllContent(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String userId = jwtUtil.extractUserId(token);

            // Vérifier si l'utilisateur peut synchroniser
            if (!syncService.canUserSync(userId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                                "success", false,
                                "message", "Configuration Xtream manquante. Veuillez configurer vos credentials."
                        ));
            }

            IntegratedContentSyncService.ContentSyncResult result = syncService.syncAllContentForUser(userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "✅ Synchronisation complète terminée",
                    "statistics", result.getStatistics()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "❌ Erreur: " + e.getMessage()
                    ));
        }
    }

    /**
     * Synchronise uniquement les Live Streams
     */
    @PostMapping("/live-streams")
    public ResponseEntity<?> syncLiveStreamsOnly(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String userId = jwtUtil.extractUserId(token);

            IntegratedContentSyncService.ContentSyncResult result = syncService.syncLiveStreamsOnly(userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "✅ Live Streams synchronisés",
                    "count", result.getLiveStreams().size()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "❌ Erreur: " + e.getMessage()
                    ));
        }
    }

    /**
     * Synchronise uniquement les VOD
     */
    @PostMapping("/vod")
    public ResponseEntity<?> syncVodOnly(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String userId = jwtUtil.extractUserId(token);

            IntegratedContentSyncService.ContentSyncResult result = syncService.syncVodOnly(userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "✅ VOD synchronisés",
                    "count", result.getVodContent().size()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "❌ Erreur: " + e.getMessage()
                    ));
        }
    }

    /**
     * Synchronise uniquement les Séries
     */
    @PostMapping("/series")
    public ResponseEntity<?> syncSeriesOnly(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String userId = jwtUtil.extractUserId(token);

            IntegratedContentSyncService.ContentSyncResult result = syncService.syncSeriesOnly(userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "✅ Séries synchronisées",
                    "count", result.getSeries().size()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "❌ Erreur: " + e.getMessage()
                    ));
        }
    }

    /**
     * Synchronise l'EPG pour un stream spécifique
     */
    @PostMapping("/epg/{streamId}")
    public ResponseEntity<?> syncEpgForStream(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Integer streamId) {

        try {
            String token = authHeader.substring(7);
            String userId = jwtUtil.extractUserId(token);

            Map<String, Object> result = syncService.syncEpgForSingleStream(userId, streamId);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "❌ Erreur: " + e.getMessage()
                    ));
        }
    }

    /**
     * Obtient les statistiques de synchronisation
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getStats(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String userId = jwtUtil.extractUserId(token);

            Map<String, Object> stats = syncService.getComprehensiveStatsForUser(userId);

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "❌ Erreur: " + e.getMessage()
                    ));
        }
    }

    /**
     * Vérifie si l'utilisateur peut synchroniser
     */
    @GetMapping("/can-sync")
    public ResponseEntity<?> canSync(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String userId = jwtUtil.extractUserId(token);

            boolean canSync = syncService.canUserSync(userId);

            return ResponseEntity.ok(Map.of(
                    "can_sync", canSync,
                    "message", canSync ?
                            "Configuration Xtream valide" :
                            "Configuration Xtream manquante ou invalide"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "❌ Erreur: " + e.getMessage()
                    ));
        }
    }
}