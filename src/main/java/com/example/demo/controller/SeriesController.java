package com.example.demo.controller;

import com.example.demo.service.SeriesService;
import com.example.demo.service.EpgService;
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

    @GetMapping
    public ResponseEntity<?> getSeries(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String userId = jwtUtil.extractUserId(token);

            List<Map<String, Object>> series = seriesService.fetchSeriesStreamsForUser(userId);

            return ResponseEntity.ok(Map.of(
                    "count", series.size(),
                    "series", series
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "❌ Erreur: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchSeries(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String name) {
        try {
            String token = authHeader.substring(7);
            String userId = jwtUtil.extractUserId(token);

            List<Map<String, Object>> results = seriesService.searchSeriesByName(userId, name);

            return ResponseEntity.ok(Map.of(
                    "count", results.size(),
                    "results", results
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "❌ Erreur: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/all-names")
    public ResponseEntity<?> getAllSeriesNames(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String userId = jwtUtil.extractUserId(token);

            List<String> seriesNames = seriesService.getAllSeriesNames(userId);

            return ResponseEntity.ok(Map.of(
                    "count", seriesNames.size(),
                    "seriesNames", seriesNames
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "❌ Erreur: " + e.getMessage()
            ));
        }
    }
}