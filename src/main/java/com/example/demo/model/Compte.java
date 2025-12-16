package com.example.demo.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "comptes")
public class Compte {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String password; // Hash du mot de passe

    @Indexed(unique = true)
    private String url; // URL unique pour chaque utilisateur

    private String status; // "PAYANT" ou "NON_PAYANT"

    private List<Favori> favoris = new ArrayList<>();

    private String nom;
    private String prenom;
    private String telephone;

    private LocalDateTime dateCreation = LocalDateTime.now();
    private LocalDateTime dateExpiration; // Pour les comptes payants

    private boolean isActive = true;

    // Informations supplémentaires
    private String googleId; // Pour OAuth Google
    private String provider = "LOCAL"; // "LOCAL" ou "GOOGLE"

    // Constructeur pour génération automatique d'URL unique
    public Compte(String email, String password) {
        this.email = email;
        this.password = password;
        this.url = generateUniqueUrl();
        this.status = "NON_PAYANT";
        this.favoris = new ArrayList<>();
        this.provider = "LOCAL";
    }

    // Génération d'URL unique
    private String generateUniqueUrl() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    // Méthode pour ajouter un favori
    public void ajouterFavori(String idContenu, String nomContenu, String type) {
        Favori favori = new Favori(idContenu, nomContenu, type);
        if (!favoris.contains(favori)) {
            favoris.add(favori);
        }
    }

    // Méthode pour retirer un favori
    public void retirerFavori(String idContenu) {
        favoris.removeIf(f -> f.getIdContenu().equals(idContenu));
    }

    // Vérifier si le compte est expiré
    public boolean isExpired() {
        if ("NON_PAYANT".equals(status)) {
            return false;
        }
        return dateExpiration != null && LocalDateTime.now().isAfter(dateExpiration);
    }

    // Vérifier si le compte est payant
    public boolean isPayant() {
        return "PAYANT".equals(status);
    }

    // Vérifier si c'est un compte Google
    public boolean isGoogleAccount() {
        return "GOOGLE".equals(provider);
    }
}