package com.example.demo.service;


import com.example.demo.model.Otp;
import com.example.demo.repository.OtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpRepository otpRepository;
    private final EmailService emailService; // Service d'envoi d'email (à créer)

    private static final int OTP_LENGTH = 6;
    private static final int OTP_VALIDITY_MINUTES = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Génère un code OTP à 6 chiffres
     */
    private String genererCodeOTP() {
        int code = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * Crée et envoie un OTP pour un email
     */
    @Transactional
    public String creerEtEnvoyerOTP(String email) {
        try {
            // Invalider les anciens OTP de cet email
            invalidateOldOTPs(email);

            // Générer le code OTP
            String code = genererCodeOTP();

            // Créer l'entité OTP
            Otp otp = Otp.builder()
                    .email(email)
                    .code(code)
                    .dateCreation(LocalDateTime.now())
                    .dateExpiration(LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES))
                    .isUsed(false)
                    .tentativesEchouees(0)
                    .build();

            // Sauvegarder en base
            otpRepository.save(otp);

            // Envoyer l'email
            emailService.envoyerOTP(email, code);

            log.info("✅ OTP créé et envoyé pour l'email: {}", email);
            return code; // En production, ne pas retourner le code !

        } catch (Exception e) {
            log.error("❌ Erreur lors de la création de l'OTP pour {}: {}", email, e.getMessage());
            throw new RuntimeException("Erreur lors de l'envoi de l'OTP", e);
        }
    }

    /**
     * Invalide tous les anciens OTP d'un email
     */
    private void invalidateOldOTPs(String email) {
        otpRepository.findByEmail(email).forEach(otp -> {
            otp.setUsed(true);
            otpRepository.save(otp);
        });
    }

    /**
     * Vérifie un code OTP pour un email
     */
    @Transactional
    public boolean verifierOTP(String email, String code) {
        try {
            Optional<Otp> otpOpt = otpRepository.findFirstByEmailOrderByDateCreationDesc(email);

            if (otpOpt.isEmpty()) {
                log.warn("⚠️ Aucun OTP trouvé pour l'email: {}", email);
                return false;
            }

            Otp otp = otpOpt.get();

            // Vérifier si l'OTP est valide
            if (!otp.isValid()) {
                log.warn("⚠️ OTP invalide (expiré ou déjà utilisé) pour: {}", email);
                return false;
            }

            // Vérifier le code
            if (!otp.getCode().equals(code)) {
                otp.incrementerTentativesEchouees();
                otpRepository.save(otp);
                log.warn("⚠️ Code OTP incorrect pour: {} (tentative {}/3)", email, otp.getTentativesEchouees());
                return false;
            }

            // Code correct - marquer comme utilisé
            otp.marquerCommeUtilise();
            otpRepository.save(otp);

            log.info("✅ OTP vérifié avec succès pour: {}", email);
            return true;

        } catch (Exception e) {
            log.error("❌ Erreur lors de la vérification de l'OTP pour {}: {}", email, e.getMessage());
            return false;
        }
    }

    /**
     * Renvoie un nouvel OTP
     */
    @Transactional
    public String renvoyerOTP(String email) {
        log.info("🔄 Renvoi d'un nouvel OTP pour: {}", email);
        return creerEtEnvoyerOTP(email);
    }

    /**
     * Vérifie si un email a un OTP valide en attente
     */
    public boolean hasValidOTP(String email) {
        Optional<Otp> otpOpt = otpRepository.findByEmailAndIsUsedFalseAndDateExpirationAfter(
                email,
                LocalDateTime.now()
        );
        return otpOpt.isPresent();
    }

    /**
     * Nettoie les OTP expirés (à exécuter périodiquement)
     */
    @Transactional
    public void nettoyerOTPExpires() {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(1);
            otpRepository.deleteByDateExpirationBefore(cutoffDate);
            log.info("🗑️ Nettoyage des OTP expirés effectué");
        } catch (Exception e) {
            log.error("❌ Erreur lors du nettoyage des OTP: {}", e.getMessage());
        }
    }

    /**
     * Obtient le temps restant avant expiration d'un OTP (en secondes)
     */
    public long getTempsRestant(String email) {
        Optional<Otp> otpOpt = otpRepository.findFirstByEmailOrderByDateCreationDesc(email);

        if (otpOpt.isEmpty() || !otpOpt.get().isValid()) {
            return 0;
        }

        Otp otp = otpOpt.get();
        LocalDateTime now = LocalDateTime.now();

        if (now.isAfter(otp.getDateExpiration())) {
            return 0;
        }

        return java.time.Duration.between(now, otp.getDateExpiration()).getSeconds();
    }
}