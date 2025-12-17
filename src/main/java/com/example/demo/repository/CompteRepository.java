package com.example.demo.repository;

import com.example.demo.model.Compte;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Compte;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.example.demo.model.Compte;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompteRepository extends MongoRepository<Compte, String> {

    // ========== RECHERCHES BASIQUES ==========

    /**
     * Recherche par email
     */
    Optional<Compte> findByEmail(String email);

    /**
     * Recherche par URL unique
     */
    Optional<Compte> findByUrl(String url);

    /**
     * Vérifier si un email existe
     */
    boolean existsByEmail(String email);

    /**
     * Vérifier si une URL existe
     */
    boolean existsByUrl(String url);

    // ========== RECHERCHES PAR STATUT ==========

    /**
     * Trouver les comptes actifs
     */
    List<Compte> findByIsActive(boolean isActive);

    /**
     * Trouver les comptes avec email vérifié
     */
    List<Compte> findByIsEmailVerified(boolean isEmailVerified);

    /**
     * Compter les comptes actifs
     */
    long countByIsActive(boolean isActive);

    /**
     * Compter les comptes avec email vérifié
     */
    long countByIsEmailVerified(boolean isEmailVerified);

    // ========== RECHERCHES PAR DATE ==========

    /**
     * Trouver les comptes créés après une date
     */
    List<Compte> findByDateCreationAfter(LocalDateTime date);

    /**
     * Trouver les comptes créés entre deux dates
     */
    @Query("{ 'dateCreation': { $gte: ?0, $lte: ?1 } }")
    List<Compte> findByDateCreationBetween(LocalDateTime startDate, LocalDateTime endDate);

    // ========== RECHERCHES PAR NOM ==========

    /**
     * Recherche par nom ou prénom (insensible à la casse)
     */
    @Query("{ $or: [ { 'nom': { $regex: ?0, $options: 'i' } }, { 'prenom': { $regex: ?0, $options: 'i' } } ] }")
    List<Compte> searchByName(String searchTerm);

    /**
     * Recherche par nom exact
     */
    List<Compte> findByNom(String nom);

    /**
     * Recherche par prénom exact
     */
    List<Compte> findByPrenom(String prenom);

    // ========== RECHERCHES PAR PLAYLISTS ==========

    /**
     * Trouve les comptes qui ont au moins une playlist
     * Alternative sans $expr - vérifie que le tableau existe et n'est pas vide
     */
    @Query("{ 'playlists.0': { $exists: true } }")
    List<Compte> findAccountsWithPlaylists();

    /**
     * Trouve les comptes sans playlists
     * Alternative sans $expr
     */
    @Query("{ $or: [ { 'playlists': { $exists: false } }, { 'playlists': [] } ] }")
    List<Compte> findAccountsWithoutPlaylists();

    // Note: Pour les méthodes de comptage de playlists, on les implémente dans le service
    // car MongoDB query sans $expr ne peut pas facilement compter les éléments d'un tableau

    // ========== STATISTIQUES ==========

    /**
     * Compte tous les comptes
     */
    @Override
    long count();

    /**
     * Compte les comptes créés aujourd'hui
     */
    @Query(value = "{ 'dateCreation': { $gte: ?0 } }", count = true)
    long countCreatedToday(LocalDateTime startOfDay);

    /**
     * Compte les comptes actifs avec email vérifié
     */
    long countByIsActiveTrueAndIsEmailVerifiedTrue();

    // ========== NETTOYAGE ==========

    /**
     * Supprime les comptes inactifs créés avant une certaine date
     */
    @Query(value = "{ 'isActive': false, 'dateCreation': { $lt: ?0 } }", delete = true)
    void deleteInactiveAccountsCreatedBefore(LocalDateTime date);

    /**
     * Supprime les comptes avec email non vérifié créés il y a plus de X jours
     */
    @Query(value = "{ 'isEmailVerified': false, 'dateCreation': { $lt: ?0 } }", delete = true)
    void deleteUnverifiedAccountsCreatedBefore(LocalDateTime date);
}






