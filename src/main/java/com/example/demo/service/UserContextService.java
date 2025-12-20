package com.example.demo.service;

import com.example.demo.config.UserXtreamConfig;
import com.example.demo.model.Compte;
import com.example.demo.model.Playlist;
import com.example.demo.repository.CompteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service pour gérer le contexte utilisateur et sa configuration Xtream
 * Support multi-playlist avec fallback intelligent
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
     * (au moins une playlist avec credentials Xtream, même si inactive)
     */
    public boolean hasValidXtreamConfig(String userId) {
        Optional<Compte> compteOpt = compteRepository.findById(userId);

        if (compteOpt.isEmpty()) {
            log.warn("⚠️ Compte introuvable pour userId: {}", userId);
            return false;
        }

        Compte compte = compteOpt.get();

        // ✅ CHANGEMENT : Accepte les playlists actives OU inactives avec config valide
        if (compte.getPlaylists() == null || compte.getPlaylists().isEmpty()) {
            return false;
        }

        return compte.getPlaylists().stream()
                .anyMatch(Playlist::hasXtreamConfig);
    }

    /**
     * Récupère la configuration Xtream de l'utilisateur
     * Retourne la première playlist avec une config valide (active en priorité, sinon inactive)
     */
    public UserXtreamConfig getUserXtreamConfig(String userId) {
        Optional<Compte> compteOpt = compteRepository.findById(userId);

        if (compteOpt.isEmpty()) {
            log.warn("⚠️ Compte introuvable pour userId: {}", userId);
            return null;
        }

        Compte compte = compteOpt.get();

        if (compte.getPlaylists() == null || compte.getPlaylists().isEmpty()) {
            log.warn("⚠️ Aucune playlist pour userId: {}", userId);
            return null;
        }

        // ✅ AMÉLIORATION : Cherche d'abord une playlist active, sinon prend n'importe quelle playlist valide
        Playlist playlist = compte.getPlaylists().stream()
                .filter(p -> p.hasXtreamConfig() && p.isActive() && !p.isExpired())
                .findFirst()
                .orElse(
                        compte.getPlaylists().stream()
                                .filter(Playlist::hasXtreamConfig)
                                .findFirst()
                                .orElse(null)
                );

        if (playlist == null) {
            log.warn("⚠️ Aucune playlist avec config Xtream valide pour userId: {}", userId);
            return null;
        }

        log.info("✅ Config Xtream trouvée pour userId: {} (playlist: {})", userId, playlist.getNom());

        // Convertit la playlist en UserXtreamConfig
        return UserXtreamConfig.builder()
                .baseUrl(playlist.getXtreamBaseUrl())
                .username(playlist.getXtreamUsername())
                .password(playlist.getXtreamPassword())
                .build();
    }

    /**
     * Récupère la configuration Xtream ou lance une exception avec un message détaillé
     */
    public UserXtreamConfig getUserXtreamConfigOrThrow(String userId) {
        UserXtreamConfig config = getUserXtreamConfig(userId);

        if (config == null) {
            Optional<Compte> compteOpt = compteRepository.findById(userId);

            if (compteOpt.isEmpty()) {
                throw new RuntimeException("Compte introuvable.");
            }

            Compte compte = compteOpt.get();

            if (!compte.hasPlaylists()) {
                throw new RuntimeException(
                        "Aucune playlist configurée. Veuillez ajouter une playlist avec des credentials Xtream."
                );
            }

            // Diagnostique détaillé
            long totalPlaylists = compte.getNombrePlaylists();
            long playlistsAvecConfig = compte.getPlaylists().stream()
                    .filter(Playlist::hasXtreamConfig)
                    .count();
            long playlistsActives = compte.getPlaylists().stream()
                    .filter(Playlist::isActive)
                    .count();

            String message = String.format(
                    "Configuration Xtream invalide. Total playlists: %d, Avec config: %d, Actives: %d. " +
                            "Vérifiez que vos playlists ont des credentials Xtream valides.",
                    totalPlaylists, playlistsAvecConfig, playlistsActives
            );

            throw new RuntimeException(message);
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
     * Obtient la liste des playlists avec diagnostic
     */
    public List<String> getPlaylistDiagnostic(String userId) {
        Optional<Compte> compteOpt = compteRepository.findById(userId);

        if (compteOpt.isEmpty()) {
            return List.of("Compte introuvable");
        }

        Compte compte = compteOpt.get();

        if (!compte.hasPlaylists()) {
            return List.of("Aucune playlist configurée");
        }

        return compte.getPlaylists().stream()
                .map(p -> String.format(
                        "Playlist '%s' - Active: %b, Config Xtream: %b, Expirée: %b",
                        p.getNom(),
                        p.isActive(),
                        p.hasXtreamConfig(),
                        p.isExpired()
                ))
                .collect(Collectors.toList());
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