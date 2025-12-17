package com.example.demo.controller;

import com.example.demo.service.SeriesService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.demo.service.SeriesService;
import com.example.demo.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/series")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class SeriesController {

    private final SeriesService seriesService;
    private final JwtUtil jwtUtil;

    @GetMapping("/sync")
    public ResponseEntity<?> syncSeries(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String userId = jwtUtil.extractUserId(token);

            List<Map<String, Object>> series = seriesService.syncAndSaveSeriesStreamsForUser(userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "✅ Séries synchronisées",
                    "count", series.size(),
                    "series", series
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "❌ Erreur: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchSeries(@RequestParam String name) {
        try {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "results", seriesService.searchSeriesByName(name)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "❌ Erreur: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/all-names")
    public ResponseEntity<?> getAllSeriesNames() {
        try {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "seriesNames", seriesService.getAllSeriesNames()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "❌ Erreur: " + e.getMessage()
            ));
        }
    }
}