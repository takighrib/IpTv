package com.example.demo.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO pour les réponses d'authentification
 */


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private boolean success;
    private String message;
    private String token;

    // Informations utilisateur
    private String userId;
    private String email;
    private String nom;
    private String prenom;
    private String url;
    private String telephone;

    // Informations abonnement
    private String status;
    private boolean isPayant;
    private LocalDateTime dateExpiration;

    // Informations provider
    private String provider;
    private boolean isNewUser;

    // ✅ NOUVEAU - Indique si l'utilisateur a configuré Xtream
    private boolean hasXtreamConfig;
}

