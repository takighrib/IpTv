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

    private String token;            // Access Token (JWT)
    private String refreshToken;
    private Long accessTokenExpiresIn;  // Durée de validité de l'access token (en secondes)
}