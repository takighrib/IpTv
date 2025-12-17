package com.example.demo.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddPlaylistRequest {

    @NotBlank(message = "Le nom de la playlist est obligatoire")
    @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
    private String nom;

    @NotBlank(message = "L'URL Xtream est obligatoire")
    @Pattern(regexp = "^https?://.*", message = "L'URL doit commencer par http:// ou https://")
    private String xtreamBaseUrl;

    @NotBlank(message = "Le nom d'utilisateur Xtream est obligatoire")
    private String xtreamUsername;

    @NotBlank(message = "Le mot de passe Xtream est obligatoire")
    private String xtreamPassword;

    // Optionnel - date d'expiration de la playlist
    private LocalDateTime dateExpiration;
}


