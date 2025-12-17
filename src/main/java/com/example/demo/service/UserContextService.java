package com.example.demo.service;

import com.example.demo.config.UserXtreamConfig;
import com.example.demo.model.Compte;
import com.example.demo.model.Playlist;
import com.example.demo.repository.CompteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service pour gérer le contexte utilisateur et sa configuration Xtream
 * Support multi-playlist
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserContextService {

    private final CompteRepository compteRepository;

    /**
     * Récupère un compte par son ID
     */
    public Optional<Compte> getCompteById(String userId) {
        return compteRepository.findById(userId);
    }

    /**
     * Vérifie si l'utilisateur a une configuration Xtream valide
     * (au moins une playlist active avec credentials Xtream)
     */
    public boolean hasValidXtreamConfig(String userId) {
        Optional<Compte> compteOpt = compteRepository.findById(userId);

        if (compteOpt.isEmpty()) {
            log.warn("⚠️ Compte introuvable pour userId: {}", userId);
            return false;
        }

        Compte compte = compteOpt.get();

        // Vérifie si au moins une playlist a une config Xtream valide
        return compte.hasAnyValidXtreamConfig();
    }

    /**
     * Récupère la configuration Xtream de l'utilisateur
     * Retourne la première playlist active avec une config valide
     */
    public UserXtreamConfig getUserXtreamConfig(String userId) {
        Optional<Compte> compteOpt = compteRepository.findById(userId);

        if (compteOpt.isEmpty()) {
            log.warn("⚠️ Compte introuvable pour userId: {}", userId);
            return null;
        }

        Compte compte = compteOpt.get();

        // Récupère la première playlist active avec config valide
        Playlist playlist = compte.getPremierPlaylistActive();

        if (playlist == null || !playlist.hasXtreamConfig()) {
            log.warn("⚠️ Aucune playlist active avec config Xtream pour userId: {}", userId);
            return null;
        }

        // Convertit la playlist en UserXtreamConfig
        return UserXtreamConfig.builder()
                .baseUrl(playlist.getXtreamBaseUrl())
                .username(playlist.getXtreamUsername())
                .password(playlist.getXtreamPassword())
                .build();
    }

    /**
     * Récupère la configuration Xtream ou lance une exception
     */
    public UserXtreamConfig getUserXtreamConfigOrThrow(String userId) {
        UserXtreamConfig config = getUserXtreamConfig(userId);

        if (config == null) {
            throw new RuntimeException(
                    "Configuration Xtream non trouvée pour l'utilisateur. " +
                            "Veuillez ajouter une playlist avec des credentials Xtream valides."
            );
        }

        return config;
    }

    /**
     * Récupère une playlist spécifique d'un utilisateur
     */
    public Optional<Playlist> getPlaylistById(String userId, String playlistId) {
        Optional<Compte> compteOpt = compteRepository.findById(userId);

        if (compteOpt.isEmpty()) {
            return Optional.empty();
        }

        Compte compte = compteOpt.get();
        Playlist playlist = compte.trouverPlaylistParId(playlistId);

        return Optional.ofNullable(playlist);
    }

    /**
     * Récupère la config Xtream pour une playlist spécifique
     */
    public UserXtreamConfig getXtreamConfigForPlaylist(String userId, String playlistId) {
        Optional<Playlist> playlistOpt = getPlaylistById(userId, playlistId);

        if (playlistOpt.isEmpty() || !playlistOpt.get().hasXtreamConfig()) {
            log.warn("⚠️ Playlist introuvable ou sans config Xtream: {}", playlistId);
            return null;
        }

        Playlist playlist = playlistOpt.get();

        return UserXtreamConfig.builder()
                .baseUrl(playlist.getXtreamBaseUrl())
                .username(playlist.getXtreamUsername())
                .password(playlist.getXtreamPassword())
                .build();
    }

    /**
     * Vérifie si une playlist spécifique a une config Xtream valide
     */
    public boolean playlistHasValidConfig(String userId, String playlistId) {
        Optional<Playlist> playlistOpt = getPlaylistById(userId, playlistId);
        return playlistOpt.isPresent() && playlistOpt.get().hasXtreamConfig();
    }

    /**
     * Compte le nombre de playlists actives pour un utilisateur
     */
    public int getActivePlaylistCount(String userId) {
        Optional<Compte> compteOpt = compteRepository.findById(userId);

        if (compteOpt.isEmpty()) {
            return 0;
        }

        return compteOpt.get().getPlaylistsActives().size();
    }

    /**
     * Compte le nombre de playlists avec config Xtream valide
     */
    public int getValidXtreamPlaylistCount(String userId) {
        Optional<Compte> compteOpt = compteRepository.findById(userId);

        if (compteOpt.isEmpty()) {
            return 0;
        }

        return compteOpt.get().getPlaylistsAvecXtreamValide().size();
    }

    /**
     * Vérifie si l'utilisateur existe
     */
    public boolean userExists(String userId) {
        return compteRepository.existsById(userId);
    }

    /**
     * Vérifie si l'utilisateur a au moins une playlist
     */
    public boolean hasPlaylists(String userId) {
        Optional<Compte> compteOpt = compteRepository.findById(userId);
        return compteOpt.isPresent() && compteOpt.get().hasPlaylists();
    }
}