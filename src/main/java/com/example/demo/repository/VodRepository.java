package com.example.demo.repository;

import com.example.demo.model.Vod;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VodRepository extends MongoRepository<Vod, String> {
}
