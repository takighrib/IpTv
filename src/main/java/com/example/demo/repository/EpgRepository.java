package com.example.demo.repository;

import com.example.demo.model.Epg;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EpgRepository extends MongoRepository<Epg, String> {

    // Pour éviter les doublons EPG
    Optional<Epg> findByStreamIdAndStartAndTitle(Integer streamId, String start, String title);

    // Récupère tous les EPG d'un stream
    List<Epg> findByStreamId(Integer streamId);

    // Supprime les anciens EPG d'un stream avant mise à jour
    void deleteByStreamId(Integer streamId);
}