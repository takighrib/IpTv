package com.example.demo.repository;


import com.example.demo.model.Otp;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OtpRepository extends MongoRepository<Otp, String> {

    /**
     * Trouve l'OTP le plus récent pour un email donné
     */
    Optional<Otp> findFirstByEmailOrderByDateCreationDesc(String email);

    /**
     * Trouve tous les OTP d'un email
     */
    List<Otp> findByEmail(String email);

    /**
     * Trouve un OTP valide (non utilisé et non expiré) pour un email
     */
    Optional<Otp> findByEmailAndIsUsedFalseAndDateExpirationAfter(String email, LocalDateTime now);

    /**
     * Supprime les OTP expirés (nettoyage)
     */
    void deleteByDateExpirationBefore(LocalDateTime date);


    void deleteByEmail(String email);


    Optional<Otp> findByEmailAndType(String email, String type);

    /**
     * Vérifie si un OTP existe pour un email
     */
    boolean existsByEmail(String email);

    /**
     * Compte les OTP non utilisés pour un email
     */
    long countByEmailAndIsUsedFalse(String email);
}
