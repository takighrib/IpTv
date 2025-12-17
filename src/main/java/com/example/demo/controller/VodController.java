package com.example.demo.controller;

import com.example.demo.service.VodService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import com.example.demo.security.JwtUtil;
import org.springframework.http.ResponseEntity;


@RestController
@RequestMapping("/api/vod")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class VodController {

    private final VodService vodService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<?> getVod(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String userId = jwtUtil.extractUserId(token);

            List<Map<String, Object>> vods = vodService.fetchVodStreamsForUser(userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count", vods.size(),
                    "vods", vods
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "❌ Erreur: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchVod(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String title) {
        try {
            String token = authHeader.substring(7);
            String userId = jwtUtil.extractUserId(token);

            List<Map<String, Object>> results = vodService.searchVodByTitle(userId, title);

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

    @GetMapping("/categories")
    public ResponseEntity<?> getCategories(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String userId = jwtUtil.extractUserId(token);

            List<String> categories = vodService.getAvailableCategories(userId);

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
