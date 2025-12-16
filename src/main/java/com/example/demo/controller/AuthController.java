package com.example.demo.controller;


import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.GoogleLoginRequest;
import com.example.demo.model.Compte;
import com.example.demo.service.CompteService;
import com.example.demo.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.*;

import java.util.Map;
import java.util.Optional;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private final CompteService compteService;
    private final JwtUtil jwtUtil;

    /**
     * 🔐 Login classique (email + password)
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            // Vérifier les credentials
            boolean isValid = compteService.verifierCredentials(
                    loginRequest.getEmail(),
                    loginRequest.getPassword()
            );

            if (!isValid) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                                "success", false,
                                "message", "Email ou mot de passe incorrect"
                        ));
            }

            // Récupérer le compte
            Optional<Compte> compteOpt = compteService.trouverParEmail(loginRequest.getEmail());
            if (compteOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "success", false,
                                "message", "Compte introuvable"
                        ));
            }

            Compte compte = compteOpt.get();

            // Vérifier si le compte est actif
            if (!compte.isActive()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                                "success", false,
                                "message", "Compte désactivé"
                        ));
            }

            // Vérifier si le compte est expiré (pour les comptes payants)
            if (compte.isExpired()) {
                compte.setStatus("NON_PAYANT");
                compte.setActive(false);
                compteService.supprimerCompte(compte.getId()); // Met à jour

                return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                        .body(Map.of(
                                "success", false,
                                "message", "Votre abonnement a expiré"
                        ));
            }

            // Générer le JWT token
            String token = jwtUtil.generateToken(compte.getEmail(), compte.getId());

            // Construire la réponse
            AuthResponse response = AuthResponse.builder()
                    .success(true)
                    .message("Connexion réussie")
                    .token(token)
                    .userId(compte.getId())
                    .email(compte.getEmail())
                    .nom(compte.getNom())
                    .prenom(compte.getPrenom())
                    .url(compte.getUrl())
                    .status(compte.getStatus())
                    .isPayant(compte.isPayant())
                    .dateExpiration(compte.getDateExpiration())
                    .provider(compte.getProvider())
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Erreur lors de la connexion: " + e.getMessage()
                    ));
        }
    }

    /**
     * 📝 Inscription classique
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            // Vérifier si l'email existe déjà
            Optional<Compte> existant = compteService.trouverParEmail(registerRequest.getEmail());
            if (existant.isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of(
                                "success", false,
                                "message", "Cet email est déjà utilisé"
                        ));
            }

            // Créer le compte
            Compte compte = compteService.creerCompte(
                    registerRequest.getEmail(),
                    registerRequest.getPassword(),
                    registerRequest.getNom(),
                    registerRequest.getPrenom()
            );

            // Si numéro de téléphone fourni
            if (registerRequest.getTelephone() != null && !registerRequest.getTelephone().isEmpty()) {
                compte.setTelephone(registerRequest.getTelephone());
            }

            // Générer le JWT token
            String token = jwtUtil.generateToken(compte.getEmail(), compte.getId());

            // Construire la réponse
            AuthResponse response = AuthResponse.builder()
                    .success(true)
                    .message("Inscription réussie")
                    .token(token)
                    .userId(compte.getId())
                    .email(compte.getEmail())
                    .nom(compte.getNom())
                    .prenom(compte.getPrenom())
                    .url(compte.getUrl())
                    .status(compte.getStatus())
                    .isPayant(false)
                    .provider("LOCAL")
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Erreur lors de l'inscription: " + e.getMessage()
                    ));
        }
    }

    /**
     * 🔐 Login avec Google OAuth2
     */
    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@Valid @RequestBody GoogleLoginRequest googleRequest) {
        try {
            // Vérifier si le compte Google existe déjà
            Optional<Compte> compteOpt = compteService.trouverParEmail(googleRequest.getEmail());

            Compte compte;
            boolean isNewUser = false;

            if (compteOpt.isEmpty()) {
                // Créer un nouveau compte Google
                compte = compteService.creerCompteGoogle(
                        googleRequest.getEmail(),
                        googleRequest.getGoogleId(),
                        googleRequest.getNom()
                );
                isNewUser = true;
            } else {
                compte = compteOpt.get();

                // Si le compte existe mais n'est pas un compte Google, mettre à jour
                if (!"GOOGLE".equals(compte.getProvider())) {
                    compte.setGoogleId(googleRequest.getGoogleId());
                    compte.setProvider("GOOGLE");
                }
            }

            // Vérifier si le compte est actif
            if (!compte.isActive()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                                "success", false,
                                "message", "Compte désactivé"
                        ));
            }

            // Générer le JWT token
            String token = jwtUtil.generateToken(compte.getEmail(), compte.getId());

            // Construire la réponse
            AuthResponse response = AuthResponse.builder()
                    .success(true)
                    .message(isNewUser ? "Compte Google créé avec succès" : "Connexion Google réussie")
                    .token(token)
                    .userId(compte.getId())
                    .email(compte.getEmail())
                    .nom(compte.getNom())
                    .prenom(compte.getPrenom())
                    .url(compte.getUrl())
                    .status(compte.getStatus())
                    .isPayant(compte.isPayant())
                    .dateExpiration(compte.getDateExpiration())
                    .provider("GOOGLE")
                    .isNewUser(isNewUser)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Erreur lors de la connexion Google: " + e.getMessage()
                    ));
        }
    }

    /**
     * 🔄 Refresh Token
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Token manquant"));
            }

            String token = authHeader.substring(7);

            // Vérifier la validité du token
            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Token invalide ou expiré"));
            }

            // Extraire l'email et régénérer un nouveau token
            String email = jwtUtil.extractEmail(token);
            String userId = jwtUtil.extractUserId(token);
            String newToken = jwtUtil.generateToken(email, userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Token rafraîchi avec succès",
                    "token", newToken
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Erreur lors du refresh: " + e.getMessage()
                    ));
        }
    }

    /**
     * 🔍 Vérifier la validité d'un token
     */
    @GetMapping("/verify")
    public ResponseEntity<?> verifyToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Token manquant"));
            }

            String token = authHeader.substring(7);
            boolean isValid = jwtUtil.validateToken(token);

            if (isValid) {
                String email = jwtUtil.extractEmail(token);
                String userId = jwtUtil.extractUserId(token);

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Token valide",
                        "email", email,
                        "userId", userId
                ));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Token invalide"));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "success", false,
                            "message", "Erreur de validation: " + e.getMessage()
                    ));
        }
    }

    /**
     * 🚪 Logout (côté client uniquement - supprimer le token)
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Déconnexion réussie"
        ));
    }

    /**
     * 🔄 Réinitialisation du mot de passe (TODO)
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        // TODO: Implémenter l'envoi d'email pour réinitialisation
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Email de réinitialisation envoyé (à implémenter)"
        ));
    }
}


