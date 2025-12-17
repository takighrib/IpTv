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

@RestController
@RequestMapping("/api/xtream-config")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class XtreamConfigController {

    private final CompteService compteService;
    private final JwtUtil jwtUtil;

    /**
     * ✅ Configurer les credentials Xtream pour un utilisateur
     */
    @PostMapping("/setup")
    public ResponseEntity<?> setupXtreamConfig(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody XtreamConfigRequest request) {

        try {
            // Extraire userId du token
            String token = authHeader.substring(7);
            String userId = jwtUtil.extractUserId(token);

            // Configurer Xtream
            Compte compte = compteService.configurerXtream(
                    userId,
                    request.getXtreamBaseUrl(),
                    request.getXtreamUsername(),
                    request.getXtreamPassword()
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Configuration Xtream enregistrée avec succès",
                    "hasXtreamConfig", compte.hasXtreamConfig()
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
     */
    @PutMapping("/update")
    public ResponseEntity<?> updateXtreamConfig(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody XtreamConfigRequest request) {

        return setupXtreamConfig(authHeader, request); // Même logique
    }

    /**
     * 🗑️ Supprimer la configuration Xtream
     */
    @DeleteMapping("/remove")
    public ResponseEntity<?> removeXtreamConfig(
            @RequestHeader("Authorization") String authHeader) {

        try {
            String token = authHeader.substring(7);
            String userId = jwtUtil.extractUserId(token);

            compteService.supprimerConfigXtream(userId);

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

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "hasXtreamConfig", compte.hasXtreamConfig(),
                    "xtreamBaseUrl", compte.getXtreamBaseUrl() != null ? compte.getXtreamBaseUrl() : "",
                    "xtreamUsername", compte.getXtreamUsername() != null ? compte.getXtreamUsername() : ""
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