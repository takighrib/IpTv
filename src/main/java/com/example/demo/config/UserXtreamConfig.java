package com.example.demo.config;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration Xtream spécifique à un utilisateur
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserXtreamConfig {

    private String baseUrl;
    private String username;
    private String password;

    /**
     * Génère l'URL pour récupérer les live streams
     */
    public String getLiveStreamsUrl() {
        return baseUrl + "/player_api.php?username=" + username +
                "&password=" + password + "&action=get_live_streams";
    }

    /**
     * Génère l'URL pour récupérer les VOD
     */
    public String getVodStreamsUrl() {
        return baseUrl + "/player_api.php?username=" + username +
                "&password=" + password + "&action=get_vod_streams";
    }

    /**
     * Génère l'URL pour récupérer les séries
     */
    public String getSeriesUrl() {
        return baseUrl + "/player_api.php?username=" + username +
                "&password=" + password + "&action=get_series";
    }

    /**
     * Génère l'URL pour récupérer l'EPG d'un stream
     */
    public String getEpgUrl(Integer streamId) {
        return baseUrl + "/player_api.php?username=" + username +
                "&password=" + password + "&action=get_short_epg&stream_id=" + streamId;
    }

    /**
     * Génère l'URL pour récupérer l'EPG complet
     */
    public String getFullEpgUrl() {
        return baseUrl + "/player_api.php?username=" + username +
                "&password=" + password + "&action=get_epg";
    }

    /**
     * Génère l'URL M3U
     */
    public String getM3uUrl() {
        return baseUrl + "/get.php?username=" + username +
                "&password=" + password + "&type=m3u_plus&output=ts";
    }

    /**
     * Génère l'URL de streaming pour un live stream
     */
    public String getLiveStreamUrl(Integer streamId) {
        return baseUrl + "/" + username + "/" + password + "/" + streamId;
    }

    /**
     * Génère l'URL de streaming pour un VOD
     */
    public String getVodStreamUrl(Integer vodId, String extension) {
        return baseUrl + "/movie/" + username + "/" + password + "/" + vodId + "." + extension;
    }

    /**
     * Génère l'URL de streaming pour une série
     */
    public String getSeriesStreamUrl(Integer seriesId, String extension) {
        return baseUrl + "/series/" + username + "/" + password + "/" + seriesId + "." + extension;
    }

    /**
     * Vérifie si la configuration est valide
     */
    public boolean isValid() {
        return baseUrl != null && !baseUrl.isEmpty()
                && username != null && !username.isEmpty()
                && password != null && !password.isEmpty();
    }
}