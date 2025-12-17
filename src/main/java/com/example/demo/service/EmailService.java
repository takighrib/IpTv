package com.example.demo.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * Envoie un code OTP par email
     */
    public void envoyerOTP(String destinataire, String codeOTP) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@iptv-app.com");
            message.setTo(destinataire);
            message.setSubject("Code de vérification IPTV");
            message.setText(construireMessageOTP(codeOTP));

            mailSender.send(message);
            log.info("✅ Email OTP envoyé à: {}", destinataire);

        } catch (Exception e) {
            log.error("❌ Erreur lors de l'envoi de l'email OTP à {}: {}", destinataire, e.getMessage());
            throw new RuntimeException("Erreur lors de l'envoi de l'email", e);
        }
    }

    /**
     * Construit le message de l'email OTP
     */
    private String construireMessageOTP(String codeOTP) {
        return String.format("""
                Bonjour,
                
                Votre code de vérification IPTV est: %s
                
                Ce code est valide pendant 10 minutes.
                
                Si vous n'avez pas demandé ce code, veuillez ignorer cet email.
                
                Cordialement,
                L'équipe IPTV
                """, codeOTP);
    }

    /**
     * Envoie un email de bienvenue après inscription
     */
    public void envoyerEmailBienvenue(String destinataire, String prenom) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@iptv-app.com");
            message.setTo(destinataire);
            message.setSubject("Bienvenue sur IPTV !");
            message.setText(construireMessageBienvenue(prenom));

            mailSender.send(message);
            log.info("✅ Email de bienvenue envoyé à: {}", destinataire);

        } catch (Exception e) {
            log.error("❌ Erreur lors de l'envoi de l'email de bienvenue: {}", e.getMessage());
        }
    }

    /**
     * Construit le message de bienvenue
     */
    private String construireMessageBienvenue(String prenom) {
        return String.format("""
                Bonjour %s,
                
                Bienvenue sur IPTV !
                
                Votre compte a été créé avec succès. Vous pouvez maintenant :
                - Ajouter vos playlists IPTV
                - Gérer vos favoris
                - Profiter de tous nos services
                
                Merci de nous faire confiance.
                
                Cordialement,
                L'équipe IPTV
                """, prenom != null ? prenom : "");
    }

    /**
     * Envoie un email de réinitialisation de mot de passe
     */
    public void envoyerEmailReinitialisationMotDePasse(String destinataire, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@iptv-app.com");
            message.setTo(destinataire);
            message.setSubject("Réinitialisation de votre mot de passe");
            message.setText(construireMessageReinitialisationMotDePasse(token));

            mailSender.send(message);
            log.info("✅ Email de réinitialisation envoyé à: {}", destinataire);

        } catch (Exception e) {
            log.error("❌ Erreur lors de l'envoi de l'email de réinitialisation: {}", e.getMessage());
            throw new RuntimeException("Erreur lors de l'envoi de l'email", e);
        }
    }

    /**
     * Construit le message de réinitialisation
     */
    private String construireMessageReinitialisationMotDePasse(String token) {
        return String.format("""
                Bonjour,
                
                Vous avez demandé à réinitialiser votre mot de passe.
                
                Utilisez le code suivant: %s
                
                Ce code est valide pendant 30 minutes.
                
                Si vous n'avez pas demandé cette réinitialisation, veuillez ignorer cet email.
                
                Cordialement,
                L'équipe IPTV
                """, token);
    }
}


