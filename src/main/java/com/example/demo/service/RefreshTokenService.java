package com.example.demo.service;


import com.example.demo.model.RefreshToken;
import com.example.demo.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Durée de validité du refresh token (en jours)
     * Par défaut : 7 jours
     */
    @Value("${jwt.refresh.expiration:7}")
    private int refreshExpirationDays;

    /**
     * Nombre maximum de refresh tokens actifs par utilisateur
     * Permet de limiter les sessions simultanées
     */
    @Value("${jwt.refresh.max-active:5}")
    private int maxActiveTokensPerUser;

    /**
     * Crée un nouveau refresh token pour un utilisateur
     *
     * @param userId ID de l'utilisateur
     * @param email Email de l'utilisateur
     * @param userAgent User-Agent du client (optionnel)
     * @param ipAddress IP du client (optionnel)
     * @return Le refresh token créé
     */
    @Transactional
    public RefreshToken creerRefreshToken(String userId, String email,
                                          String userAgent, String ipAddress) {
        try {
            // Nettoyer les anciens tokens si limite atteinte
            nettoyerTokensExcedentaires(userId);

            // Générer un token unique
            String tokenValue = genererTokenUnique();

            // Créer le refresh token
            RefreshToken refreshToken = RefreshToken.builder()
                    .token(tokenValue)
                    .userId(userId)
                    .email(email)
                    .dateCreation(LocalDateTime.now())
                    .dateExpiration(LocalDateTime.now().plusDays(refreshExpirationDays))
                    .isRevoked(false)
                    .derniereUtilisation(LocalDateTime.now())
                    .userAgent(userAgent)
                    .ipAddress(ipAddress)
                    .build();

            refreshToken = refreshTokenRepository.save(refreshToken);

            log.info("✅ Refresh token créé pour userId: {} (expire le: {})",
                    userId, refreshToken.getDateExpiration());

            return refreshToken;

        } catch (Exception e) {
            log.error("❌ Erreur création refresh token pour userId {}: {}", userId, e.getMessage());
            throw new RuntimeException("Erreur lors de la création du refresh token", e);
        }
    }

    /**
     * Valide un refresh token et retourne l'entité si valide
     *
     * @param tokenValue La valeur du token
     * @return Le refresh token si valide
     * @throws RuntimeException si le token est invalide
     */
    @Transactional
    public RefreshToken validerRefreshToken(String tokenValue) {
        try {
            Optional<RefreshToken> tokenOpt = refreshTokenRepository
                    .findValidToken(tokenValue, LocalDateTime.now());

            if (tokenOpt.isEmpty()) {
                log.warn("⚠️ Refresh token invalide ou expiré: {}", tokenValue);
                throw new RuntimeException("Refresh token invalide ou expiré");
            }

            RefreshToken refreshToken = tokenOpt.get();

            // Mettre à jour la dernière utilisation
            refreshToken.mettreAJourUtilisation();
            refreshTokenRepository.save(refreshToken);

            log.info("✅ Refresh token validé pour userId: {}", refreshToken.getUserId());

            return refreshToken;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ Erreur validation refresh token: {}", e.getMessage());
            throw new RuntimeException("Erreur lors de la validation du refresh token", e);
        }
    }

    /**
     * Révoque un refresh token spécifique (logout sur un appareil)
     *
     * @param tokenValue La valeur du token à révoquer
     */
    @Transactional
    public void revoquerToken(String tokenValue) {
        try {
            Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByToken(tokenValue);

            if (tokenOpt.isPresent()) {
                RefreshToken token = tokenOpt.get();
                token.revoquer();
                refreshTokenRepository.save(token);

                log.info("✅ Refresh token révoqué pour userId: {}", token.getUserId());
            } else {
                log.warn("⚠️ Tentative de révocation d'un token inexistant");
            }

        } catch (Exception e) {
            log.error("❌ Erreur révocation refresh token: {}", e.getMessage());
            throw new RuntimeException("Erreur lors de la révocation du token", e);
        }
    }

    /**
     * Révoque tous les tokens d'un utilisateur (logout global)
     *
     * @param userId ID de l'utilisateur
     */
    @Transactional
    public void revoquerTousLesTokens(String userId) {
        try {
            List<RefreshToken> tokens = refreshTokenRepository.findByUserId(userId);

            for (RefreshToken token : tokens) {
                if (!token.isRevoked()) {
                    token.revoquer();
                    refreshTokenRepository.save(token);
                }
            }

            log.info("✅ Tous les refresh tokens révoqués pour userId: {} ({} tokens)",
                    userId, tokens.size());

        } catch (Exception e) {
            log.error("❌ Erreur révocation de tous les tokens pour userId {}: {}",
                    userId, e.getMessage());
            throw new RuntimeException("Erreur lors de la révocation des tokens", e);
        }
    }

    /**
     * Supprime un refresh token de la base
     *
     * @param tokenValue La valeur du token à supprimer
     */
    @Transactional
    public void supprimerToken(String tokenValue) {
        try {
            Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByToken(tokenValue);
            tokenOpt.ifPresent(refreshTokenRepository::delete);

            log.info("✅ Refresh token supprimé");

        } catch (Exception e) {
            log.error("❌ Erreur suppression refresh token: {}", e.getMessage());
        }
    }

    /**
     * Nettoie les tokens expirés (à exécuter périodiquement)
     */
    @Transactional
    public void nettoyerTokensExpires() {
        try {
            LocalDateTime now = LocalDateTime.now();
            refreshTokenRepository.deleteByDateExpirationBefore(now);

            log.info("🗑️ Nettoyage des refresh tokens expirés effectué");

        } catch (Exception e) {
            log.error("❌ Erreur nettoyage tokens expirés: {}", e.getMessage());
        }
    }

    /**
     * Nettoie les anciens tokens révoqués (plus de 30 jours)
     */
    @Transactional
    public void nettoyerAnciennesRevocations() {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
            refreshTokenRepository.deleteOldRevokedTokens(cutoffDate);

            log.info("🗑️ Nettoyage des anciennes révocations effectué");

        } catch (Exception e) {
            log.error("❌ Erreur nettoyage anciennes révocations: {}", e.getMessage());
        }
    }

    /**
     * Obtient tous les tokens actifs d'un utilisateur
     */
    public List<RefreshToken> getTokensActifs(String userId) {
        return refreshTokenRepository.findActiveTokensByUserId(userId, LocalDateTime.now());
    }

    /**
     * Compte les tokens actifs d'un utilisateur
     */
    public long compterTokensActifs(String userId) {
        return refreshTokenRepository.countActiveTokensByUserId(userId, LocalDateTime.now());
    }

    /**
     * Nettoie les tokens excédentaires pour respecter la limite
     */
    private void nettoyerTokensExcedentaires(String userId) {
        List<RefreshToken> activeTokens = getTokensActifs(userId);

        if (activeTokens.size() >= maxActiveTokensPerUser) {
            // Trier par date de création (du plus ancien au plus récent)
            activeTokens.sort((t1, t2) -> t1.getDateCreation().compareTo(t2.getDateCreation()));

            // Supprimer les plus anciens
            int tokensASupprimer = activeTokens.size() - maxActiveTokensPerUser + 1;
            for (int i = 0; i < tokensASupprimer; i++) {
                refreshTokenRepository.delete(activeTokens.get(i));
                log.info("🗑️ Ancien refresh token supprimé pour userId: {}", userId);
            }
        }
    }

    /**
     * Génère un token unique (UUID)
     */
    private String genererTokenUnique() {
        return UUID.randomUUID().toString();
    }

    /**
     * Obtient le temps restant avant expiration (en jours)
     */
    public long getJoursRestants(String tokenValue) {
        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByToken(tokenValue);

        if (tokenOpt.isEmpty() || !tokenOpt.get().isValid()) {
            return 0;
        }

        RefreshToken token = tokenOpt.get();
        LocalDateTime now = LocalDateTime.now();

        if (now.isAfter(token.getDateExpiration())) {
            return 0;
        }

        return java.time.Duration.between(now, token.getDateExpiration()).toDays();
    }
}


