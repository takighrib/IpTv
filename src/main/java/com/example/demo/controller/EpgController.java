package com.example.demo.controller;

import com.example.demo.service.EpgService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import com.example.demo.security.JwtUtil;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/epg")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
class EpgController {

    private final EpgService epgService;
    private final JwtUtil jwtUtil;

    @GetMapping("/{streamId}")
    public ResponseEntity<?> getEpg(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Integer streamId) {

        try {
            String token = authHeader.substring(7);
            String userId = jwtUtil.extractUserId(token);

            List<Map<String, Object>> epgList = epgService.fetchEpgForStreamForUser(userId, streamId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "streamId", streamId,
                    "count", epgList.size(),
                    "epg", epgList
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "❌ Erreur: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/{streamId}/current")
    public ResponseEntity<?> getCurrentProgram(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Integer streamId) {

        try {
            String token = authHeader.substring(7);
            String userId = jwtUtil.extractUserId(token);

            Map<String, Object> currentProgram = epgService.getCurrentProgramForStream(userId, streamId);

            if (currentProgram == null) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "streamId", streamId,
                        "message", "Aucun programme en cours"
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "streamId", streamId,
                    "currentProgram", currentProgram
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "❌ Erreur: " + e.getMessage()
            ));
        }
    }
}






