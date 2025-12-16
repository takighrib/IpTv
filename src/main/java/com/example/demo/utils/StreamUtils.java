package com.example.demo.utils;
import java.util.Map;

public final class StreamUtils {

    private StreamUtils() {
        // Classe utilitaire - pas d'instanciation
    }

    /**
     * Parse un Object en Integer de manière sécurisée
     */
    public static Integer parseIntSafely(Object value) {
        if (value == null) return null;
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Parse un Object en int avec valeur par défaut 0
     */
    public static int parseIntOrZero(Object value) {
        Integer result = parseIntSafely(value);
        return result != null ? result : 0;
    }

    /**
     * Récupère une String de manière sécurisée depuis une Map
     */
    public static String getStringSafely(Map<String, Object> map, String... keys) {
        if (map == null) return "";
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null && !value.toString().trim().isEmpty()) {
                return value.toString().trim();
            }
        }
        return "";
    }

    /**
     * Génère un ID unique basé sur l'URL
     */
    public static int generateStreamId(String url) {
        if (url == null || url.isEmpty()) return 0;
        return Math.abs(url.hashCode());
    }

    /**
     * Génère un ID de catégorie basé sur le nom du groupe
     */
    public static int getCategoryId(String groupTitle) {
        if (groupTitle == null || groupTitle.isEmpty()) return 0;
        return Math.abs(groupTitle.hashCode()) % 1000;
    }

    /**
     * Extrait un attribut d'une ligne EXTINF
     */
    public static String extractAttribute(String extinf, String attributeName) {
        if (extinf == null || attributeName == null) return null;

        String pattern = attributeName + "=\"";
        int start = extinf.indexOf(pattern);
        if (start == -1) return null;

        start += pattern.length();
        int end = extinf.indexOf("\"", start);
        if (end == -1) return null;

        return extinf.substring(start, end);
    }
}