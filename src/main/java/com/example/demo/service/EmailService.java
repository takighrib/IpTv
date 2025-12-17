package com.example.demo.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

import org.springframework.mail.javamail.MimeMessageHelper;
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    @Value("${spring.mail.username}")
    private String fromEmail;

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

    public void sendPasswordResetEmail(String to, String otpCode, String prenom) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("🔐 Réinitialisation de votre mot de passe - IPTV");

            String htmlContent = String.format("""
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                    <div style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 30px; border-radius: 10px 10px 0 0; text-align: center;">
                        <h1 style="color: white; margin: 0; font-size: 28px;">🔐 Réinitialisation du mot de passe</h1>
                    </div>
                    
                    <div style="background: #f8f9fa; padding: 30px; border-radius: 0 0 10px 10px;">
                        <p style="font-size: 16px; color: #333;">Bonjour <strong>%s</strong>,</p>
                        
                        <p style="font-size: 16px; color: #333;">
                            Nous avons reçu une demande de réinitialisation de mot de passe pour votre compte IPTV.
                        </p>
                        
                        <div style="background: white; padding: 25px; border-radius: 8px; margin: 25px 0; text-align: center; border: 2px solid #667eea;">
                            <p style="color: #666; margin-bottom: 10px; font-size: 14px;">Votre code de vérification :</p>
                            <h2 style="color: #667eea; font-size: 36px; letter-spacing: 10px; margin: 15px 0; font-weight: bold;">%s</h2>
                            <p style="color: #999; font-size: 14px; margin-top: 10px;">
                                ⏰ Ce code expire dans <strong style="color: #e74c3c;">10 minutes</strong>
                            </p>
                        </div>
                        
                        <div style="background: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; border-radius: 4px;">
                            <p style="margin: 0; color: #856404; font-size: 14px;">
                                ⚠️ <strong>Important :</strong> Si vous n'avez pas demandé cette réinitialisation, 
                                veuillez ignorer cet email. Votre mot de passe restera inchangé.
                            </p>
                        </div>
                        
                        <div style="background: #e8f4fd; border-left: 4px solid #2196F3; padding: 15px; margin: 20px 0; border-radius: 4px;">
                            <p style="margin: 0; color: #0d47a1; font-size: 13px;">
                                💡 <strong>Conseil de sécurité :</strong> Choisissez un mot de passe fort 
                                d'au moins 8 caractères avec des lettres, chiffres et symboles.
                            </p>
                        </div>
                        
                        <p style="font-size: 14px; color: #666; margin-top: 30px;">
                            Cordialement,<br>
                            <strong>L'équipe IPTV</strong>
                        </p>
                    </div>
                    
                    <div style="text-align: center; margin-top: 20px; padding: 20px; color: #999; font-size: 12px;">
                        <p style="margin: 5px 0;">Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>
                        <p style="margin: 5px 0;">© 2024 IPTV - Tous droits réservés</p>
                    </div>
                </div>
                """, prenom, otpCode);

            helper.setText(htmlContent, true);
            mailSender.send(message);

            log.info("✅ Email de réinitialisation envoyé à: {}", to);

        } catch (Exception e) {
            log.error("❌ Erreur lors de l'envoi de l'email de réinitialisation: {}", e.getMessage());
            throw new RuntimeException("Erreur lors de l'envoi de l'email: " + e.getMessage());
        }
    }

    public void sendPasswordChangedConfirmation(String to, String prenom) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("✅ Votre mot de passe a été modifié - IPTV");

            String htmlContent = String.format("""
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                    <div style="background: linear-gradient(135deg, #28a745 0%%, #20c997 100%%); padding: 30px; border-radius: 10px 10px 0 0; text-align: center;">
                        <h1 style="color: white; margin: 0; font-size: 28px;">✅ Mot de passe modifié</h1>
                    </div>
                    
                    <div style="background: #f8f9fa; padding: 30px; border-radius: 0 0 10px 10px;">
                        <p style="font-size: 16px; color: #333;">Bonjour <strong>%s</strong>,</p>
                        
                        <p style="font-size: 16px; color: #333;">
                            Votre mot de passe a été modifié avec succès.
                        </p>
                        
                        <div style="background: #d4edda; border-left: 4px solid #28a745; padding: 15px; margin: 20px 0; border-radius: 4px;">
                            <p style="margin: 0; color: #155724; font-size: 14px;">
                                ✅ <strong>Confirmation :</strong> Vous pouvez maintenant vous connecter avec votre nouveau mot de passe.
                            </p>
                        </div>
                        
                        <div style="background: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; border-radius: 4px;">
                            <p style="margin: 0; color: #856404; font-size: 14px;">
                                ⚠️ <strong>Vous n'êtes pas à l'origine de ce changement ?</strong><br>
                                Contactez immédiatement notre support pour sécuriser votre compte.
                            </p>
                        </div>
                        
                        <p style="font-size: 14px; color: #666; margin-top: 30px;">
                            Cordialement,<br>
                            <strong>L'équipe IPTV</strong>
                        </p>
                    </div>
                    
                    <div style="text-align: center; margin-top: 20px; padding: 20px; color: #999; font-size: 12px;">
                        <p style="margin: 5px 0;">Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>
                        <p style="margin: 5px 0;">© 2024 IPTV - Tous droits réservés</p>
                    </div>
                </div>
                """, prenom);

            helper.setText(htmlContent, true);
            mailSender.send(message);

            log.info("✅ Email de confirmation envoyé à: {}", to);

        } catch (Exception e) {
            log.error("❌ Erreur lors de l'envoi de l'email de confirmation: {}", e.getMessage());
            // Ne pas throw d'exception ici car ce n'est pas critique
        }
    }

}


