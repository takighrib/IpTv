package com.example.demo.controller;


import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.AuthResponse;
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


import com.example.demo.dto.*;
import com.example.demo.service.OtpService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private final CompteService compteService;
    private final OtpService otpService;
    private final JwtUtil jwtUtil;

    /**
     * 📝 Étape 1 : Inscription initiale (envoie l'OTP)
     */
    @PostMapping("/register/step1")
    public ResponseEntity<?> registerStepOne(@Valid @RequestBody RegisterStepOneRequest request) {
        try {
            // Créer le compte (non actif)
            compteService.creerCompteNonVerifie(
                    request.getEmail(),
                    request.getPassword(),
                    request.getNom(),
                    request.getPrenom()
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Un code de vérification a été envoyé à votre email",
                    "email", request.getEmail()
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Erreur lors de l'inscription: " + e.getMessage()
                    ));
        }
    }

    /**
     * ✅ Étape 2 : Vérification de l'OTP et activation du compte
     */
    @PostMapping("/register/verify")
    public ResponseEntity<?> verifyOTP(@Valid @RequestBody VerifyOTPRequest request) {
        try {
            // Vérifier l'OTP et activer le compte
            Compte compte = compteService.verifierEmailEtActiverCompte(
                    request.getEmail(),
                    request.getCode()
            );

            // Générer le JWT token
            String token = jwtUtil.generateToken(compte.getEmail(), compte.getId());

            // Construire la réponse
            AuthResponse response = AuthResponse.builder()
                    .success(true)
                    .message("Inscription réussie ! Votre email a été vérifié.")
                    .token(token)
                    .userId(compte.getId())
                    .email(compte.getEmail())
                    .nom(compte.getNom())
                    .prenom(compte.getPrenom())
                    .isEmailVerified(true)
                    .hasPlaylists(compte.hasPlaylists())
                    .build();

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Erreur lors de la vérification: " + e.getMessage()
                    ));
        }
    }

    /**
     * 🔄 Renvoyer un OTP
     */
    @PostMapping("/register/resend-otp")
    public ResponseEntity<?> resendOTP(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            if (email == null || email.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Email requis"));
            }

            compteService.renvoyerOTP(email);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Un nouveau code a été envoyé à votre email"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Erreur lors du renvoi de l'OTP: " + e.getMessage()
                    ));
        }
    }

    /**
     * 🔐 Login
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

            // Vérifier si l'email est vérifié
            if (!compte.isEmailVerified()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                                "success", false,
                                "message", "Email non vérifié. Veuillez vérifier votre email.",
                                "needsEmailVerification", true
                        ));
            }

            // Vérifier si le compte est actif
            if (!compte.isActive()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                                "success", false,
                                "message", "Compte désactivé"
                        ));
            }

            // Générer le token
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
                    .isEmailVerified(compte.isEmailVerified())
                    .hasPlaylists(compte.hasPlaylists())
                    .nombrePlaylists(compte.getNombrePlaylists())
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

            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Token invalide ou expiré"));
            }

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
     * 🚪 Logout
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Déconnexion réussie"
        ));
    }

    @PostMapping("/delete")
    public ResponseEntity<?> deleteAccount(@RequestParam String email) {
        try {
            if (compteService.deleteAccount(email)) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Account is deleted with success!"
                ));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Error while deleting the account with email: " + email + e.getMessage()
                    ));
        }
    }
}