package com.example.demo.controller;


import com.example.demo.dto.ForgotPasswordRequest;
import com.example.demo.dto.ResetPasswordRequest;
import com.example.demo.dto.VerifyResetOtpRequest;
import com.example.demo.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller pour la réinitialisation du mot de passe
 */
@RestController
@RequestMapping("/api/auth/password")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Slf4j
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    /**
     * Étape 1 : Demander un OTP de réinitialisation
     * POST /api/auth/password/forgot
     */
    @PostMapping("/forgot")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            log.info("📧 Demande de réinitialisation pour: {}", request.getEmail());

            passwordResetService.sendResetOtp(request.getEmail());

            return ResponseEntity.ok(Map.of(
                    "message", "Un code de vérification a été envoyé à votre email",
                    "email", request.getEmail()
            ));

        } catch (Exception e) {
            log.error("❌ Erreur forgot password: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Étape 2 : Vérifier l'OTP de réinitialisation (optionnel)
     * POST /api/auth/password/verify-otp
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyResetOtp(@Valid @RequestBody VerifyResetOtpRequest request) {
        try {
            log.info("🔍 Vérification OTP pour: {}", request.getEmail());

            passwordResetService.verifyResetOtp(request.getEmail(), request.getCode());

            return ResponseEntity.ok(Map.of(
                    "message", "Code OTP vérifié avec succès",
                    "email", request.getEmail()
            ));

        } catch (Exception e) {
            log.error("❌ Erreur vérification OTP: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Étape 3 : Réinitialiser le mot de passe
     * POST /api/auth/password/reset
     */
    @PostMapping("/reset")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            log.info("🔑 Réinitialisation du mot de passe pour: {}", request.getEmail());

            passwordResetService.resetPassword(
                    request.getEmail(),
                    request.getCode(),
                    request.getNewPassword()
            );

            return ResponseEntity.ok(Map.of(
                    "message", "Mot de passe réinitialisé avec succès",
                    "email", request.getEmail()
            ));

        } catch (Exception e) {
            log.error("❌ Erreur reset password: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Renvoyer l'OTP de réinitialisation
     * POST /api/auth/password/resend-otp
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendResetOtp(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            log.info("🔄 Renvoi OTP de réinitialisation pour: {}", request.getEmail());

            passwordResetService.sendResetOtp(request.getEmail());

            return ResponseEntity.ok(Map.of(
                    "message", "Un nouveau code a été envoyé à votre email",
                    "email", request.getEmail()
            ));

        } catch (Exception e) {
            log.error("❌ Erreur resend OTP: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "message", e.getMessage()
            ));
        }
    }
}