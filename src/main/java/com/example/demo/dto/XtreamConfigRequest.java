package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class XtreamConfigRequest {

    @NotBlank(message = "L'URL Xtream est obligatoire")
    @Pattern(regexp = "^https?://.*", message = "L'URL doit commencer par http:// ou https://")
    private String xtreamBaseUrl;

    @NotBlank(message = "Le nom d'utilisateur Xtream est obligatoire")
    private String xtreamUsername;

    @NotBlank(message = "Le mot de passe Xtream est obligatoire")
    private String xtreamPassword;
}