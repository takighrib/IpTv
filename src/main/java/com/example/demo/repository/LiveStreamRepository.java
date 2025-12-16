package com.example.demo.repository;


import com.example.demo.model.LiveStream;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface LiveStreamRepository extends MongoRepository<LiveStream, String> {

    // Recherche par streamId pour éviter les doublons
    Optional<LiveStream> findByStreamId(Integer streamId);

    // Vérifie si un stream existe déjà
    boolean existsByStreamId(Integer streamId);

    // Recherche par nom (pour la fonctionnalité de recherche)
    List<LiveStream> findByNameContainingIgnoreCase(String name);

    // Recherche par catégorie
    List<LiveStream> findByCategoryName(String categoryName);

    // Supprime par streamId
    void deleteByStreamId(Integer streamId);
}
