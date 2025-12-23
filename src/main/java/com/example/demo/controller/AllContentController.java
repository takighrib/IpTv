package com.example.demo.controller;


import com.example.demo.service.AllContentService;
import com.example.demo.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller pour récupérer TOUT le contenu IPTV
 * VERSION SIMPLIFIÉE - Données uniquement
 */
@RestController
@RequestMapping("/api/content")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Slf4j
public class AllContentController {

    private final AllContentService allContentService;
    private final JwtUtil jwtUtil;

    /**
     * Récupère TOUT le contenu (Live + VOD + Séries)
     * GET /api/content/all
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllContent(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String userId = jwtUtil.extractUserId(token);
            Map<String, Object> content = allContentService.getAllContentForUser(userId);
            return ResponseEntity.ok(content);
        } catch (Exception e) {
            log.error("❌ Erreur GET ALL CONTENT: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Recherche dans TOUT le contenu
     * GET /api/content/search?query=sport
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchAllContent(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String query) {
        try {
            String token = authHeader.substring(7);
            String userId = jwtUtil.extractUserId(token);
            log.info("🔍 Requête SEARCH ALL pour userId: {} - query: {}", userId, query);
            Map<String, Object> results = allContentService.searchAllContent(userId, query);
            return ResponseEntity.ok(results);

        } catch (Exception e) {
            log.error("❌ Erreur SEARCH ALL: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Récupère toutes les catégories disponibles
     * GET /api/content/categories
     */
    @GetMapping("/categories")
    public ResponseEntity<?> getAllCategories(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String userId = jwtUtil.extractUserId(token);
            log.info("📂 Requête ALL CATEGORIES pour userId: {}", userId);
            Map<String, Object> categories = allContentService.getAllCategories(userId);
            return ResponseEntity.ok(categories);

        } catch (Exception e) {
            log.error("❌ Erreur ALL CATEGORIES: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }
}