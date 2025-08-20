package com.example.demo.controller;

import com.example.demo.service.EpgService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import com.example.demo.service.EpgService.EpgSyncResult;


@RestController
@RequestMapping("/epg")
@RequiredArgsConstructor
public class EpgController {

    private final EpgService epgService;

    @GetMapping("/sync/{streamId}")
    public Map<String, Object> syncEpg(
            @PathVariable Integer streamId,
            @RequestParam(defaultValue = "false") boolean forceFallback) {

        Map<String, Object> response = new HashMap<>();

        try {
            EpgSyncResult result;

            if (forceFallback) {
                // Utiliser la méthode fallback pour ce stream
                result = epgService.syncEpgForStream(streamId); // dans le service, gère fallback si nécessaire
            } else {
                result = epgService.syncEpgForStream(streamId);
            }

            response.put("message", result.getMessage());
            response.put("streamId", result.getStreamId());
            response.put("entriesCount", result.getEntriesCount());
            response.put("status", "success");

        } catch (Exception e) {
            e.printStackTrace();
            response.put("message", "❌ Error synchronizing EPG for stream " + streamId);
            response.put("error", e.getMessage());
            response.put("status", "failed");
        }

        return response;
    }
}
