package com.example.demo.repository;

import com.example.demo.model.Vod;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VodRepository extends MongoRepository<Vod, String> {

    Optional<Vod> findByVodId(Integer vodId);

    boolean existsByVodId(Integer vodId);

    List<Vod> findByNameContainingIgnoreCase(String name);

    List<Vod> findByCategoryName(String categoryName);
}
