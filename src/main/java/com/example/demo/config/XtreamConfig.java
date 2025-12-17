package com.example.demo.config;

import org.springframework.context.annotation.Configuration;

/**
 * Configuration globale pour Xtream Codes API
 * Les credentials spécifiques sont dans UserXtreamConfig (par utilisateur)
 */
@Configuration
public class XtreamConfig {

    // Timeouts en secondes
    public static final int DEFAULT_TIMEOUT = 30;
    public static final int M3U_TIMEOUT = 600;
    public static final int EPG_TIMEOUT = 45;

    // Taille maximale en MB
    public static final int MAX_M3U_SIZE_MB = 200;
    public static final int MAX_IN_MEMORY_SIZE_MB = 50;

    // Retry configuration
    public static final int MAX_RETRY_ATTEMPTS = 3;
    public static final int RETRY_DELAY_MS = 1000;

    /**
     * Actions disponibles dans l'API Xtream
     */
    public static class Actions {
        public static final String GET_LIVE_STREAMS = "get_live_streams";
        public static final String GET_VOD_STREAMS = "get_vod_streams";
        public static final String GET_SERIES = "get_series";
        public static final String GET_SHORT_EPG = "get_short_epg";
        public static final String GET_SIMPLE_DATA_TABLE = "get_simple_data_table";
        public static final String GET_EPG = "get_epg";
        public static final String GET_LIVE_CATEGORIES = "get_live_categories";
        public static final String GET_VOD_CATEGORIES = "get_vod_categories";
        public static final String GET_SERIES_CATEGORIES = "get_series_categories";
    }

    /**
     * Extensions de conteneurs vidéo supportées
     */
    public static class Extensions {
        public static final String TS = "ts";
        public static final String M3U8 = "m3u8";
        public static final String MP4 = "mp4";
        public static final String MKV = "mkv";
        public static final String AVI = "avi";
    }

    /**
     * Types de streaming
     */
    public static class StreamTypes {
        public static final String LIVE = "live";
        public static final String MOVIE = "movie";
        public static final String SERIES = "series";
    }

    /**
     * Formats de sortie M3U
     */
    public static class OutputFormats {
        public static final String M3U = "m3u";
        public static final String M3U_PLUS = "m3u_plus";
        public static final String TS = "ts";
        public static final String HLS = "hls";
    }
}