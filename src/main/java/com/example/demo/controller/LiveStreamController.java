package com.example.demo.controller;

import com.example.demo.service.LiveStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.demo.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/live-streams")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class LiveStreamController {

    private final LiveStreamService liveStreamService;
    private final JwtUtil jwtUtil;

    /**
     * Synchronise les live streams pour l'utilisateur connecté
     */
    @GetMapping("/sync")
    public ResponseEntity<?> syncLiveStreams(@RequestHeader("Authorization") String authHeader) {
        try {
            // Extraire userId du token
            String token = authHeader.substring(7);
            String userId = jwtUtil.extractUserId(token);

            // Synchroniser les streams
            List<Map<String, Object>> streams = liveStreamService.syncAndSaveLiveStreamsForUser(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "✅ Live Streams synchronisés");
            response.put("count", streams.size());
            response.put("streams", streams);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "❌ Erreur: " + e.getMessage()
            ));
        }
    }

    /**
     * Récupère les live streams sans sauvegarder
     */
    @GetMapping("/fetch")
    public ResponseEntity<?> fetchLiveStreams(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String userId = jwtUtil.extractUserId(token);

            List<Map<String, Object>> streams = liveStreamService.fetchLiveStreamsForUser(userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count", streams.size(),
                    "streams", streams
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "❌ Erreur: " + e.getMessage()
            ));
        }
    }

    /**
     * Recherche de live streams
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchLiveStreams(@RequestParam String query) {
        try {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "results", liveStreamService.searchLiveStreamsByName(query)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "❌ Erreur: " + e.getMessage()
            ));
        }
    }
}
