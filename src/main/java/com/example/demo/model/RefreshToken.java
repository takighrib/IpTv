package com.example.demo.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "refresh_tokens")
public class RefreshToken {

    @Id
    private String id;

    /**
     * Le token de rafraîchissement (UUID unique)
     */
    @Indexed(unique = true)
    private String token;

    /**
     * ID de l'utilisateur propriétaire de ce token
     */
    @Indexed
    private String userId;

    /**
     * Email de l'utilisateur (pour faciliter les requêtes)
     */
    private String email;

    /**
     * Date de création du token
     */
    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();

    /**
     * Date d'expiration du token (ex: 7 jours après création)
     */
    private LocalDateTime dateExpiration;

    /**
     * Indique si le token a été révoqué (déconnexion manuelle)
     */
    @Builder.Default
    private boolean isRevoked = false;

    /**
     * Dernière utilisation du token (pour tracking)
     */
    private LocalDateTime derniereUtilisation;

    /**
     * Informations sur l'appareil (optionnel)
     */
    private String userAgent;
    private String ipAddress;

    /**
     * Vérifie si le token est expiré
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(dateExpiration);
    }

    /**
     * Vérifie si le token est valide (non expiré et non révoqué)
     */
    public boolean isValid() {
        return !isExpired() && !isRevoked;
    }

    /**
     * Marque le token comme révoqué
     */
    public void revoquer() {
        this.isRevoked = true;
    }

    /**
     * Met à jour la dernière utilisation
     */
    public void mettreAJourUtilisation() {
        this.derniereUtilisation = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "RefreshToken{" +
                "userId='" + userId + '\'' +
                ", email='" + email + '\'' +
                ", isRevoked=" + isRevoked +
                ", isExpired=" + isExpired() +
                ", dateExpiration=" + dateExpiration +
                '}';
    }
}