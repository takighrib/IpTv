package com.example.demo.repository;

import com.example.demo.model.Compte;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompteRepository extends MongoRepository<Compte, String> {

    // Recherche par email
    Optional<Compte> findByEmail(String email);

    // Recherche par URL unique
    Optional<Compte> findByUrl(String url);

    // Recherche par Google ID
    Optional<Compte> findByGoogleId(String googleId);

    // Vérifier si un email existe
    boolean existsByEmail(String email);

    // Vérifier si une URL existe
    boolean existsByUrl(String url);

    // Trouver tous les comptes payants
    List<Compte> findByStatus(String status);

    // Trouver les comptes actifs
    List<Compte> findByIsActive(boolean isActive);

    // Trouver les comptes par provider (LOCAL ou GOOGLE)
    List<Compte> findByProvider(String provider);

    // Trouver les comptes expirés
    @Query("{ 'status': 'PAYANT', 'dateExpiration': { $lt: ?0 } }")
    List<Compte> findExpiredAccounts(LocalDateTime currentDate);

    // Trouver les comptes qui expirent bientôt (dans les X jours)
    @Query("{ 'status': 'PAYANT', 'dateExpiration': { $gte: ?0, $lte: ?1 } }")
    List<Compte> findAccountsExpiringSoon(LocalDateTime startDate, LocalDateTime endDate);

    // Recherche par nom ou prénom (insensible à la casse)
    @Query("{ $or: [ { 'nom': { $regex: ?0, $options: 'i' } }, { 'prenom': { $regex: ?0, $options: 'i' } } ] }")
    List<Compte> searchByName(String searchTerm);

    // Compter les comptes par status
    long countByStatus(String status);

    // Compter les comptes actifs
    long countByIsActive(boolean isActive);

    // Trouver les comptes créés après une date
    List<Compte> findByDateCreationAfter(LocalDateTime date);

    // Trouver les comptes par téléphone
    Optional<Compte> findByTelephone(String telephone);
}