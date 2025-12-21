package com.example.demo.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String message;
    private String token;            // Access Token (JWT)
    private String refreshToken;
    // Informations utilisateur
    private String userId;
    private String email;
    private String nom;
    private String prenom;

    // ✅ NOUVEAUX CHAMPS
    private boolean isEmailVerified; // Indique si l'email est vérifié
    private boolean hasPlaylists;    // Indique si l'utilisateur a des playlists
    private int nombrePlaylists;     // Nombre de playlists
    private Long accessTokenExpiresIn;  // Durée de validité de l'access token (en secondes)
    private Long refreshTokenExpiresIn; // Durée de validité du refresh token (en secondes)
}