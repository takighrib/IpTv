package com.example.demo.controller;

import com.example.demo.service.VodService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.demo.service.VodService;
import com.example.demo.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vod")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class VodController {

    private final VodService vodService;
    private final JwtUtil jwtUtil;

    @GetMapping("/sync")
    public ResponseEntity<?> syncVod(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String userId = jwtUtil.extractUserId(token);

            List<Map<String, Object>> vods = vodService.syncAndSaveVodStreamsForUser(userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "✅ VOD synchronisés",
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
    public ResponseEntity<?> searchVod(@RequestParam String title) {
        try {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "results", vodService.searchVodByTitle(title)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "❌ Erreur: " + e.getMessage()
            ));
        }
    }
}
