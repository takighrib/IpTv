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

    private boolean success;
    private String message;
    private String token;

    // Informations utilisateur
    private String userId;
    private String email;
    private String nom;
    private String prenom;
    private String url;

    // ✅ NOUVEAUX CHAMPS
    private boolean isEmailVerified; // Indique si l'email est vérifié
    private boolean hasPlaylists;    // Indique si l'utilisateur a des playlists
    private int nombrePlaylists;     // Nombre de playlists
}