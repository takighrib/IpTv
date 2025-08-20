package com.example.demo.repository;

import com.example.demo.model.Series;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SeriesRepository extends MongoRepository<Series, String> {
}
