package com.example.demo.service;

import com.example.demo.config.UserXtreamConfig;
import com.example.demo.model.Compte;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Optional;

import com.example.demo.repository.CompteRepository;


import java.util.Optional;

/**
 * Service pour gérer le contexte utilisateur et sa configuration Xtream
 */
@Service
@RequiredArgsConstructor
public class UserContextService {

    private final CompteRepository compteRepository;

    /**
     * Récupère un compte par son ID
     */
    public Optional<Compte> getCompteById(String userId) {
        return compteRepository.findById(userId);
    }
}