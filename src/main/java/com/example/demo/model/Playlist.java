package com.example.demo.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Playlist {

    @Builder.Default
    private String id = UUID.randomUUID().toString();

    private String nom; // Nom de la playlist

    // Credentials Xtream pour cette playlist
    private String xtreamBaseUrl;    // Ex: http://buysmart.tn:8080
    private String xtreamUsername;   // Ex: buysmart01370
    private String xtreamPassword;   // Ex: 0731brd

    // Favoris spécifiques à cette playlist
    @Builder.Default
    private List<Favori> favoris = new ArrayList<>();

    // Date d'expiration de cette playlist
    private LocalDateTime dateExpiration;

    // Dates de création et modification
    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();
    private LocalDateTime dateModification;

    // Statut de la playlist
    @Builder.Default
    private boolean isActive = true;

    // Constructeur avec génération automatique d'ID
    public Playlist(String nom, String xtreamBaseUrl, String xtreamUsername, String xtreamPassword) {
        this.id = UUID.randomUUID().toString();
        this.nom = nom;
        this.xtreamBaseUrl = xtreamBaseUrl;
        this.xtreamUsername = xtreamUsername;
        this.xtreamPassword = xtreamPassword;
        this.favoris = new ArrayList<>();
        this.dateCreation = LocalDateTime.now();
        this.isActive = true;
    }

    // Méthodes utilitaires
    public void ajouterFavori(String idContenu, String nomContenu, String type) {
        Favori favori = new Favori(idContenu, nomContenu, type);
        if (!favoris.contains(favori)) {
            favoris.add(favori);
        }
    }

    public void retirerFavori(String idContenu) {
        favoris.removeIf(f -> f.getIdContenu().equals(idContenu));
    }

    public boolean isExpired() {
        return dateExpiration != null && LocalDateTime.now().isAfter(dateExpiration);
    }

    public boolean hasXtreamConfig() {
        return xtreamBaseUrl != null && !xtreamBaseUrl.isEmpty()
                && xtreamUsername != null && !xtreamUsername.isEmpty()
                && xtreamPassword != null && !xtreamPassword.isEmpty();
    }

    /**
     * Génère l'URL pour récupérer les live streams
     */
    public String getLiveStreamsUrl() {
        if (!hasXtreamConfig()) {
            throw new IllegalStateException("Configuration Xtream incomplète");
        }
        return xtreamBaseUrl + "/player_api.php?username=" + xtreamUsername +
                "&password=" + xtreamPassword + "&action=get_live_streams";
    }

    /**
     * Génère l'URL pour récupérer les VOD
     */
    public String getVodStreamsUrl() {
        if (!hasXtreamConfig()) {
            throw new IllegalStateException("Configuration Xtream incomplète");
        }
        return xtreamBaseUrl + "/player_api.php?username=" + xtreamUsername +
                "&password=" + xtreamPassword + "&action=get_vod_streams";
    }

    /**
     * Génère l'URL pour récupérer les séries
     */
    public String getSeriesUrl() {
        if (!hasXtreamConfig()) {
            throw new IllegalStateException("Configuration Xtream incomplète");
        }
        return xtreamBaseUrl + "/player_api.php?username=" + xtreamUsername +
                "&password=" + xtreamPassword + "&action=get_series";
    }

    /**
     * Génère l'URL pour récupérer l'EPG d'un stream
     */
    public String getEpgUrl(Integer streamId) {
        if (!hasXtreamConfig()) {
            throw new IllegalStateException("Configuration Xtream incomplète");
        }
        return xtreamBaseUrl + "/player_api.php?username=" + xtreamUsername +
                "&password=" + xtreamPassword + "&action=get_short_epg&stream_id=" + streamId;
    }

    /**
     * Génère l'URL pour récupérer l'EPG complet
     */
    public String getFullEpgUrl() {
        if (!hasXtreamConfig()) {
            throw new IllegalStateException("Configuration Xtream incomplète");
        }
        return xtreamBaseUrl + "/player_api.php?username=" + xtreamUsername +
                "&password=" + xtreamPassword + "&action=get_epg";
    }

    /**
     * Génère l'URL M3U
     */
    public String getM3uUrl() {
        if (!hasXtreamConfig()) {
            throw new IllegalStateException("Configuration Xtream incomplète");
        }
        return xtreamBaseUrl + "/get.php?username=" + xtreamUsername +
                "&password=" + xtreamPassword + "&type=m3u_plus&output=ts";
    }

    /**
     * Génère l'URL de streaming pour un live stream
     */
    public String getLiveStreamUrl(Integer streamId) {
        if (!hasXtreamConfig()) {
            throw new IllegalStateException("Configuration Xtream incomplète");
        }
        return xtreamBaseUrl + "/" + xtreamUsername + "/" + xtreamPassword + "/" + streamId;
    }

    /**
     * Génère l'URL de streaming pour un VOD
     */
    public String getVodStreamUrl(Integer vodId, String extension) {
        if (!hasXtreamConfig()) {
            throw new IllegalStateException("Configuration Xtream incomplète");
        }
        return xtreamBaseUrl + "/movie/" + xtreamUsername + "/" + xtreamPassword + "/" + vodId + "." + extension;
    }

    /**
     * Génère l'URL de streaming pour une série
     */
    public String getSeriesStreamUrl(Integer seriesId, String extension) {
        if (!hasXtreamConfig()) {
            throw new IllegalStateException("Configuration Xtream incomplète");
        }
        return xtreamBaseUrl + "/series/" + xtreamUsername + "/" + xtreamPassword + "/" + seriesId + "." + extension;
    }

    /**
     * Vérifie si la configuration est valide (alias pour hasXtreamConfig)
     */
    public boolean isValid() {
        return hasXtreamConfig();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Playlist playlist = (Playlist) o;
        return id != null && id.equals(playlist.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Playlist{" +
                "id='" + id + '\'' +
                ", nom='" + nom + '\'' +
                ", xtreamBaseUrl='" + xtreamBaseUrl + '\'' +
                ", isActive=" + isActive +
                ", dateExpiration=" + dateExpiration +
                ", nombreFavoris=" + (favoris != null ? favoris.size() : 0) +
                '}';
    }
}


