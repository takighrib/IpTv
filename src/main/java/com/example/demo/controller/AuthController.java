package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.model.Compte;
import com.example.demo.model.RefreshToken;
import com.example.demo.service.CompteService;
import com.example.demo.service.OtpService;
import com.example.demo.service.RefreshTokenService;
import com.example.demo.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private final CompteService compteService;
    private final OtpService otpService;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;

    @Value("${jwt.refresh.expiration:7}")
    private int refreshExpirationDays;

    /**
     * Étape 1 : Inscription initiale (envoie l'OTP)
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
                    "message", "Un code de vérification a été envoyé à votre email",
                    "email", request.getEmail()
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "message", e.getMessage()
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message", "Erreur lors de l'inscription: " + e.getMessage()
                    ));
        }
    }

    /**
     * ✅ Étape 2 : Vérification de l'OTP et activation du compte
     */
    @PostMapping("/register/verify")
    public ResponseEntity<?> verifyOTP(@Valid @RequestBody VerifyOTPRequest request,
                                       HttpServletRequest httpRequest) {
        try {
            // Vérifier l'OTP et activer le compte
            Compte compte = compteService.verifierEmailEtActiverCompte(
                    request.getEmail(),
                    request.getCode()
            );

            // ✅ Générer Access Token
            String accessToken = jwtUtil.generateToken(compte.getEmail(), compte.getId());

            // ✅ Générer Refresh Token
            RefreshToken refreshToken = refreshTokenService.creerRefreshToken(
                    compte.getId(),
                    compte.getEmail(),
                    httpRequest.getHeader("User-Agent"),
                    httpRequest.getRemoteAddr()
            );

            // Construire la réponse
            AuthResponse response = AuthResponse.builder()
                    .success(true)
                    .message("Inscription réussie ! Votre email a été vérifié.")
                    .token(accessToken)
                    .refreshToken(refreshToken.getToken())
                    .userId(compte.getId())
                    .email(compte.getEmail())
                    .nom(compte.getNom())
                    .prenom(compte.getPrenom())
                    .isEmailVerified(true)
                    .hasPlaylists(compte.hasPlaylists())
                    .accessTokenExpiresIn(jwtUtil.getTokenExpirationInSeconds())
                    .refreshTokenExpiresIn((long) refreshExpirationDays * 24 * 60 * 60)
                    .build();

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "message", e.getMessage()
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
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
                        .body(Map.of( "message", "Email requis"));
            }

            compteService.renvoyerOTP(email);

            return ResponseEntity.ok(Map.of(
                    "message", "Un nouveau code a été envoyé à votre email"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message", "Erreur lors du renvoi de l'OTP: " + e.getMessage()
                    ));
        }
    }

    /**
     * 🔐 Login
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest,
                                   HttpServletRequest httpRequest) {
        try {
            // Vérifier les credentials
            boolean isValid = compteService.verifierCredentials(
                    loginRequest.getEmail(),
                    loginRequest.getPassword()
            );

            if (!isValid) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                                "message", "Email ou mot de passe incorrect"
                        ));
            }

            // Récupérer le compte
            Optional<Compte> compteOpt = compteService.trouverParEmail(loginRequest.getEmail());
            if (compteOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "message", "Compte introuvable"
                        ));
            }

            Compte compte = compteOpt.get();

            // Vérifier si l'email est vérifié
            if (!compte.isEmailVerified()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                                "message", "Email non vérifié. Veuillez vérifier votre email.",
                                "needsEmailVerification", true
                        ));
            }

            // Vérifier si le compte est actif
            if (!compte.isActive()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                                "message", "Compte désactivé"
                        ));
            }

            // ✅ Générer Access Token
            String accessToken = jwtUtil.generateToken(compte.getEmail(), compte.getId());

            // ✅ Générer Refresh Token
            RefreshToken refreshToken = refreshTokenService.creerRefreshToken(
                    compte.getId(),
                    compte.getEmail(),
                    httpRequest.getHeader("User-Agent"),
                    httpRequest.getRemoteAddr()
            );

            // Construire la réponse
            AuthResponse response = AuthResponse.builder()
                    .success(true)
                    .message("Connexion réussie")
                    .token(accessToken)
                    .refreshToken(refreshToken.getToken())
                    .userId(compte.getId())
                    .email(compte.getEmail())
                    .nom(compte.getNom())
                    .prenom(compte.getPrenom())
                    .isEmailVerified(compte.isEmailVerified())
                    .hasPlaylists(compte.hasPlaylists())
                    .nombrePlaylists(compte.getNombrePlaylists())
                    .accessTokenExpiresIn(jwtUtil.getTokenExpirationInSeconds())
                    .refreshTokenExpiresIn((long) refreshExpirationDays * 24 * 60 * 60)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message", "Erreur lors de la connexion: " + e.getMessage()
                    ));
        }
    }

    /**
     * ✅ NOUVEAU - Refresh Token Endpoint
     * Génère un nouveau Access Token à partir d'un Refresh Token valide
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request,
                                          HttpServletRequest httpRequest) {
        try {
            String refreshTokenValue = request.get("refreshToken");

            if (refreshTokenValue == null || refreshTokenValue.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                                "message", "Refresh token manquant"
                        ));
            }

            // Valider le refresh token
            RefreshToken refreshToken = refreshTokenService.validerRefreshToken(refreshTokenValue);

            // Générer un nouveau access token
            String newAccessToken = jwtUtil.generateToken(
                    refreshToken.getEmail(),
                    refreshToken.getUserId()
            );

            // Optionnel : Générer aussi un nouveau refresh token (rotation)
            RefreshToken newRefreshToken = refreshTokenService.creerRefreshToken(
                    refreshToken.getUserId(),
                    refreshToken.getEmail(),
                    httpRequest.getHeader("User-Agent"),
                    httpRequest.getRemoteAddr()
            );

            // Révoquer l'ancien refresh token (sécurité)
            refreshTokenService.revoquerToken(refreshTokenValue);

            return ResponseEntity.ok(Map.of(
                    "message", "Tokens rafraîchis avec succès",
                    "token", newAccessToken,
                    "refreshToken", newRefreshToken.getToken(),
                    "accessTokenExpiresIn", jwtUtil.getTokenExpirationInSeconds(),
                    "refreshTokenExpiresIn", (long) refreshExpirationDays * 24 * 60 * 60
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "message", e.getMessage()
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message", "Erreur lors du rafraîchissement: " + e.getMessage()
                    ));
        }
    }

    /**
     * 🔄 Refresh Access Token (ancienne méthode - maintenant obsolète mais gardée pour compatibilité)
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAccessToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of( "message", "Token manquant"));
            }

            String token = authHeader.substring(7);

            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Token invalide ou expiré"));
            }

            String email = jwtUtil.extractEmail(token);
            String userId = jwtUtil.extractUserId(token);
            String newToken = jwtUtil.generateToken(email, userId);

            return ResponseEntity.ok(Map.of(
                    "message", "Token rafraîchi avec succès",
                    "token", newToken,
                    "expiresIn", jwtUtil.getTokenExpirationInSeconds()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
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
                        "message", "Token valide",
                        "email", email,
                        "userId", userId,
                        "expiresIn", jwtUtil.getTimeUntilExpirationInSeconds(token)
                ));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of( "message", "Token invalide"));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "message", "Erreur de validation: " + e.getMessage()
                    ));
        }
    }

    /**
     * ✅ NOUVEAU - Logout (révoque le refresh token)
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody(required = false) Map<String, String> request) {
        try {
            if (request != null) {
                String refreshTokenValue = request.get("refreshToken");

                if (refreshTokenValue != null && !refreshTokenValue.isEmpty()) {
                    refreshTokenService.revoquerToken(refreshTokenValue);
                }
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Déconnexion réussie"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message", "Erreur lors de la déconnexion: " + e.getMessage()
                    ));
        }
    }

    /**
     * ✅ NOUVEAU - Logout Global (révoque tous les refresh tokens de l'utilisateur)
     */
    @PostMapping("/logout-all")
    public ResponseEntity<?> logoutAll(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of( "message", "Token manquant"));
            }

            String token = authHeader.substring(7);
            String userId = jwtUtil.extractUserId(token);

            refreshTokenService.revoquerTousLesTokens(userId);

            return ResponseEntity.ok(Map.of(
                    "message", "Déconnexion de tous les appareils réussie"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message", "Erreur lors de la déconnexion globale: " + e.getMessage()
                    ));
        }
    }

    /**
     * ✅ NOUVEAU - Obtenir les sessions actives
     */
    @GetMapping("/sessions")
    public ResponseEntity<?> getSessions(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "Token manquant"));
            }

            String token = authHeader.substring(7);
            String userId = jwtUtil.extractUserId(token);

            var sessions = refreshTokenService.getTokensActifs(userId);

            return ResponseEntity.ok(Map.of(
                    "sessions", sessions,
                    "count", sessions.size()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message", "Erreur: " + e.getMessage()
                    ));
        }
    }

    /**
     * 🗑️ Supprimer un compte
     */
    @PostMapping("/delete")
    public ResponseEntity<?> deleteAccount(@RequestParam String email) {
        try {
            if (compteService.deleteAccount(email)) {
                return ResponseEntity.ok(Map.of(
                        "message", "Account is deleted with success!"
                ));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message", "Error while deleting the account with email: " + email + e.getMessage()
                    ));
        }
    }
}