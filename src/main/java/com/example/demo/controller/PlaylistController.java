package com.example.demo.controller;

import com.example.demo.dto.AddPlaylistRequest;
import com.example.demo.model.Compte;
import com.example.demo.service.CompteService;
import com.example.demo.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/playlists")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class PlaylistController {

    private final CompteService compteService;
    private final JwtUtil jwtUtil;

    /**
     * ➕ Ajouter une playlist
     */
    @PostMapping("/add")
    public ResponseEntity<?> addPlaylist(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody AddPlaylistRequest request) {

        try {
            String token = authHeader.substring(7);
            String userId = jwtUtil.extractUserId(token);

            Compte compte = compteService.ajouterPlaylist(
                    userId,
                    request.getNom(),
                    request.getXtreamBaseUrl(),
                    request.getXtreamUsername(),
                    request.getXtreamPassword(),
                    request.getDateExpiration()
            );

            return ResponseEntity.ok(Map.of(
                    "message", "Playlist ajoutée avec succès",
                    "nombrePlaylists", compte.getNombrePlaylists()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message", "Erreur lors de l'ajout de la playlist: " + e.getMessage()
                    ));
        }
    }

    /**
     * 📋 Lister les playlists
     */
    @GetMapping("/list")
    public ResponseEntity<?> listPlaylists(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String email = jwtUtil.extractEmail(token);

            Compte compte = compteService.trouverParEmail(email)
                    .orElseThrow(() -> new RuntimeException("Compte introuvable"));

            return ResponseEntity.ok(Map.of(
                    "playlists", compte.getPlaylists(),
                    "total", compte.getNombrePlaylists()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message", "Erreur: " + e.getMessage()
                    ));
        }
    }

    /**
     * 🔄 Mettre à jour une playlist
     */
    @PutMapping("/{playlistId}")
    public ResponseEntity<?> updatePlaylist(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String playlistId,
            @Valid @RequestBody AddPlaylistRequest request) {

        try {
            String token = authHeader.substring(7);
            String userId = jwtUtil.extractUserId(token);

            compteService.mettreAJourPlaylist(
                    userId,
                    playlistId,
                    request.getNom(),
                    request.getXtreamBaseUrl(),
                    request.getXtreamUsername(),
                    request.getXtreamPassword(),
                    request.getDateExpiration()
            );

            return ResponseEntity.ok(Map.of(
                    "message", "Playlist mise à jour avec succès"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message", "Erreur: " + e.getMessage()
                    ));
        }
    }

    /**
     * 🗑️ Supprimer une playlist
     */
    @DeleteMapping("/{playlistId}")
    public ResponseEntity<?> deletePlaylist(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String playlistId) {

        try {
            String token = authHeader.substring(7);
            String userId = jwtUtil.extractUserId(token);

            compteService.supprimerPlaylist(userId, playlistId);

            return ResponseEntity.ok(Map.of(
                    "message", "Playlist supprimée avec succès"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message", "Erreur: " + e.getMessage()
                    ));
        }
    }

    /**
     * ⭐ Ajouter un favori à une playlist
     */
    @PostMapping("/{playlistId}/favoris")
    public ResponseEntity<?> addFavorite(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String playlistId,
            @RequestBody Map<String, String> payload) {

        try {
            String token = authHeader.substring(7);
            String userId = jwtUtil.extractUserId(token);

            String idContenu = payload.get("idContenu");
            String nomContenu = payload.get("nomContenu");
            String type = payload.get("type");

            compteService.ajouterFavoriAPlaylist(userId, playlistId, idContenu, nomContenu, type);

            return ResponseEntity.ok(Map.of(
                    "message", "Favori ajouté avec succès"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message", "Erreur: " + e.getMessage()
                    ));
        }
    }

    /**
     * 🗑️ Retirer un favori d'une playlist
     */
    @DeleteMapping("/{playlistId}/favoris/{idContenu}")
    public ResponseEntity<?> removeFavorite(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String playlistId,
            @PathVariable String idContenu) {

        try {
            String token = authHeader.substring(7);
            String userId = jwtUtil.extractUserId(token);

            compteService.retirerFavoriDePlaylist(userId, playlistId, idContenu);

            return ResponseEntity.ok(Map.of(
                    "message", "Favori retiré avec succès"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message", "Erreur: " + e.getMessage()
                    ));
        }
    }
}



