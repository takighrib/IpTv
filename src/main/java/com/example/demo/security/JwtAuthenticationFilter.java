package com.example.demo.security;


import com.example.demo.service.CompteService;
import com.example.demo.model.Compte;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

import org.springframework.context.annotation.Lazy;


/**
 * Filtre pour intercepter et valider les tokens JWT dans les requêtes
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CompteService compteService;

    // ✅ SOLUTION : Injection Lazy pour casser la dépendance circulaire
    public JwtAuthenticationFilter(JwtUtil jwtUtil, @Lazy CompteService compteService) {
        this.jwtUtil = jwtUtil;
        this.compteService = compteService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            // Récupère le header Authorization
            final String authHeader = request.getHeader("Authorization");

            // Si pas de header ou ne commence pas par "Bearer ", continuer sans authentification
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            // Extrait le token (retire "Bearer ")
            final String jwt = authHeader.substring(7);

            // Valide le token
            if (!jwtUtil.validateToken(jwt)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Token invalide ou expiré\"}");
                return;
            }

            // Extrait l'email du token
            final String email = jwtUtil.extractEmail(jwt);

            // Si l'email est présent et qu'il n'y a pas déjà d'authentification
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Récupère le compte utilisateur
                Optional<Compte> compteOpt = compteService.trouverParEmail(email);

                if (compteOpt.isPresent()) {
                    Compte compte = compteOpt.get();

                    // Vérifie si le compte est actif
                    if (!compte.isActive()) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\": \"Compte désactivé\"}");
                        return;
                    }

                    // Vérifie si le compte est expiré (pour les comptes payants)
                    if (compte.isExpired()) {
                        response.setStatus(HttpServletResponse.SC_PAYMENT_REQUIRED);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\": \"Abonnement expiré\"}");
                        return;
                    }

                    // Crée un token d'authentification Spring Security
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            new ArrayList<>() // Pas de rôles pour l'instant
                    );

                    // Ajoute les détails de la requête
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Définit l'authentification dans le contexte de sécurité
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }

        } catch (Exception e) {
            logger.error("Erreur lors de l'authentification JWT: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Erreur d'authentification\"}");
            return;
        }

        // Continue la chaîne de filtres
        filterChain.doFilter(request, response);
    }

    /**
     * Détermine si le filtre doit être appliqué à cette requête
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        // Ne pas appliquer le filtre aux endpoints publics
        return path.startsWith("/api/auth/") ||
                path.startsWith("/actuator/") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/swagger-ui");
    }
}