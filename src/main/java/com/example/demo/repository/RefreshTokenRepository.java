package com.example.demo.repository;


import com.example.demo.model.RefreshToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour gérer les Refresh Tokens
 */
@Repository
public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {

    /**
     * Trouve un refresh token par sa valeur
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Trouve tous les tokens d'un utilisateur
     */
    List<RefreshToken> findByUserId(String userId);

    /**
     * Trouve tous les tokens actifs d'un utilisateur
     */
    @Query("{ 'userId': ?0, 'isRevoked': false, 'dateExpiration': { $gt: ?1 } }")
    List<RefreshToken> findActiveTokensByUserId(String userId, LocalDateTime now);

    /**
     * Vérifie si un token existe et est valide
     */
    @Query("{ 'token': ?0, 'isRevoked': false, 'dateExpiration': { $gt: ?1 } }")
    Optional<RefreshToken> findValidToken(String token, LocalDateTime now);

    /**
     * Supprime tous les tokens d'un utilisateur
     */
    void deleteByUserId(String userId);

    /**
     * Supprime les tokens expirés (nettoyage)
     */
    void deleteByDateExpirationBefore(LocalDateTime date);

    /**
     * Supprime les tokens révoqués anciens (nettoyage)
     */
    @Query(value = "{ 'isRevoked': true, 'dateCreation': { $lt: ?0 } }", delete = true)
    void deleteOldRevokedTokens(LocalDateTime date);

    /**
     * Compte les tokens actifs d'un utilisateur
     */
    @Query(value = "{ 'userId': ?0, 'isRevoked': false, 'dateExpiration': { $gt: ?1 } }", count = true)
    long countActiveTokensByUserId(String userId, LocalDateTime now);

    /**
     * Révoque tous les tokens d'un utilisateur (pour logout global)
     */
    @Query("{ 'userId': ?0 }")
    List<RefreshToken> findAllByUserId(String userId);
}



