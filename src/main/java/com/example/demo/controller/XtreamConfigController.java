package com.example.demo.controller;

import com.example.demo.dto.XtreamConfigRequest;
import com.example.demo.model.Compte;
import com.example.demo.service.CompteService;
import com.example.demo.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

import com.example.demo.dto.XtreamConfigRequest;
import com.example.demo.model.Compte;
import com.example.demo.service.CompteService;
import com.example.demo.service.UserContextService;
import com.example.demo.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * ⚠️ CONTROLLER OBSOLÈTE - Utiliser PlaylistController à la place
 *
 * Ce controller est maintenu pour compatibilité avec l'ancienne API,
 * mais il est recommandé d'utiliser /api/playlists pour gérer les configurations Xtream
 */
@RestController
@RequestMapping("/api/xtream-config")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class XtreamConfigController {

    private final CompteService compteService;
    private final UserContextService userContextService;
    private final JwtUtil jwtUtil;

    /**
     * ✅ Configurer les credentials Xtream pour un utilisateur
     * Crée automatiquement une playlist "Ma Playlist"
     */
    @PostMapping("/setup")
    public ResponseEntity<?> setupXtreamConfig(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody XtreamConfigRequest request) {

        try {
            // Extraire userId du token
            String token = authHeader.substring(7);
            String userId = jwtUtil.extractUserId(token);

            // Ajouter une playlist avec ces credentials
            Compte compte = compteService.ajouterPlaylist(
                    userId,
                    "Ma Playlist", // Nom par défaut
                    request.getXtreamBaseUrl(),
                    request.getXtreamUsername(),
                    request.getXtreamPassword(),
                    null // Pas de date d'expiration
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Configuration Xtream enregistrée avec succès",
                    "hasXtreamConfig", userContextService.hasValidXtreamConfig(userId),
                    "nombrePlaylists", compte.getNombrePlaylists()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Erreur lors de la configuration: " + e.getMessage()
                    ));
        }
    }

    /**
     * 🔄 Mettre à jour les credentials Xtream
     * Met à jour la première playlist ou en crée une nouvelle
     */
    @PutMapping("/update")
    public ResponseEntity<?> updateXtreamConfig(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody XtreamConfigRequest request) {

        try {
            String token = authHeader.substring(7);
            String userId = jwtUtil.extractUserId(token);

            // Récupérer le compte
            Optional<Compte> compteOpt = compteService.trouverParEmail(
                    jwtUtil.extractEmail(token)
            );

            if (compteOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "Compte introuvable"));
            }

            Compte compte = compteOpt.get();

            // Si l'utilisateur a des playlists, mettre à jour la première
            if (compte.hasPlaylists()) {
                String playlistId = compte.getPlaylists().get(0).getId();
                compteService.mettreAJourPlaylist(
                        userId,
                        playlistId,
                        null, // Garder le nom existant
                        request.getXtreamBaseUrl(),
                        request.getXtreamUsername(),
                        request.getXtreamPassword(),
                        null // Garder l'expiration existante
                );
            } else {
                // Sinon, créer une nouvelle playlist
                compteService.ajouterPlaylist(
                        userId,
                        "Ma Playlist",
                        request.getXtreamBaseUrl(),
                        request.getXtreamUsername(),
                        request.getXtreamPassword(),
                        null
                );
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Configuration Xtream mise à jour avec succès",
                    "hasXtreamConfig", userContextService.hasValidXtreamConfig(userId)
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Erreur lors de la mise à jour: " + e.getMessage()
                    ));
        }
    }

    /**
     * 🗑️ Supprimer la configuration Xtream
     * Supprime toutes les playlists de l'utilisateur
     */
    @DeleteMapping("/remove")
    public ResponseEntity<?> removeXtreamConfig(
            @RequestHeader("Authorization") String authHeader) {

        try {
            String token = authHeader.substring(7);
            String userId = jwtUtil.extractUserId(token);
            String email = jwtUtil.extractEmail(token);

            Optional<Compte> compteOpt = compteService.trouverParEmail(email);

            if (compteOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "Compte introuvable"));
            }

            Compte compte = compteOpt.get();

            // Supprimer toutes les playlists
            if (compte.hasPlaylists()) {
                for (int i = compte.getPlaylists().size() - 1; i >= 0; i--) {
                    String playlistId = compte.getPlaylists().get(i).getId();
                    compteService.supprimerPlaylist(userId, playlistId);
                }
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Configuration Xtream supprimée"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Erreur: " + e.getMessage()
                    ));
        }
    }

    /**
     * 🔍 Vérifier le statut de la configuration Xtream
     */
    @GetMapping("/status")
    public ResponseEntity<?> getXtreamConfigStatus(
            @RequestHeader("Authorization") String authHeader) {

        try {
            String token = authHeader.substring(7);
            String userId = jwtUtil.extractUserId(token);

            Optional<Compte> compteOpt = compteService.trouverParEmail(
                    jwtUtil.extractEmail(token)
            );

            if (compteOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "Compte introuvable"));
            }

            Compte compte = compteOpt.get();

            // Récupérer la première playlist avec config valide
            String baseUrl = "";
            String username = "";

            if (compte.hasPlaylists()) {
                for (var playlist : compte.getPlaylists()) {
                    if (playlist.hasXtreamConfig()) {
                        baseUrl = playlist.getXtreamBaseUrl();
                        username = playlist.getXtreamUsername();
                        break;
                    }
                }
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "hasXtreamConfig", userContextService.hasValidXtreamConfig(userId),
                    "nombrePlaylists", compte.getNombrePlaylists(),
                    "playlistsActives", userContextService.getActivePlaylistCount(userId),
                    "xtreamBaseUrl", baseUrl,
                    "xtreamUsername", username
                    // ⚠️ NE PAS RETOURNER LE MOT DE PASSE
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Erreur: " + e.getMessage()
                    ));
        }
    }
}

