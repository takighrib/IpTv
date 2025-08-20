package com.example.demo.controller;

import com.example.demo.service.VodService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/vod")
@RequiredArgsConstructor
public class VodController {

    private final VodService vodService;

    /**
     * Synchronise et sauvegarde les VOD
     * @param forceFallback si true, force la récupération via M3U
     */
    @GetMapping("/sync")
    public Map<String, Object> syncVod(@RequestParam(defaultValue = "false") boolean forceFallback) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<Map<String, Object>> vods;

            if (forceFallback) {
                vods = vodService.fetchVodFromM3U();
            } else {
                vods = vodService.fetchVodStreams();
            }

            // Sauvegarde en base
            vodService.saveVodStreams(vods);

            response.put("message", "✅ VOD synchronisés avec succès");
            response.put("count", vods.size());
            response.put("vods", vods);
            response.put("status", "success");

        } catch (Exception e) {
            e.printStackTrace();
            response.put("message", "❌ Erreur lors de la synchronisation des VOD");
            response.put("error", e.getMessage());
            response.put("status", "failed");
        }

        return response;
    }

    /**
     * Recherche VOD par titre
     */
    @GetMapping("/search")
    public Map<String, Object> searchVod(@RequestParam String title) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<?> results = vodService.searchVodByTitle(title);
            response.put("message", "Résultats trouvés pour: " + title);
            response.put("count", results.size());
            response.put("results", results);
            response.put("status", "success");
        } catch (Exception e) {
            e.printStackTrace();
            response.put("message", "❌ Erreur lors de la recherche VOD");
            response.put("error", e.getMessage());
            response.put("status", "failed");
        }
        return response;
    }



}
