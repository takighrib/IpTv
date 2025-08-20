package com.example.demo.controller;

import com.example.demo.service.SeriesService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/series")
@RequiredArgsConstructor
public class SeriesController {

    private final SeriesService seriesService;

    /**
     * Synchronise et sauvegarde les séries
     * @param forceFallback si true, utilise le fallback M3U
     */
    @GetMapping("/sync")
    public Map<String, Object> syncSeries(@RequestParam(defaultValue = "false") boolean forceFallback) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<Map<String, Object>> series;

            if (forceFallback) {
                // 🚨 Forcer la récupération via M3U
                series = seriesService.fetchSeriesFromM3U();
            } else {
                // 🌐 Tentative API Xtream avec fallback automatique
                series = seriesService.fetchSeriesStreams();
            }

            // Sauvegarde des séries en base
            seriesService.saveSeriesStreams(series);

            response.put("message", "✅ Séries synchronisées avec succès");
            response.put("count", series.size());
            response.put("series", series);
            response.put("status", "success");

        } catch (Exception e) {
            e.printStackTrace();
            response.put("message", "❌ Erreur lors de la synchronisation des séries");
            response.put("error", e.getMessage());
            response.put("status", "failed");
        }

        return response;
    }

    /**
     * Recherche des séries par nom
     */
    @GetMapping("/search")
    public Map<String, Object> searchSeries(@RequestParam String name) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<?> results = seriesService.searchSeriesByName(name);
            response.put("message", "Résultats trouvés pour: " + name);
            response.put("count", results.size());
            response.put("results", results);
            response.put("status", "success");
        } catch (Exception e) {
            e.printStackTrace();
            response.put("message", "❌ Erreur lors de la recherche");
            response.put("error", e.getMessage());
            response.put("status", "failed");
        }
        return response;
    }

    /**
     * Obtenir tous les noms de séries
     */
    @GetMapping("/all-names")
    public Map<String, Object> getAllSeriesNames() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<String> names = seriesService.getAllSeriesNames();
            response.put("message", "Liste de toutes les séries");
            response.put("count", names.size());
            response.put("seriesNames", names);
            response.put("status", "success");
        } catch (Exception e) {
            e.printStackTrace();
            response.put("message", "❌ Erreur lors de la récupération des séries");
            response.put("error", e.getMessage());
            response.put("status", "failed");
        }
        return response;
    }
}
