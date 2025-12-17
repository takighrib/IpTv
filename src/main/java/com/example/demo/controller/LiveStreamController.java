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



import com.example.demo.service.LiveStreamService;
import com.example.demo.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/live-streams")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class LiveStreamController {

    private final LiveStreamService liveStreamService;
    private final JwtUtil jwtUtil;

    /**
     * Récupère les live streams en temps réel
     */
    @GetMapping
    public ResponseEntity<?> getLiveStreams(@RequestHeader("Authorization") String authHeader) {
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
    public ResponseEntity<?> searchLiveStreams(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String query) {
        try {
            String token = authHeader.substring(7);
            String userId = jwtUtil.extractUserId(token);

            List<Map<String, Object>> results = liveStreamService.searchLiveStreamsByName(userId, query);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count", results.size(),
                    "results", results
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "❌ Erreur: " + e.getMessage()
            ));
        }
    }

    /**
     * Filtre par catégorie
     */
    @GetMapping("/category/{categoryName}")
    public ResponseEntity<?> getByCategory(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String categoryName) {
        try {
            String token = authHeader.substring(7);
            String userId = jwtUtil.extractUserId(token);

            List<Map<String, Object>> streams = liveStreamService.getLiveStreamsByCategory(userId, categoryName);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "category", categoryName,
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
     * Liste des catégories disponibles
     */
    @GetMapping("/categories")
    public ResponseEntity<?> getCategories(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String userId = jwtUtil.extractUserId(token);

            List<String> categories = liveStreamService.getAvailableCategories(userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count", categories.size(),
                    "categories", categories
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "❌ Erreur: " + e.getMessage()
            ));
        }
    }
}
