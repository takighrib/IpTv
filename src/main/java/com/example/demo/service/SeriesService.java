package com.example.demo.service;

import com.example.demo.config.UserXtreamConfig;
import com.example.demo.model.Series;
import com.example.demo.repository.SeriesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SeriesService {

    private final SeriesRepository seriesRepository;
    private final WebClient webClient;
    private final UserContextService userContextService;

    /**
     * Récupère les séries pour un utilisateur spécifique
     */
    public List<Map<String, Object>> fetchSeriesStreamsForUser(String userId) {
        UserXtreamConfig config = userContextService.getUserXtreamConfigOrThrow(userId);
        return fetchSeriesStreams(config);
    }

    /**
     * Récupère la liste des séries depuis l'API Xtream
     */
    private List<Map<String, Object>> fetchSeriesStreams(UserXtreamConfig config) {
        try {
            List<Map<String, Object>> series = webClient.get()
                    .uri(config.getSeriesUrl())
                    .retrieve()
                    .bodyToMono(List.class)
                    .block();

            System.out.println("✅ Récupéré " + (series != null ? series.size() : 0) + " séries depuis Xtream API");

            // ✅ Ajouter l'URL de streaming complète
            if (series != null) {
                for (Map<String, Object> serie : series) {
                    Integer seriesId = parseIntSafely(serie.get("series_id"));
                    String extension = getStringSafely(serie, "container_extension");
                    if (seriesId > 0 && !extension.isEmpty()) {
                        serie.put("stream_url", config.getSeriesStreamUrl(seriesId, extension));
                    }
                }
            }

            return series != null ? series : new ArrayList<>();

        } catch (Exception e) {
            System.err.println("❌ Erreur API Séries : " + e.getMessage());
            return fetchSeriesFromM3U(config);
        }
    }

    /**
     * Fallback pour récupérer les séries depuis le M3U
     */
    private List<Map<String, Object>> fetchSeriesFromM3U(UserXtreamConfig config) {
        try {
            String m3uContent = webClient.get()
                    .uri(config.getM3uUrl())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseSeriesFromM3U(m3uContent);
        } catch (Exception ex) {
            throw new RuntimeException("Impossible de récupérer les séries via fallback M3U", ex);
        }
    }

    /**
     * Parse un fichier M3U pour extraire uniquement les séries
     */
    private List<Map<String, Object>> parseSeriesFromM3U(String m3uContent) {
        List<Map<String, Object>> series = new ArrayList<>();
        if (m3uContent == null) return series;

        String[] lines = m3uContent.split("\n");
        Map<String, Object> currentSeries = null;

        for (String line : lines) {
            if (line.startsWith("#EXTINF:")) {
                // Vérifie si c'est une série
                if (isSeriesContent(line)) {
                    currentSeries = new HashMap<>();
                    String[] parts = line.split(",", 2);
                    if (parts.length == 2) {
                        String title = parts[1].trim();
                        currentSeries.put("name", title);

                        // Extrait les métadonnées de série
                        extractSeriesMetadata(title, line, currentSeries);
                    }
                } else {
                    currentSeries = null; // Ignore les non-séries
                }
            } else if (line.startsWith("http") && currentSeries != null) {
                // Vérification finale de l'URL
                if (isValidSeriesUrl(line.trim())) {
                    currentSeries.put("stream_url", line.trim());
                    currentSeries.put("series_id", generateStreamId(line.trim()));
                    currentSeries.put("category_id", getCategoryId((String) currentSeries.get("group_title")));
                    currentSeries.put("category_name",
                            currentSeries.getOrDefault("group_title", "TV Series").toString());
                    series.add(currentSeries);
                }
                currentSeries = null;
            }
        }

        System.out.println("✅ Parsé " + series.size() + " épisodes de séries depuis M3U");
        return series;
    }

    /**
     * Détermine si une ligne EXTINF correspond à une série
     */
    private boolean isSeriesContent(String extinf) {
        if (extinf == null) return false;

        String line = extinf.toLowerCase();

        // Patterns indiquant des séries
        String[] seriesPatterns = {
                "group-title=\"series\"", "group-title=\"tv shows\"",
                "group-title=\"shows\"", "group-title=\"serie\"",
                "group-title=\"tv series\"", "season", "episode",
                "s\\d{1,2}e\\d{1,3}", "s\\d{2}e\\d{2}",
                "saison", "épisode", "ep\\d+", "parte"
        };

        for (String pattern : seriesPatterns) {
            if (Pattern.compile(pattern).matcher(line).find()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Valide si l'URL correspond à une série
     */
    private boolean isValidSeriesUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        String lowerUrl = url.toLowerCase();

        // Patterns d'URL série
        return lowerUrl.contains("/series/") ||
                lowerUrl.contains("/episode/") ||
                lowerUrl.matches(".*s\\d+e\\d+.*");
    }

    /**
     * Extrait les métadonnées spécifiques aux séries
     */
    private void extractSeriesMetadata(String title, String extinf, Map<String, Object> series) {
        // Nom de la série (sans saison/épisode)
        String seriesName = extractSeriesName(title);
        if (seriesName != null) series.put("series_name", seriesName);

        // Numéro de saison
        Integer season = extractSeason(title);
        if (season != null) series.put("season", season);

        // Numéro d'épisode
        Integer episode = extractEpisode(title);
        if (episode != null) series.put("episode", episode);

        // Année
        String year = extractYear(title);
        if (year != null) series.put("year", Integer.parseInt(year));

        // Genre depuis group-title
        String groupTitle = extractAttribute(extinf, "group-title");
        if (groupTitle != null) {
            series.put("group_title", groupTitle);
            series.put("genre", mapCategoryToGenre(groupTitle));
        }

        // Logo/Cover
        String logo = extractAttribute(extinf, "tvg-logo");
        if (logo != null) series.put("stream_icon", logo);

        // Durée de l'épisode
        String duration = extractDuration(extinf);
        if (duration != null) series.put("duration", duration);
    }

    /**
     * Extrait le nom de la série sans les informations d'épisode
     */
    private String extractSeriesName(String title) {
        // Supprime les patterns de saison/épisode pour obtenir le nom de la série
        String seriesName = title;

        // Patterns à supprimer
        String[] patterns = {
                "\\s+S\\d{1,2}E\\d{1,3}.*",
                "\\s+Season\\s+\\d+.*",
                "\\s+Saison\\s+\\d+.*",
                "\\s+Episode\\s+\\d+.*",
                "\\s+Ep\\d+.*",
                "\\s+\\(\\d{4}\\).*"
        };

        for (String pattern : patterns) {
            seriesName = seriesName.replaceFirst(pattern, "").trim();
        }

        return seriesName.isEmpty() ? title : seriesName;
    }

    /**
     * Extrait le numéro de saison
     */
    private Integer extractSeason(String title) {
        // Patterns pour saison
        String[] seasonPatterns = {
                "S(\\d{1,2})E\\d{1,3}",
                "Season\\s+(\\d{1,2})",
                "Saison\\s+(\\d{1,2})"
        };

        for (String patternStr : seasonPatterns) {
            Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(title);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
        }

        return null;
    }

    /**
     * Extrait le numéro d'épisode
     */
    private Integer extractEpisode(String title) {
        // Patterns pour épisode
        String[] episodePatterns = {
                "S\\d{1,2}E(\\d{1,3})",
                "Episode\\s+(\\d{1,3})",
                "Ep(\\d{1,3})",
                "Épisode\\s+(\\d{1,3})"
        };

        for (String patternStr : episodePatterns) {
            Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(title);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
        }

        return null;
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
        if (category == null) return "Drama";

        String lowerCategory = category.toLowerCase();
        if (lowerCategory.contains("comedy")) return "Comedy";
        if (lowerCategory.contains("drama")) return "Drama";
        if (lowerCategory.contains("action")) return "Action";
        if (lowerCategory.contains("thriller")) return "Thriller";
        if (lowerCategory.contains("sci-fi") || lowerCategory.contains("science")) return "Sci-Fi";
        if (lowerCategory.contains("romance")) return "Romance";
        if (lowerCategory.contains("crime")) return "Crime";
        if (lowerCategory.contains("horror")) return "Horror";
        if (lowerCategory.contains("documentary")) return "Documentary";
        if (lowerCategory.contains("animation")) return "Animation";

        return "Series";
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
     * Sauvegarde les séries en base avec validation
     */
    public void saveSeriesStreams(List<Map<String, Object>> streams) {
        if (streams == null || streams.isEmpty()) {
            System.out.println("⚠ Aucune série à sauvegarder");
            return;
        }

        int savedCount = 0;
        int errorCount = 0;

        for (Map<String, Object> s : streams) {
            try {
                Series series = Series.builder()
                        .seriesId(parseIntSafely(s.get("series_id")))
                        .name(getStringSafely(s, "name"))
                        .categoryId(parseIntSafely(s.get("category_id")))
                        .categoryName(getStringSafely(s, "category_name"))
                        .streamUrl(getStringSafely(s, "stream_url"))
                        .streamIcon(getStringSafely(s, "stream_icon"))
                        .build();

                // Validation avant sauvegarde
                if (series.getName() != null && series.getStreamUrl() != null) {
                    seriesRepository.save(series);
                    savedCount++;
                } else {
                    System.err.println("⚠ Série invalide ignorée: " + s.get("name"));
                    errorCount++;
                }
            } catch (Exception e) {
                System.err.println("❌ Erreur sauvegarde série: " + s.get("name") + " - " + e.getMessage());
                errorCount++;
            }
        }

        System.out.println("✅ Séries sauvegardées: " + savedCount + " réussies, " + errorCount + " erreurs");
    }

    /**
     * Sauvegarde en lot pour de gros volumes
     */
    private void saveSeriesStreamsBatch(List<Map<String, Object>> streams, int batchSize) {
        if (streams == null || streams.isEmpty()) {
            System.out.println("⚠ Aucune série à sauvegarder");
            return;
        }

        for (int i = 0; i < streams.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, streams.size());
            List<Map<String, Object>> batch = streams.subList(i, endIndex);

            System.out.println("📦 Traitement du lot Séries " + (i / batchSize + 1) + " (" + batch.size() + " éléments)");
            saveSeriesStreams(batch);
        }
    }

    /**
     * Synchronise et sauvegarde les séries pour un utilisateur
     */
    public List<Map<String, Object>> syncAndSaveSeriesStreamsForUser(String userId) {
        List<Map<String, Object>> streams = fetchSeriesStreamsForUser(userId);

        if (streams.size() > 1000) {
            saveSeriesStreamsBatch(streams, 100);
        } else {
            saveSeriesStreams(streams);
        }

        return streams;
    }

    /**
     * Recherche de séries par nom
     */
    public List<Series> searchSeriesByName(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return seriesRepository.findByNameContainingIgnoreCase(searchTerm.trim());
    }

    /**
     * Récupère tous les épisodes d'une série
     */
    public List<Series> getEpisodesBySeriesName(String seriesName) {
        // TODO: Implémenter dans le repository
        // return seriesRepository.findBySeriesNameOrderBySeasonAscEpisodeAsc(seriesName);
        return new ArrayList<>();
    }

    /**
     * Récupère les épisodes d'une saison spécifique
     */
    public List<Series> getEpisodesBySeriesAndSeason(String seriesName, Integer season) {
        // TODO: Implémenter dans le repository
        // return seriesRepository.findBySeriesNameAndSeasonOrderByEpisodeAsc(seriesName, season);
        return new ArrayList<>();
    }

    /**
     * Récupère les séries par genre
     */
    public List<Series> getSeriesByGenre(String genre) {
        // TODO: Implémenter dans le repository
        // return seriesRepository.findByGenre(genre);
        return new ArrayList<>();
    }

    /**
     * Obtient la liste de toutes les séries (noms uniques)
     */
    public List<String> getAllSeriesNames() {
        // TODO: Implémenter dans le repository
        // return seriesRepository.findDistinctSeriesNames();
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