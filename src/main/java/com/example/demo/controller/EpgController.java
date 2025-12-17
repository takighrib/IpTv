package com.example.demo.controller;

import com.example.demo.service.EpgService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import com.example.demo.service.EpgService.EpgSyncResult;


import com.example.demo.service.EpgService;
import com.example.demo.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/epg")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class EpgController {

    private final EpgService epgService;
    private final JwtUtil jwtUtil;

    @GetMapping("/sync/{streamId}")
    public ResponseEntity<?> syncEpg(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Integer streamId) {

        try {
            String token = authHeader.substring(7);
            String userId = jwtUtil.extractUserId(token);

            EpgService.EpgSyncResult result = epgService.syncEpgForStreamForUser(userId, streamId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", result.getMessage(),
                    "streamId", result.getStreamId(),
                    "entriesCount", result.getEntriesCount()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "❌ Erreur: " + e.getMessage()
            ));
        }
    }
}
