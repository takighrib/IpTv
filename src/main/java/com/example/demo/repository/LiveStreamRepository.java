package com.example.demo.repository;


import com.example.demo.model.LiveStream;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LiveStreamRepository extends MongoRepository<LiveStream, String> {
}
