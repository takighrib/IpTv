package com.example.demo.service;


import com.example.demo.model.Compte;
import com.example.demo.model.Otp;
import com.example.demo.repository.CompteRepository;
import com.example.demo.repository.OtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;
import com.example.demo.model.Compte;
import com.example.demo.model.Otp;
import com.example.demo.repository.CompteRepository;
import com.example.demo.repository.OtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

/**
 * Service pour gérer la réinitialisation du mot de passe
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final CompteRepository compteRepository;
    private final OtpRepository otpRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.dev-mode:false}")
    private boolean devMode;

    /**
     * Étape 1 : Envoyer l'OTP pour réinitialisation
     */
    public void sendResetOtp(String email) {
        log.info("📧 Demande de réinitialisation pour: {}", email);

        // Vérifier que le compte existe
        Compte compte = compteRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Aucun compte trouvé avec cet email"));

        // Vérifier que l'email est vérifié
        if (!compte.isEmailVerified()) {
            throw new RuntimeException("Veuillez d'abord vérifier votre email");
        }

        // Supprimer les anciens OTP de type PASSWORD_RESET pour cet email
        otpRepository.findByEmailAndType(email, "PASSWORD_RESET")
                .ifPresent(otpRepository::delete);

        // Générer un nouveau code OTP
        String code = generateOtpCode();

        // Créer et sauvegarder le nouvel OTP avec Builder
        Otp otp = Otp.builder()
                .email(email)
                .code(code)
                .type("PASSWORD_RESET")
                .dateCreation(LocalDateTime.now())
                .dateExpiration(LocalDateTime.now().plusMinutes(10))
                .isUsed(false)
                .tentativesEchouees(0)
                .build();

        otpRepository.save(otp);

        // Envoyer l'email
        if (devMode) {
            log.info("🔓 MODE DEV - Code OTP de réinitialisation pour {}: {}", email, code);
        } else {
            try {
                emailService.sendPasswordResetEmail(email, code, compte.getPrenom());
                log.info("✅ Email de réinitialisation envoyé à: {}", email);
            } catch (Exception e) {
                log.error("❌ Erreur envoi email: {}", e.getMessage());
                // En mode dev, on affiche le code même si l'envoi échoue
                log.info("🔓 CODE OTP (email failed): {}", code);
            }
        }
    }

    /**
     * Étape 2 : Vérifier l'OTP de réinitialisation
     */
    public boolean verifyResetOtp(String email, String code) {
        log.info("🔍 Vérification OTP de réinitialisation pour: {}", email);

        // Récupérer l'OTP
        Otp otp = otpRepository.findByEmailAndType(email, "PASSWORD_RESET")
                .orElseThrow(() -> new RuntimeException("Code OTP invalide ou expiré"));

        // Vérifier si expiré
        if (otp.isExpired()) {
            otpRepository.delete(otp);
            throw new RuntimeException("Le code OTP a expiré. Demandez un nouveau code.");
        }

        // Vérifier si déjà utilisé
        if (otp.isUsed()) {
            throw new RuntimeException("Ce code OTP a déjà été utilisé.");
        }

        // Vérifier le nombre de tentatives
        if (otp.getTentativesEchouees() >= 3) {
            otpRepository.delete(otp);
            throw new RuntimeException("Trop de tentatives. Demandez un nouveau code.");
        }

        // Vérifier le code
        if (!otp.getCode().equals(code)) {
            otp.incrementerTentativesEchouees();
            otpRepository.save(otp);
            throw new RuntimeException("Code OTP incorrect. Tentatives restantes: " + (3 - otp.getTentativesEchouees()));
        }

        log.info("✅ OTP vérifié avec succès pour: {}", email);
        return true;
    }

    /**
     * Étape 3 : Réinitialiser le mot de passe
     */
    public void resetPassword(String email, String code, String newPassword) {
        log.info("🔑 Réinitialisation du mot de passe pour: {}", email);

        // Vérifier l'OTP une dernière fois
        verifyResetOtp(email, code);

        // Récupérer le compte
        Compte compte = compteRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Compte introuvable"));

        // Changer le mot de passe
        compte.setPassword(passwordEncoder.encode(newPassword));
        compteRepository.save(compte);

        // Marquer l'OTP comme utilisé et le supprimer
        otpRepository.findByEmailAndType(email, "PASSWORD_RESET")
                .ifPresent(otp -> {
                    otp.marquerCommeUtilise();
                    otpRepository.delete(otp);
                });

        log.info("✅ Mot de passe réinitialisé avec succès pour: {}", email);
    }

    /**
     * Générer un code OTP à 6 chiffres
     */
    private String generateOtpCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
}






