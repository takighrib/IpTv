package com.example.demo.service;

import com.example.demo.config.UserXtreamConfig;
import com.example.demo.model.Compte;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Optional;
import com.example.demo.repository.CompteRepository;

/**
 * Service pour gérer le contexte utilisateur et sa configuration Xtream
 */
@Service
@RequiredArgsConstructor
public class UserContextService {

    private final CompteRepository compteRepository;

    /**
     * Récupère la configuration Xtream d'un utilisateur par son ID
     */
    public Optional<UserXtreamConfig> getUserXtreamConfig(String userId) {
        Optional<Compte> compteOpt = compteRepository.findById(userId);

        if (compteOpt.isEmpty()) {
            return Optional.empty();
        }

        Compte compte = compteOpt.get();

        if (!compte.hasXtreamConfig()) {
            return Optional.empty();
        }

        UserXtreamConfig config = UserXtreamConfig.builder()
                .baseUrl(compte.getXtreamBaseUrl())
                .username(compte.getXtreamUsername())
                .password(compte.getXtreamPassword())
                .build();

        return Optional.of(config);
    }

    /**
     * Récupère la configuration Xtream ou lance une exception
     */
    public UserXtreamConfig getUserXtreamConfigOrThrow(String userId) {
        return getUserXtreamConfig(userId)
                .orElseThrow(() -> new RuntimeException(
                        "Configuration Xtream non trouvée pour l'utilisateur " + userId +
                                ". Veuillez configurer vos credentials Xtream."
                ));
    }

    /**
     * Vérifie si un utilisateur a une configuration Xtream valide
     */
    public boolean hasValidXtreamConfig(String userId) {
        return getUserXtreamConfig(userId)
                .map(UserXtreamConfig::isValid)
                .orElse(false);
    }

    /**
     * Récupère un compte par son ID
     */
    public Optional<Compte> getCompteById(String userId) {
        return compteRepository.findById(userId);
    }
}