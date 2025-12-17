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
@Document(collection = "otps")
public class Otp {

    @Id
    private String id;

    @Indexed
    private String email;

    private String code; // Code OTP à 6 chiffres

    /**
     * Type d'OTP
     * - "REGISTRATION" : pour l'inscription
     * - "PASSWORD_RESET" : pour la réinitialisation de mot de passe
     */
    @Builder.Default
    private String type = "REGISTRATION";

    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime dateExpiration = LocalDateTime.now().plusMinutes(10); // Expire après 10 minutes

    @Builder.Default
    private boolean isUsed = false; // Indique si l'OTP a été utilisé

    @Builder.Default
    private int tentativesEchouees = 0; // Nombre de tentatives échouées

    private static final int MAX_TENTATIVES = 3;

    /**
     * Vérifie si l'OTP est expiré
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(dateExpiration);
    }

    /**
     * Vérifie si l'OTP est valide (non utilisé, non expiré, pas trop de tentatives)
     */
    public boolean isValid() {
        return !isUsed && !isExpired() && tentativesEchouees < MAX_TENTATIVES;
    }

    /**
     * Incrémente le nombre de tentatives échouées
     */
    public void incrementerTentativesEchouees() {
        this.tentativesEchouees++;
    }

    /**
     * Marque l'OTP comme utilisé
     */
    public void marquerCommeUtilise() {
        this.isUsed = true;
    }

    @Override
    public String toString() {
        return "Otp{" +
                "email='" + email + '\'' +
                ", type='" + type + '\'' +
                ", isUsed=" + isUsed +
                ", isExpired=" + isExpired() +
                ", tentativesEchouees=" + tentativesEchouees +
                ", dateExpiration=" + dateExpiration +
                '}';
    }
}




