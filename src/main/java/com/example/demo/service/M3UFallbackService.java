package com.example.demo.service;

import com.example.demo.config.XtreamConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class M3UFallbackService {

    @Qualifier("m3uWebClient")
    private final WebClient m3uWebClient;
    private final XtreamConfig xtreamConfig;

    private static final int BUFFER_SIZE = 8192; // 8KB buffer

    /**
     * Version optimisée qui traite le fichier M3U en streaming
     * et sépare automatiquement par type de contenu
     */
    public Flux<ContentItem> fetchM3UContent() {
        return m3uWebClient.get()
                .uri(xtreamConfig.getM3uUrl())
                .retrieve()
                .bodyToFlux(DataBuffer.class)
                .transform(this::processStreamingContent)
                .scan(new LineBuffer(), this::accumulateLines)
                .filter(buffer -> buffer.hasCompleteLine())
                .flatMap(this::extractContentFromBuffer)
                .filter(Objects::nonNull);
    }

    /**
     * Version avec URL personnalisée
     */
    public Flux<ContentItem> fetchM3UContent(String m3uUrl) {
        return m3uWebClient.get()
                .uri(m3uUrl)
                .retrieve()
                .bodyToFlux(DataBuffer.class)
                .transform(this::processStreamingContent)
                .scan(new LineBuffer(), this::accumulateLines)
                .filter(buffer -> buffer.hasCompleteLine())
                .flatMap(this::extractContentFromBuffer)
                .filter(Objects::nonNull);
    }

    /**
     * Traite le contenu en streaming avec gestion des buffers
     */
    private Flux<String> processStreamingContent(Flux<DataBuffer> dataBufferFlux) {
        AtomicReference<String> remainder = new AtomicReference<>("");

        return dataBufferFlux
                .flatMap(dataBuffer -> {
                    try {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);

                        String chunk = remainder.get() + new String(bytes, StandardCharsets.UTF_8);
                        String[] lines = chunk.split("\\r?\\n", -1);

                        if (lines.length > 0) {
                            remainder.set(lines[lines.length - 1]);
                        }

                        return Flux.fromArray(lines)
                                .take(lines.length - 1)
                                .filter(line -> !line.trim().isEmpty());
                    } finally {
                        DataBufferUtils.release(dataBuffer);
                    }
                })
                .concatWith(Mono.fromCallable(() -> remainder.get())
                        .filter(line -> !line.trim().isEmpty()));
    }

    /**
     * Accumule les lignes pour former des paires EXTINF + URL
     */
    private LineBuffer accumulateLines(LineBuffer buffer, String line) {
        if (line.startsWith("#EXTINF:")) {
            return new LineBuffer(line, buffer.url);
        } else if (line.startsWith("http") && buffer.extinf != null) {
            return new LineBuffer(buffer.extinf, line, true);
        }
        return buffer;
    }

    /**
     * Extrait le contenu du buffer et détermine son type
     */
    private Flux<ContentItem> extractContentFromBuffer(LineBuffer buffer) {
        if (buffer.isComplete()) {
            ContentItem item = createContentItem(buffer.extinf, buffer.url);
            return item != null ? Flux.just(item) : Flux.empty();
        }
        return Flux.empty();
    }

    /**
     * Crée un ContentItem et détermine automatiquement son type
     */
    private ContentItem createContentItem(String extinf, String url) {
        try {
            ContentType type = determineContentType(extinf, url);
            String name = parseName(extinf);
            Map<String, String> attributes = parseAllAttributes(extinf);

            if (name == null || name.trim().isEmpty()) {
                return null;
            }

            return new ContentItem(name, url, type, attributes);
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing: " + extinf + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * Détermine le type de contenu basé sur les attributs et patterns
     */
    private ContentType determineContentType(String extinf, String url) {
        if (extinf == null || url == null) {
            return ContentType.UNKNOWN;
        }

        String line = extinf.toLowerCase();
        String urlLower = url.toLowerCase();

        // 1. Vérification par group-title
        ContentType groupType = determineTypeByGroup(line);
        if (groupType != ContentType.UNKNOWN) {
            return groupType;
        }

        // 2. Vérification par patterns dans le nom/titre
        ContentType patternType = determineTypeByPatterns(line);
        if (patternType != ContentType.UNKNOWN) {
            return patternType;
        }

        // 3. Vérification par URL
        ContentType urlType = determineTypeByUrl(urlLower);
        if (urlType != ContentType.UNKNOWN) {
            return urlType;
        }

        // 4. Par défaut, considère comme Live Stream
        return ContentType.LIVE_STREAM;
    }

    /**
     * Détermine le type par group-title
     */
    private ContentType determineTypeByGroup(String line) {
        // Live Streams
        String[] liveGroups = {
                "group-title=\"live\"", "group-title=\"tv\"", "group-title=\"television\"",
                "group-title=\"news\"", "group-title=\"sport\"", "group-title=\"sports\"",
                "group-title=\"entertainment\"", "group-title=\"kids\"", "group-title=\"music\"",
                "group-title=\"documentary\"", "group-title=\"lifestyle\"", "group-title=\"adult\"",
                "group-title=\"general\"", "group-title=\"national\"", "group-title=\"local\""
        };

        // VOD
        String[] vodGroups = {
                "group-title=\"movies\"", "group-title=\"vod\"", "group-title=\"films\"",
                "group-title=\"cinema\"", "group-title=\"movie\"", "group-title=\"film\"",
                "group-title=\"hollywood\"", "group-title=\"bollywood\""
        };

        // Series
        String[] seriesGroups = {
                "group-title=\"series\"", "group-title=\"tv shows\"", "group-title=\"shows\"",
                "group-title=\"serie\"", "group-title=\"tv series\"", "group-title=\"drama\""
        };

        for (String group : liveGroups) {
            if (line.contains(group)) return ContentType.LIVE_STREAM;
        }

        for (String group : vodGroups) {
            if (line.contains(group)) return ContentType.VOD;
        }

        for (String group : seriesGroups) {
            if (line.contains(group)) return ContentType.SERIES;
        }

        return ContentType.UNKNOWN;
    }

    /**
     * Détermine le type par patterns dans le contenu
     */
    private ContentType determineTypeByPatterns(String line) {
        // Patterns pour séries
        String[] seriesPatterns = {
                "season", "episode", "s\\d{2}e\\d{2}", "s\\d{1}e\\d{1,2}",
                "saison", "épisode", "ep\\d+", "parte", "temporada"
        };

        // Patterns pour VOD
        String[] vodPatterns = {
                "720p", "1080p", "4k", "2160p", "bluray", "webrip", "dvdrip",
                "hdtv", "web-dl", "brrip", "\\(\\d{4}\\)", "movie", "película"
        };

        for (String pattern : seriesPatterns) {
            if (Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(line).find()) {
                return ContentType.SERIES;
            }
        }

        for (String pattern : vodPatterns) {
            if (Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(line).find()) {
                return ContentType.VOD;
            }
        }

        return ContentType.UNKNOWN;
    }

    /**
     * Détermine le type par URL
     */
    private ContentType determineTypeByUrl(String url) {
        // Extensions vidéo = VOD
        String[] videoExtensions = {
                ".mp4", ".mkv", ".avi", ".mov", ".wmv",
                ".flv", ".webm", ".m4v", ".3gp", ".m2ts"
        };

        for (String ext : videoExtensions) {
            if (url.endsWith(ext)) {
                return ContentType.VOD;
            }
        }

        // Patterns d'URL pour live streams
        if (url.contains("/live/") || url.contains(".ts") || url.contains(".m3u8")) {
            return ContentType.LIVE_STREAM;
        }

        // Pattern Xtream pour series
        if (url.matches(".*series/.*") || url.contains("/episode/")) {
            return ContentType.SERIES;
        }

        // Pattern Xtream pour VOD
        if (url.matches(".*movie/.*") || url.contains("/vod/")) {
            return ContentType.VOD;
        }

        return ContentType.UNKNOWN;
    }

    /**
     * Parse tous les attributs de la ligne EXTINF
     */
    private Map<String, String> parseAllAttributes(String extinf) {
        Map<String, String> attributes = new HashMap<>();

        if (extinf == null) return attributes;

        // Patterns pour extraire les attributs
        String[] attrNames = {
                "tvg-id", "tvg-name", "tvg-logo", "tvg-country",
                "tvg-language", "group-title", "radio", "tvg-shift"
        };

        for (String attr : attrNames) {
            String value = extractAttribute(extinf, attr);
            if (value != null && !value.trim().isEmpty()) {
                attributes.put(attr, value.trim());
            }
        }

        // Extrait aussi la durée
        Pattern durationPattern = Pattern.compile("#EXTINF:(-?\\d+(?:\\.\\d+)?)");
        Matcher matcher = durationPattern.matcher(extinf);
        if (matcher.find()) {
            attributes.put("duration", matcher.group(1));
        }

        return attributes;
    }

    /**
     * Parse le nom de la chaîne/contenu
     */
    private String parseName(String extinf) {
        if (extinf == null || extinf.trim().isEmpty()) {
            return null;
        }

        // Essaie d'abord tvg-name
        String name = extractAttribute(extinf, "tvg-name");
        if (name != null && !name.trim().isEmpty()) {
            return name.trim();
        }

        // Fallback vers le nom après la virgule
        int commaIndex = extinf.lastIndexOf(',');
        if (commaIndex != -1 && commaIndex < extinf.length() - 1) {
            return extinf.substring(commaIndex + 1).trim();
        }

        return null;
    }

    /**
     * Extrait un attribut de la ligne EXTINF
     */
    private String extractAttribute(String extinf, String attributeName) {
        String pattern = attributeName + "=\"";
        int start = extinf.indexOf(pattern);
        if (start == -1) {
            return null;
        }

        start += pattern.length();
        int end = extinf.indexOf("\"", start);
        if (end == -1) {
            return null;
        }

        return extinf.substring(start, end);
    }

    /**
     * Récupère les statistiques du parsing M3U
     */
    public Mono<M3UParsingStats> getM3UParsingStats() {
        return fetchM3UContent()
                .collectList()
                .map(items -> {
                    Map<ContentType, Long> typeCount = new HashMap<>();
                    for (ContentType type : ContentType.values()) {
                        typeCount.put(type, items.stream()
                                .filter(item -> item.getType() == type)
                                .count());
                    }
                    return new M3UParsingStats(items.size(), typeCount);
                });
    }

    /**
     * Enum pour les types de contenu
     */
    public enum ContentType {
        LIVE_STREAM("Live Stream"),
        VOD("Video on Demand"),
        SERIES("TV Series"),
        EPG("Electronic Program Guide"),
        UNKNOWN("Unknown");

        private final String displayName;

        ContentType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Classe pour représenter un élément de contenu
     */
    public static class ContentItem {
        private final String name;
        private final String url;
        private final ContentType type;
        private final Map<String, String> attributes;

        public ContentItem(String name, String url, ContentType type, Map<String, String> attributes) {
            this.name = name;
            this.url = url;
            this.type = type;
            this.attributes = attributes != null ? new HashMap<>(attributes) : new HashMap<>();
        }

        // Getters
        public String getName() { return name; }
        public String getUrl() { return url; }
        public ContentType getType() { return type; }
        public Map<String, String> getAttributes() { return new HashMap<>(attributes); }

        // Getters pour attributs spécifiques
        public String getTvgId() { return attributes.get("tvg-id"); }
        public String getTvgLogo() { return attributes.get("tvg-logo"); }
        public String getGroupTitle() { return attributes.get("group-title"); }
        public String getTvgCountry() { return attributes.get("tvg-country"); }
        public String getTvgLanguage() { return attributes.get("tvg-language"); }
        public String getDuration() { return attributes.get("duration"); }
        public String getTvgShift() { return attributes.get("tvg-shift"); }

        @Override
        public String toString() {
            return String.format("ContentItem{name='%s', type=%s, group='%s'}",
                    name, type, getGroupTitle());
        }
    }

    /**
     * Classe pour gérer l'accumulation des lignes
     */
    private static class LineBuffer {
        final String extinf;
        final String url;
        final boolean complete;

        public LineBuffer() {
            this(null, null, false);
        }

        public LineBuffer(String extinf, String url) {
            this(extinf, url, false);
        }

        public LineBuffer(String extinf, String url, boolean complete) {
            this.extinf = extinf;
            this.url = url;
            this.complete = complete;
        }

        public boolean hasCompleteLine() {
            return extinf != null || url != null;
        }

        public boolean isComplete() {
            return complete && extinf != null && url != null;
        }
    }

    /**
     * Statistiques du parsing M3U
     */
    public static class M3UParsingStats {
        private final int totalItems;
        private final Map<ContentType, Long> typeDistribution;

        public M3UParsingStats(int totalItems, Map<ContentType, Long> typeDistribution) {
            this.totalItems = totalItems;
            this.typeDistribution = typeDistribution;
        }

        public int getTotalItems() { return totalItems; }
        public Map<ContentType, Long> getTypeDistribution() { return typeDistribution; }

        @Override
        public String toString() {
            return String.format("M3UParsingStats{total=%d, live=%d, vod=%d, series=%d}",
                    totalItems,
                    typeDistribution.getOrDefault(ContentType.LIVE_STREAM, 0L),
                    typeDistribution.getOrDefault(ContentType.VOD, 0L),
                    typeDistribution.getOrDefault(ContentType.SERIES, 0L));
        }
    }
}