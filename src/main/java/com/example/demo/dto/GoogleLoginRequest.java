package com.example.demo.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;


/**
 * DTO pour la requête de login Google OAuth2
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoogleLoginRequest {

    @NotBlank(message = "L'email Google est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;

    @NotBlank(message = "L'ID Google est obligatoire")
    private String googleId;

    private String nom;
    private String prenom;
    private String photoUrl;
}


