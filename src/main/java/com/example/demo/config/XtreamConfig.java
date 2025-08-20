package com.example.demo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration centralisée pour les paramètres Xtream
 */
@Configuration
@ConfigurationProperties(prefix = "xtream")
@Data
public class XtreamConfig {

    private String baseUrl = "http://buysmart.tn:8080";
    private String username = "buysmart01370";
    private String password = "0731brd";

    // URLs construites automatiquement
    public String getLiveStreamsUrl() {
        return baseUrl + "/player_api.php?username=" + username +
                "&password=" + password + "&action=get_live_streams";
    }

    public String getVodStreamsUrl() {
        return baseUrl + "/player_api.php?username=" + username +
                "&password=" + password + "&action=get_vod_streams";
    }

    public String getSeriesUrl() {
        return baseUrl + "/player_api.php?username=" + username +
                "&password=" + password + "&action=get_series";
    }

    public String getEpgUrl(Integer streamId) {
        return baseUrl + "/player_api.php?username=" + username +
                "&password=" + password + "&action=get_short_epg&stream_id=" + streamId;
    }

    public String getFullEpgUrl() {
        return baseUrl + "/player_api.php?username=" + username +
                "&password=" + password + "&action=get_epg";
    }

    public String getM3uUrl() {
        return baseUrl + "/get.php?username=" + username +
                "&password=" + password + "&type=m3u_plus&output=ts";
    }
}