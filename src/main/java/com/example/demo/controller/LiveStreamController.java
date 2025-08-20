package com.example.demo.controller;

import com.example.demo.service.LiveStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class LiveStreamController {

    private final LiveStreamService liveStreamService;

    @GetMapping("/sync/live-streams")
    public Map<String, Object> syncLiveStreams(@RequestParam(defaultValue = "false") boolean forceFallback) {
        List<Map<String, Object>> streams;

        if (forceFallback) {
            // 🚨 On force l’usage du fallback M3U
            streams = liveStreamService.fetchFromM3U();
        } else {
            // 🌐 On essaie l’API Xtream, avec fallback automatique en cas d’échec
            streams = liveStreamService.fetchLiveStreamsFromXtream();
        }

        // Sauvegarde des streams en base
        liveStreamService.saveLiveStreams(streams);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "✅ Live Streams synchronisés");
        response.put("count", streams.size());
        response.put("streams", streams);

        return response;
    }
}
