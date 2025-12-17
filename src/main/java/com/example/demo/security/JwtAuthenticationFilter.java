package com.example.demo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            // Récupérer le header Authorization
            String authHeader = request.getHeader("Authorization");

            log.debug("🔍 Request URL: {}", request.getRequestURI());
            log.debug("🔍 Authorization Header: {}", authHeader != null ? "Present" : "Missing");

            // Si pas de header Authorization, continuer sans authentification
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.debug("⚠️  No valid Authorization header found");
                filterChain.doFilter(request, response);
                return;
            }

            // Extraire le token (enlever "Bearer ")
            String token = authHeader.substring(7);
            log.debug("🔑 Token extracted (first 20 chars): {}...",
                    token.length() > 20 ? token.substring(0, 20) : token);

            // Valider le token
            if (!jwtUtil.validateToken(token)) {
                log.warn("❌ Token validation failed");
                filterChain.doFilter(request, response);
                return;
            }

            // Extraire l'userId du token
            String userId = jwtUtil.extractUserId(token);
            log.debug("✅ Token valid for userId: {}", userId);

            // Si l'utilisateur n'est pas déjà authentifié
            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Créer l'authentification
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                new ArrayList<>()
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // Définir l'authentification dans le contexte Spring Security
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("✅ User authenticated: {}", userId);
            }

        } catch (Exception e) {
            log.error("❌ Error in JWT filter: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}






