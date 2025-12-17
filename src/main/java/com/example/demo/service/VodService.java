package com.example.demo.service;

import com.example.demo.config.UserXtreamConfig;
import com.example.demo.model.Vod;
import com.example.demo.repository.VodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class VodService {

    private final VodRepository vodRepository;
    private final WebClient webClient;
    private final UserContextService userContextService;

    /**
     * Récupère les VOD pour un utilisateur spécifique
     */
    public List<Map<String, Object>> fetchVodStreamsForUser(String userId) {
        UserXtreamConfig config = userContextService.getUserXtreamConfigOrThrow(userId);
        return fetchVodStreams(config);
    }

    /**
     * Récupère la liste des VOD depuis l'API Xtream
     */
    private List<Map<String, Object>> fetchVodStreams(UserXtreamConfig config) {
        try {
            List<Map<String, Object>> vods = webClient.get()
                    .uri(config.getVodStreamsUrl())
                    .retrieve()
                    .bodyToMono(List.class)
                    .block();

            System.out.println("✅ Récupéré " + (vods != null ? vods.size() : 0) + " VOD depuis Xtream API");

            // ✅ Ajouter l'URL de streaming complète
            if (vods != null) {
                for (Map<String, Object> vod : vods) {
                    Integer vodId = parseIntSafely(vod.get("stream_id"));
                    String extension = getStringSafely(vod, "container_extension");
                    if (vodId > 0 && !extension.isEmpty()) {
                        vod.put("stream_url", config.getVodStreamUrl(vodId, extension));
                    }
                }
            }

            return vods != null ? vods : new ArrayList<>();

        } catch (Exception e) {
            System.err.println("❌ Erreur API VOD : " + e.getMessage());
            return fetchVodFromM3U(config);
        }
    }

    /**
     * Fallback pour récupérer les VOD depuis le M3U
     */
    private List<Map<String, Object>> fetchVodFromM3U(UserXtreamConfig config) {
        try {
            String m3uContent = webClient.get()
                    .uri(config.getM3uUrl())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseVodFromM3U(m3uContent);
        } catch (Exception ex) {
            throw new RuntimeException("Impossible de récupérer les VOD via fallback M3U", ex);
        }
    }

    /**
     * Parse un fichier M3U pour extraire uniquement les VOD
     */
    private List<Map<String, Object>> parseVodFromM3U(String m3uContent) {
        List<Map<String, Object>> vods = new ArrayList<>();
        if (m3uContent == null) return vods;

        String[] lines = m3uContent.split("\n");
        Map<String, Object> currentVod = null;

        for (String line : lines) {
            if (line.startsWith("#EXTINF:")) {
                // Vérifie si c'est un VOD
                if (isVodContent(line)) {
                    currentVod = new HashMap<>();
                    String[] parts = line.split(",", 2);
                    if (parts.length == 2) {
                        String title = parts[1].trim();
                        currentVod.put("name", title);

                        // Extrait les métadonnées du titre
                        extractVodMetadata(title, line, currentVod);
                    }
                } else {
                    currentVod = null; // Ignore les non-VOD
                }
            } else if (line.startsWith("http") && currentVod != null) {
                // Vérification finale de l'URL
                if (isValidVodUrl(line.trim())) {
                    currentVod.put("stream_url", line.trim());
                    currentVod.put("stream_id", generateStreamId(line.trim()));
                    currentVod.put("category_id", getCategoryId((String) currentVod.get("group_title")));
                    currentVod.put("category_name",
                            currentVod.getOrDefault("group_title", "Movies").toString());
                    vods.add(currentVod);
                }
                currentVod = null;
            }
        }

        System.out.println("✅ Parsé " + vods.size() + " VOD depuis M3U");
        return vods;
    }

    /**
     * Détermine si une ligne EXTINF correspond à un contenu VOD
     */
    private boolean isVodContent(String extinf) {
        if (extinf == null) return false;

        String line = extinf.toLowerCase();

        // Patterns indiquant du VOD
        String[] vodPatterns = {
                "group-title=\"movies\"", "group-title=\"vod\"",
                "group-title=\"films\"", "group-title=\"cinema\"",
                "group-title=\"movie\"", "group-title=\"film\"",
                "720p", "1080p", "4k", "bluray", "webrip", "dvdrip",
                "hdtv", "web-dl", "brrip", "\\(\\d{4}\\)"
        };

        for (String pattern : vodPatterns) {
            if (Pattern.compile(pattern).matcher(line).find()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Valide si l'URL correspond à un contenu VOD
     */
    private boolean isValidVodUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        String lowerUrl = url.toLowerCase();

        // Extensions vidéo typiques du VOD
        String[] vodExtensions = {
                ".mp4", ".mkv", ".avi", ".mov", ".wmv",
                ".flv", ".webm", ".m4v"
        };

        for (String ext : vodExtensions) {
            if (lowerUrl.endsWith(ext)) {
                return true;
            }
        }

        // Patterns d'URL VOD
        return lowerUrl.contains("/movie/") || lowerUrl.contains("/vod/");
    }

    /**
     * Extrait les métadonnées spécifiques aux VOD
     */
    private void extractVodMetadata(String title, String extinf, Map<String, Object> vod) {
        // Année de sortie
        String year = extractYear(title);
        if (year != null) vod.put("year", Integer.parseInt(year));

        // Qualité
        String quality = extractQuality(title);
        if (quality != null) vod.put("quality", quality);

        // Genre depuis group-title
        String groupTitle = extractAttribute(extinf, "group-title");
        if (groupTitle != null) {
            vod.put("group_title", groupTitle);
            vod.put("genre", mapCategoryToGenre(groupTitle));
        }

        // Logo/Icône
        String logo = extractAttribute(extinf, "tvg-logo");
        if (logo != null) vod.put("stream_icon", logo);

        // Durée (si présente)
        String duration = extractDuration(extinf);
        if (duration != null) vod.put("duration", duration);
    }

    /**
     * Extrait l'année du titre
     */
    private String extractYear(String title) {
        Pattern yearPattern = Pattern.compile("\\((\\d{4})\\)");
        Matcher matcher = yearPattern.matcher(title);
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * Extrait la qualité du titre
     */
    private String extractQuality(String title) {
        String[] qualities = {"4K", "2160p", "1080p", "720p", "480p", "HD", "BluRay", "WEBRip", "DVDRip"};
        String upperTitle = title.toUpperCase();

        for (String quality : qualities) {
            if (upperTitle.contains(quality)) {
                return quality;
            }
        }
        return null;
    }

    /**
     * Extrait la durée depuis EXTINF
     */
    private String extractDuration(String extinf) {
        Pattern durationPattern = Pattern.compile("#EXTINF:(-?\\d+(?:\\.\\d+)?)");
        Matcher matcher = durationPattern.matcher(extinf);
        if (matcher.find()) {
            try {
                int seconds = (int) Double.parseDouble(matcher.group(1));
                if (seconds > 0) {
                    return String.valueOf(seconds / 60); // Conversion en minutes
                }
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    /**
     * Mappe une catégorie vers un genre
     */
    private String mapCategoryToGenre(String category) {
        if (category == null) return "Unknown";

        String lowerCategory = category.toLowerCase();
        if (lowerCategory.contains("action")) return "Action";
        if (lowerCategory.contains("comedy")) return "Comedy";
        if (lowerCategory.contains("drama")) return "Drama";
        if (lowerCategory.contains("horror")) return "Horror";
        if (lowerCategory.contains("sci-fi") || lowerCategory.contains("science")) return "Sci-Fi";
        if (lowerCategory.contains("romance")) return "Romance";
        if (lowerCategory.contains("thriller")) return "Thriller";
        if (lowerCategory.contains("documentary")) return "Documentary";
        if (lowerCategory.contains("animation")) return "Animation";

        return "Movie";
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
     * Sauvegarde les VOD en base avec validation
     */
    public void saveVodStreams(List<Map<String, Object>> streams) {
        if (streams == null || streams.isEmpty()) {
            System.out.println("⚠ Aucun VOD à sauvegarder");
            return;
        }

        int savedCount = 0;
        int errorCount = 0;

        for (Map<String, Object> s : streams) {
            try {
                Vod vod = Vod.builder()
                        .vodId(parseIntSafely(s.get("stream_id")))
                        .name(getStringSafely(s, "name"))
                        .categoryId(parseIntSafely(s.get("category_id")))
                        .categoryName(getStringSafely(s, "category_name"))
                        .streamUrl(getStringSafely(s, "stream_url"))
                        .streamIcon(getStringSafely(s, "stream_icon"))
                        .build();

                // Validation avant sauvegarde
                if (vod.getName() != null && vod.getStreamUrl() != null) {
                    vodRepository.save(vod);
                    savedCount++;
                } else {
                    System.err.println("⚠ VOD invalide ignoré: " + s.get("name"));
                    errorCount++;
                }
            } catch (Exception e) {
                System.err.println("❌ Erreur sauvegarde VOD: " + s.get("name") + " - " + e.getMessage());
                errorCount++;
            }
        }

        System.out.println("✅ VOD sauvegardés: " + savedCount + " réussis, " + errorCount + " erreurs");
    }

    /**
     * Sauvegarde en lot pour de gros volumes
     */
    public void saveVodStreamsBatch(List<Map<String, Object>> streams, int batchSize) {
        if (streams == null || streams.isEmpty()) {
            System.out.println("⚠ Aucun VOD à sauvegarder");
            return;
        }

        for (int i = 0; i < streams.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, streams.size());
            List<Map<String, Object>> batch = streams.subList(i, endIndex);

            System.out.println("📦 Traitement du lot VOD " + (i / batchSize + 1) + " (" + batch.size() + " éléments)");
            saveVodStreams(batch);
        }
    }

    /**
     * Synchronise et sauvegarde les VOD pour un utilisateur
     */
    public List<Map<String, Object>> syncAndSaveVodStreamsForUser(String userId) {
        List<Map<String, Object>> streams = fetchVodStreamsForUser(userId);

        if (streams.size() > 1000) {
            saveVodStreamsBatch(streams, 100);
        } else {
            saveVodStreams(streams);
        }

        return streams;
    }

    /**
     * Recherche de VOD par titre
     */
    public List<Vod> searchVodByTitle(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return vodRepository.findByNameContainingIgnoreCase(searchTerm.trim());
    }

    /**
     * Récupère les VOD par année
     */
    public List<Vod> getVodByYear(Integer year) {
        // TODO: Implémenter dans le repository
        // return vodRepository.findByYear(year);
        return new ArrayList<>();
    }

    /**
     * Récupère les VOD par genre
     */
    public List<Vod> getVodByGenre(String genre) {
        // TODO: Implémenter dans le repository
        // return vodRepository.findByGenre(genre);
        return new ArrayList<>();
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