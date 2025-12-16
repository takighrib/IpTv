package com.example.demo.repository;

import com.example.demo.model.Series;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeriesRepository extends MongoRepository<Series, String> {

    Optional<Series> findBySeriesId(Integer seriesId);

    boolean existsBySeriesId(Integer seriesId);

    List<Series> findByNameContainingIgnoreCase(String name);

    List<Series> findByCategoryName(String categoryName);
}