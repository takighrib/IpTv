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
    private String password;

    @Indexed(unique = true)
    private String url; // URL unique du profil utilisateur

    // ✅ NOUVEAUX CHAMPS - Credentials Xtream de l'utilisateur
    private String xtreamBaseUrl;    // Ex: http://buysmart.tn:8080
    private String xtreamUsername;   // Ex: buysmart01370
    private String xtreamPassword;   // Ex: 0731brd

    private String status;
    private List<Favori> favoris = new ArrayList<>();
    private String nom;
    private String prenom;
    private String telephone;
    private LocalDateTime dateCreation = LocalDateTime.now();
    private LocalDateTime dateExpiration;
    private boolean isActive = true;
    private String googleId;
    private String provider = "LOCAL";

    // Constructeur pour génération automatique d'URL unique
    public Compte(String email, String password) {
        this.email = email;
        this.password = password;
        this.url = generateUniqueUrl();
        this.status = "NON_PAYANT";
        this.favoris = new ArrayList<>();
        this.provider = "LOCAL";
    }

    private String generateUniqueUrl() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    public void ajouterFavori(String idContenu, String nomContenu, String type) {
        Favori favori = new Favori(idContenu, nomContenu, type);
        if (!favoris.contains(favori)) {
            favoris.add(favori);
        }
    }

    public void retirerFavori(String idContenu) {
        favoris.removeIf(f -> f.getIdContenu().equals(idContenu));
    }

    public boolean isExpired() {
        if ("NON_PAYANT".equals(status)) {
            return false;
        }
        return dateExpiration != null && LocalDateTime.now().isAfter(dateExpiration);
    }

    public boolean isPayant() {
        return "PAYANT".equals(status);
    }

    public boolean isGoogleAccount() {
        return "GOOGLE".equals(provider);
    }

    // ✅ NOUVELLE MÉTHODE - Vérifier si l'utilisateur a configuré Xtream
    public boolean hasXtreamConfig() {
        return xtreamBaseUrl != null && !xtreamBaseUrl.isEmpty()
                && xtreamUsername != null && !xtreamUsername.isEmpty()
                && xtreamPassword != null && !xtreamPassword.isEmpty();
    }
}