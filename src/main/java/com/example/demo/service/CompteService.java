package com.example.demo.service;


import com.example.demo.model.Compte;
import com.example.demo.repository.CompteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CompteService {

    @Autowired
    private CompteRepository compteRepository;
    private  PasswordEncoder passwordEncoder;

    // Créer un compte
    public Compte creerCompte(String email, String password, String nom, String prenom) {
        if (compteRepository.existsByEmail(email)) {
            throw new RuntimeException("Email déjà utilisé");
        }

        Compte compte = new Compte();
        compte.setEmail(email);
        compte.setPassword(passwordEncoder.encode(password));
        compte.setNom(nom);
        compte.setPrenom(prenom);
        compte.setUrl(generateUniqueUrl());
        compte.setStatus("NON_PAYANT");
        compte.setProvider("LOCAL");

        return compteRepository.save(compte);
    }

    // Créer un compte Google
    public Compte creerCompteGoogle(String email, String googleId, String nom) {
        Optional<Compte> existant = compteRepository.findByEmail(email);
        if (existant.isPresent()) {
            return existant.get();
        }

        Compte compte = new Compte();
        compte.setEmail(email);
        compte.setGoogleId(googleId);
        compte.setNom(nom);
        compte.setUrl(generateUniqueUrl());
        compte.setStatus("NON_PAYANT");
        compte.setProvider("GOOGLE");

        return compteRepository.save(compte);
    }

    // Trouver par email
    public Optional<Compte> trouverParEmail(String email) {
        return compteRepository.findByEmail(email);
    }

    // Trouver par URL unique
    public Optional<Compte> trouverParUrl(String url) {
        return compteRepository.findByUrl(url);
    }

    // Vérifier les credentials
    public boolean verifierCredentials(String email, String password) {
        Optional<Compte> compte = compteRepository.findByEmail(email);
        return compte.isPresent() &&
                compte.get().getPassword() != null &&
                passwordEncoder.matches(password, compte.get().getPassword());
    }

    // Ajouter aux favoris
    public Compte ajouterFavori(String compteId, String idContenu, String nomContenu, String type) {
        Compte compte = compteRepository.findById(compteId)
                .orElseThrow(() -> new RuntimeException("Compte introuvable"));

        // Valider le type
        if (!type.equals("CHAINE") && !type.equals("FILM") && !type.equals("SERIE")) {
            throw new RuntimeException("Type invalide. Utilisez: CHAINE, FILM, ou SERIE");
        }

        compte.ajouterFavori(idContenu, nomContenu, type);
        return compteRepository.save(compte);
    }

    // Retirer des favoris
    public Compte retirerFavori(String compteId, String idContenu) {
        Compte compte = compteRepository.findById(compteId)
                .orElseThrow(() -> new RuntimeException("Compte introuvable"));

        compte.retirerFavori(idContenu);
        return compteRepository.save(compte);
    }

    // Passer en compte payant
    public Compte passerEnPayant(String compteId, int dureeMois) {
        Compte compte = compteRepository.findById(compteId)
                .orElseThrow(() -> new RuntimeException("Compte introuvable"));

        compte.setStatus("PAYANT");
        compte.setDateExpiration(LocalDateTime.now().plusMonths(dureeMois));
        compte.setActive(true);

        return compteRepository.save(compte);
    }

    // Passer en compte non payant
    public Compte passerEnNonPayant(String compteId) {
        Compte compte = compteRepository.findById(compteId)
                .orElseThrow(() -> new RuntimeException("Compte introuvable"));

        compte.setStatus("NON_PAYANT");
        compte.setDateExpiration(null);

        return compteRepository.save(compte);
    }

    // Vérifier l'expiration et mettre à jour le status
    public void verifierExpirations() {
        List<Compte> comptes = compteRepository.findAll();
        for (Compte compte : comptes) {
            if (compte.isExpired()) {
                compte.setStatus("NON_PAYANT");
                compte.setActive(false);
                compteRepository.save(compte);
            }
        }
    }

    // Générer URL unique
    private String generateUniqueUrl() {
        String url;
        do {
            url = java.util.UUID.randomUUID().toString().substring(0, 8);
        } while (compteRepository.existsByUrl(url));
        return url;
    }

    // Obtenir tous les comptes
    public List<Compte> obtenirTousLesComptes() {
        return compteRepository.findAll();
    }

    // Supprimer un compte
    public void supprimerCompte(String compteId) {
        compteRepository.deleteById(compteId);
    }
}
