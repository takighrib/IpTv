package com.example.demo.service;

import com.example.demo.config.XtreamConfig;
import com.example.demo.model.LiveStream;
import com.example.demo.repository.LiveStreamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
@RequiredArgsConstructor
public class LiveStreamService {

    private final LiveStreamRepository liveStreamRepository;
    private final WebClient webClient;
    private final XtreamConfig xtreamConfig;

    /**
     * Récupère la liste des streams depuis l'API Xtream.
     * Si erreur -> fallback vers le fichier M3U.
     */
    public List<Map<String, Object>> fetchLiveStreamsFromXtream() {
        try {
            List<Map<String, Object>> streams = webClient.get()
                    .uri(xtreamConfig.getLiveStreamsUrl())
                    .retrieve()
                    .bodyToMono(List.class)
                    .block();

            System.out.println("✅ Récupéré " + (streams != null ? streams.size() : 0) + " live streams depuis Xtream API");
            return streams != null ? streams : new ArrayList<>();

        } catch (Exception e) {
            System.err.println("❌ Erreur API Live Streams : " + e.getMessage());
            return fetchFromM3U();
        }
    }

    /**
     * Fallback vers l'API M3U (get.php)
     */
    public List<Map<String, Object>> fetchFromM3U() {
        try {
            String m3uContent = webClient.get()
                    .uri(xtreamConfig.getM3uUrl())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseM3U(m3uContent);
        } catch (Exception ex) {
            throw new RuntimeException("Impossible de récupérer les streams via fallback M3U", ex);
        }
    }

    /**
     * Parse un fichier M3U en liste de streams avec filtrage pour live streams uniquement
     */
    private List<Map<String, Object>> parseM3U(String m3uContent) {
        List<Map<String, Object>> streams = new ArrayList<>();
        if (m3uContent == null) return streams;

        String[] lines = m3uContent.split("\n");
        Map<String, Object> currentStream = null;

        for (String line : lines) {
            if (line.startsWith("#EXTINF:")) {
                // Vérifie si c'est un live stream avant de créer l'objet
                if (isLiveStream(line)) {
                    currentStream = new HashMap<>();
                    String[] parts = line.split(",", 2);
                    if (parts.length == 2) {
                        currentStream.put("name", parts[1].trim());
                    }

                    // Extrait les métadonnées
                    extractM3UMetadata(line, currentStream);
                } else {
                    currentStream = null; // Ignore les non-live streams
                }
            } else if (line.startsWith("http") && currentStream != null) {
                // Double vérification au niveau de l'URL
                if (isValidLiveStreamUrl(line.trim())) {
                    currentStream.put("stream_url", line.trim());
                    currentStream.put("stream_id", generateStreamId(line.trim()));
                    currentStream.put("category_id", getCategoryId((String) currentStream.get("group_title")));
                    currentStream.put("category_name",
                            currentStream.getOrDefault("group_title", "Live TV").toString());
                    streams.add(currentStream);
                }
                currentStream = null;
            }
        }

        System.out.println("✅ Parsé " + streams.size() + " live streams depuis M3U");
        return streams;
    }

    /**
     * Extrait les métadonnées du M3U
     */
    private void extractM3UMetadata(String extinf, Map<String, Object> stream) {
        // tvg-id
        String tvgId = extractAttribute(extinf, "tvg-id");
        if (tvgId != null) stream.put("tvg_id", tvgId);

        // tvg-logo
        String tvgLogo = extractAttribute(extinf, "tvg-logo");
        if (tvgLogo != null) stream.put("stream_icon", tvgLogo);

        // group-title
        String groupTitle = extractAttribute(extinf, "group-title");
        if (groupTitle != null) stream.put("group_title", groupTitle);

        // tvg-country
        String country = extractAttribute(extinf, "tvg-country");
        if (country != null) stream.put("country", country);

        // tvg-language
        String language = extractAttribute(extinf, "tvg-language");
        if (language != null) stream.put("language", language);
    }

    /**
     * Extrait un attribut de la ligne EXTINF
     */
    private String extractAttribute(String extinf, String attributeName) {
        String pattern = attributeName + "=\"";
        int start = extinf.indexOf(pattern);
        if (start == -1) return null;

        start += pattern.length();
        int end = extinf.indexOf("\"", start);
        if (end == -1) return null;

        return extinf.substring(start, end);
    }

    /**
     * Détermine si une ligne EXTINF correspond à un live stream
     */
    private boolean isLiveStream(String extinf) {
        if (extinf == null) return false;

        String line = extinf.toLowerCase();

        // Patterns indiquant des VOD/Séries à exclure
        String[] vodPatterns = {
                "group-title=\"movies\"", "group-title=\"vod\"",
                "group-title=\"series\"", "group-title=\"tv shows\"",
                "group-title=\"films\"", "group-title=\"cinema\"",
                "season", "episode", "s01e", "s02e",
                "720p", "1080p", "4k", "bluray", "webrip"
        };

        for (String pattern : vodPatterns) {
            if (line.contains(pattern)) {
                return false;
            }
        }

        return true; // Par défaut, considère comme live stream
    }

    /**
     * Valide si l'URL correspond à un live stream
     */
    private boolean isValidLiveStreamUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        String lowerUrl = url.toLowerCase();

        // Extensions de fichiers vidéo (VOD)
        String[] videoExtensions = {
                ".mp4", ".mkv", ".avi", ".mov", ".wmv",
                ".flv", ".webm", ".m4v", ".3gp"
        };

        for (String ext : videoExtensions) {
            if (lowerUrl.endsWith(ext)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Sauvegarde en DB avec gestion des erreurs améliorée
     */
    public void saveLiveStreams(List<Map<String, Object>> streams) {
        if (streams == null || streams.isEmpty()) {
            System.out.println("⚠ Aucun live stream à sauvegarder");
            return;
        }

        int savedCount = 0;
        int errorCount = 0;

        for (Map<String, Object> s : streams) {
            try {
                LiveStream liveStream = LiveStream.builder()
                        .streamId(parseIntSafely(s.get("stream_id")))
                        .name(getStringSafely(s, "name"))
                        .categoryId(parseIntSafely(s.get("category_id")))
                        .categoryName(getStringSafely(s, "category_name"))
                        .streamUrl(getStringSafely(s, "stream_url"))
                        .streamIcon(getStringSafely(s, "stream_icon"))
                        .build();

                // Validation avant sauvegarde
                if (liveStream.getName() != null && liveStream.getStreamUrl() != null) {
                    liveStreamRepository.save(liveStream);
                    savedCount++;
                } else {
                    System.err.println("⚠ Live stream invalide ignoré: " + s.get("name"));
                    errorCount++;
                }
            } catch (Exception e) {
                System.err.println("❌ Erreur sauvegarde live stream: " + s.get("name") + " - " + e.getMessage());
                errorCount++;
            }
        }

        System.out.println("✅ Live Streams sauvegardés: " + savedCount + " réussis, " + errorCount + " erreurs");
    }

    /**
     * Sauvegarde optimisée en lot pour de gros volumes
     */
    public void saveLiveStreamsBatch(List<Map<String, Object>> streams, int batchSize) {
        if (streams == null || streams.isEmpty()) {
            System.out.println("⚠ Aucun live stream à sauvegarder");
            return;
        }

        // Traitement par batch pour éviter les problèmes de mémoire
        for (int i = 0; i < streams.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, streams.size());
            List<Map<String, Object>> batch = streams.subList(i, endIndex);

            System.out.println("📦 Traitement du lot " + (i / batchSize + 1) + " (" + batch.size() + " live streams)");
            saveLiveStreams(batch);
        }
    }

    /**
     * Récupère les live streams avec flux réactif
     */
    public Flux<Map<String, Object>> fetchLiveStreamsReactive() {
        return webClient.get()
                .uri(xtreamConfig.getLiveStreamsUrl())
                .retrieve()
                .bodyToFlux(List.class)
                .flatMapIterable(list -> (List<Map<String, Object>>) list)
                .onErrorResume(throwable -> {
                    System.err.println("❌ Erreur flux réactif, fallback M3U: " + throwable.getMessage());
                    return Flux.fromIterable(fetchFromM3U());
                });
    }

    /**
     * Recherche de live streams par nom
     */
    public List<LiveStream> searchLiveStreamsByName(String searchTerm) {
        // TODO: Implémenter dans le repository
        // return liveStreamRepository.findByNameContainingIgnoreCase(searchTerm);
        return new ArrayList<>();
    }

    /**
     * Récupère les live streams par catégorie
     */
    public List<LiveStream> getLiveStreamsByCategory(String categoryName) {
        // TODO: Implémenter dans le repository
        // return liveStreamRepository.findByCategoryName(categoryName);
        return new ArrayList<>();
    }

    /**
     * Méthode pratique pour synchroniser et sauvegarder
     */
    public List<Map<String, Object>> syncAndSaveLiveStreams() {
        List<Map<String, Object>> streams = fetchLiveStreamsFromXtream();

        // Utilise la sauvegarde par batch pour de gros volumes
        if (streams.size() > 1000) {
            saveLiveStreamsBatch(streams, 100);
        } else {
            saveLiveStreams(streams);
        }

        return streams;
    }

    // Méthodes utilitaires
    private int parseIntSafely(Object value) {
        if (value == null) return 0;
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String getStringSafely(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null && !value.toString().trim().isEmpty()) {
                return value.toString().trim();
            }
        }
        return "";
    }

    private int generateStreamId(String url) {
        return Math.abs(url.hashCode());
    }

    private int getCategoryId(String groupTitle) {
        if (groupTitle == null) return 0;
        return Math.abs(groupTitle.hashCode()) % 1000;
    }
}