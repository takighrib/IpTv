package com.example.demo.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "comptes")
public class Compte {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String password;

    // Informations de base
    private String nom;
    private String prenom;

    // Liste des playlists associées au compte
    @Builder.Default
    private List<Playlist> playlists = new ArrayList<>();

    // Dates
    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Builder.Default
    private boolean isActive = true;

    @Builder.Default
    private boolean isEmailVerified = false; // ✅ NOUVEAU - Indique si l'email est vérifié

    // Constructeur simplifié pour la création de compte
    public Compte(String email, String password, String nom, String prenom) {
        this.email = email;
        this.password = password;
        this.nom = nom;
        this.prenom = prenom;
        this.playlists = new ArrayList<>();
        this.dateCreation = LocalDateTime.now();
        this.isActive = true;
        this.isEmailVerified = false;
    }


    // ✅ Méthodes pour gérer les playlists
    public void ajouterPlaylist(Playlist playlist) {
        if (playlist != null && !playlists.contains(playlist)) {
            playlists.add(playlist);
        }
    }

    public void retirerPlaylist(String playlistId) {
        if (playlists != null) {
            playlists.removeIf(p -> p.getId().equals(playlistId));
        }
    }

    public Playlist trouverPlaylistParId(String playlistId) {
        if (playlists == null) {
            return null;
        }
        return playlists.stream()
                .filter(p -> p.getId().equals(playlistId))
                .findFirst()
                .orElse(null);
    }

    public boolean hasPlaylists() {
        return playlists != null && !playlists.isEmpty();
    }

    public int getNombrePlaylists() {
        return playlists != null ? playlists.size() : 0;
    }

    // ✅ Méthode pour vérifier si au moins une playlist a une config Xtream valide
    public boolean hasAnyValidXtreamConfig() {
        return playlists != null && playlists.stream()
                .anyMatch(Playlist::hasXtreamConfig);
    }

    /**
     * Obtient la première playlist active
     */
    public Playlist getPremierPlaylistActive() {
        if (playlists == null) {
            return null;
        }
        return playlists.stream()
                .filter(Playlist::isActive)
                .findFirst()
                .orElse(null);
    }

    /**
     * Obtient toutes les playlists actives
     */
    public List<Playlist> getPlaylistsActives() {
        if (playlists == null) {
            return new ArrayList<>();
        }
        return playlists.stream()
                .filter(Playlist::isActive)
                .toList();
    }

    /**
     * Obtient toutes les playlists avec une config Xtream valide
     */
    public List<Playlist> getPlaylistsAvecXtreamValide() {
        if (playlists == null) {
            return new ArrayList<>();
        }
        return playlists.stream()
                .filter(Playlist::hasXtreamConfig)
                .toList();
    }

    @Override
    public String toString() {
        return "Compte{" +
                "id='" + id + '\'' +
                ", email='" + email + '\'' +
                ", nom='" + nom + '\'' +
                ", prenom='" + prenom + '\'' +
                ", isActive=" + isActive +
                ", isEmailVerified=" + isEmailVerified +
                ", nombrePlaylists=" + getNombrePlaylists() +
                '}';
    }
}


