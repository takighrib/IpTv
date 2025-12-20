package com.example.demo.service;


import com.example.demo.model.Compte;
import com.example.demo.repository.CompteRepository;
import io.micrometer.common.util.StringUtils;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.demo.model.Playlist;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompteService {

    private final CompteRepository compteRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final EmailService emailService;

    /**
     * Étape 1 : Crée un compte non vérifié et envoie l'OTP
     */
    @Transactional
    public Compte creerCompteNonVerifie(String email, String password, String nom, String prenom) {
        // Vérifier si l'email existe déjà
        if (compteRepository.existsByEmail(email)) {
            throw new RuntimeException("Cet email est déjà utilisé");
        }

        // Créer le compte (non actif tant que l'email n'est pas vérifié)
        Compte compte = Compte.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .nom(nom)
                .prenom(prenom)
                .dateCreation(LocalDateTime.now())
                .isActive(false) // ✅ Désactivé jusqu'à vérification
                .isEmailVerified(false)
                .build();

        compte = compteRepository.save(compte);
        log.info("✅ Compte créé (non vérifié) pour: {}", email);

        // Envoyer l'OTP
        otpService.creerEtEnvoyerOTP(email);
        log.info("📧 OTP envoyé à: {}", email);

        return compte;
    }

    /**
     * Étape 2 : Vérifie l'OTP et active le compte
     */
    @Transactional
    public Compte verifierEmailEtActiverCompte(String email, String codeOTP) {
        // Vérifier l'OTP
        if (!otpService.verifierOTP(email, codeOTP)) {
            throw new RuntimeException("Code OTP invalide ou expiré");
        }

        // Récupérer le compte
        Compte compte = compteRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Compte introuvable"));

        // Activer le compte
        compte.setActive(true);
        compte.setEmailVerified(true);
        compte = compteRepository.save(compte);

        log.info("✅ Email vérifié et compte activé pour: {}", email);

        // Envoyer email de bienvenue
        emailService.envoyerEmailBienvenue(email, compte.getPrenom());

        return compte;
    }

    /**
     * Renvoie un OTP
     */
    public void renvoyerOTP(String email) {
        // Vérifier que le compte existe
        if (!compteRepository.existsByEmail(email)) {
            throw new RuntimeException("Aucun compte associé à cet email");
        }

        otpService.renvoyerOTP(email);
        log.info("🔄 OTP renvoyé pour: {}", email);
    }

    /**
     * Ajoute une playlist à un compte
     */
    @Transactional
    public Compte ajouterPlaylist(String compteId, String nom, String xtreamBaseUrl,
                                  String xtreamUsername, String xtreamPassword,
                                  LocalDateTime dateExpiration) {
        Compte compte = compteRepository.findById(compteId)
                .orElseThrow(() -> new RuntimeException("Compte introuvable"));

        // Créer la playlist
        Playlist playlist = Playlist.builder()
                .id(UUID.randomUUID().toString())
                .nom(nom)
                .xtreamUsername(xtreamUsername)
                .xtreamPassword(xtreamPassword)
                .dateExpiration(dateExpiration)
                .dateCreation(LocalDateTime.now())
                .isActive(true)
                .build();

        // Ajouter la playlist au compte
        compte.ajouterPlaylist(playlist);
        compte = compteRepository.save(compte);

        log.info("✅ Playlist '{}' ajoutée au compte {}", nom, compteId);
        return compte;
    }

    /**
     * Met à jour une playlist
     */
    @Transactional
    public Compte mettreAJourPlaylist(String compteId, String playlistId, String nom,
                                      String xtreamUsername,
                                      String xtreamPassword, LocalDateTime dateExpiration) {
        Compte compte = compteRepository.findById(compteId)
                .orElseThrow(() -> new RuntimeException("Compte introuvable"));

        Playlist playlist = compte.trouverPlaylistParId(playlistId);
        if (playlist == null) {
            throw new RuntimeException("Playlist introuvable");
        }

        // Mettre à jour les champs
        if (nom != null) playlist.setNom(nom);
        if (xtreamUsername != null) playlist.setXtreamUsername(xtreamUsername);
        if (xtreamPassword != null) playlist.setXtreamPassword(xtreamPassword);
        if (dateExpiration != null) playlist.setDateExpiration(dateExpiration);
        playlist.setDateModification(LocalDateTime.now());

        compte = compteRepository.save(compte);
        log.info("✅ Playlist '{}' mise à jour", playlistId);
        return compte;
    }

    /**
     * Supprime une playlist
     */
    @Transactional
    public Compte supprimerPlaylist(String compteId, String playlistId) {
        Compte compte = compteRepository.findById(compteId)
                .orElseThrow(() -> new RuntimeException("Compte introuvable"));

        compte.retirerPlaylist(playlistId);
        compte = compteRepository.save(compte);

        log.info("✅ Playlist '{}' supprimée", playlistId);
        return compte;
    }

    /**
     * Ajoute un favori à une playlist
     */
    @Transactional
    public Compte ajouterFavoriAPlaylist(String compteId, String playlistId,
                                         String idContenu, String nomContenu, String type) {
        Compte compte = compteRepository.findById(compteId)
                .orElseThrow(() -> new RuntimeException("Compte introuvable"));

        Playlist playlist = compte.trouverPlaylistParId(playlistId);
        if (playlist == null) {
            throw new RuntimeException("Playlist introuvable");
        }

        playlist.ajouterFavori(idContenu, nomContenu, type);
        compte = compteRepository.save(compte);

        log.info("✅ Favori ajouté à la playlist '{}'", playlistId);
        return compte;
    }

    /**
     * Retire un favori d'une playlist
     */
    @Transactional
    public Compte retirerFavoriDePlaylist(String compteId, String playlistId, String idContenu) {
        Compte compte = compteRepository.findById(compteId)
                .orElseThrow(() -> new RuntimeException("Compte introuvable"));

        Playlist playlist = compte.trouverPlaylistParId(playlistId);
        if (playlist == null) {
            throw new RuntimeException("Playlist introuvable");
        }

        playlist.retirerFavori(idContenu);
        compte = compteRepository.save(compte);

        log.info("✅ Favori retiré de la playlist '{}'", playlistId);
        return compte;
    }

    /**
     * Trouve un compte par email
     */
    public Optional<Compte> trouverParEmail(String email) {
        return compteRepository.findByEmail(email);
    }



    /**
     * Vérifie les credentials
     */
    public boolean verifierCredentials(String email, String password) {
        Optional<Compte> compte = compteRepository.findByEmail(email);
        return compte.isPresent() &&
                compte.get().getPassword() != null &&
                passwordEncoder.matches(password, compte.get().getPassword());
    }

    /**
     * Obtient tous les comptes
     */
    public List<Compte> obtenirTousLesComptes() {
        return compteRepository.findAll();
    }

    /**
     * Supprime un compte
     */
    @Transactional
    public void supprimerCompte(String compteId) {
        compteRepository.deleteById(compteId);
        log.info("✅ Compte '{}' supprimé", compteId);
    }

    /**
     * Vérifie les playlists expirées et les désactive
     */
    @Transactional
    public void verifierExpirations() {
        List<Compte> comptes = compteRepository.findAll();
        for (Compte compte : comptes) {
            boolean modified = false;
            for (Playlist playlist : compte.getPlaylists()) {
                if (playlist.isExpired() && playlist.isActive()) {
                    playlist.setActive(false);
                    modified = true;
                    log.info("⚠️ Playlist '{}' expirée et désactivée", playlist.getId());
                }
            }
            if (modified) {
                compteRepository.save(compte);
            }
        }
    }




    public boolean deleteAccount(String email) {
        if(!StringUtils.isEmpty(email) && compteRepository.existsByEmail(email)) {
            return compteRepository.deleteByEmail(email);
        }
        return false;
    }
}


