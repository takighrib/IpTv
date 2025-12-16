package com.example.demo.controller;


import com.example.demo.model.Compte;
import com.example.demo.service.CompteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comptes")
public class CompteController {

    @Autowired
    private CompteService compteService;

    // Obtenir tous les comptes
    @GetMapping
    public ResponseEntity<List<Compte>> obtenirTousLesComptes() {
        return ResponseEntity.ok(compteService.obtenirTousLesComptes());
    }

    // Obtenir un compte par ID
    @GetMapping("/{id}")
    public ResponseEntity<Compte> obtenirCompte(@PathVariable String id) {
        return ResponseEntity.ok(compteService.trouverParEmail(id).orElse(null));
    }

    // Obtenir compte par URL unique
    @GetMapping("/url/{url}")
    public ResponseEntity<Compte> obtenirCompteParUrl(@PathVariable String url) {
        return ResponseEntity.ok(compteService.trouverParUrl(url).orElse(null));
    }

    // Ajouter aux favoris
    @PostMapping("/{compteId}/favoris")
    public ResponseEntity<Compte> ajouterFavori(
            @PathVariable String compteId,
            @RequestBody Map<String, String> payload) {
        String idContenu = payload.get("idContenu");
        String nomContenu = payload.get("nomContenu");
        String type = payload.get("type"); // "CHAINE", "FILM", ou "SERIE"
        Compte compte = compteService.ajouterFavori(compteId, idContenu, nomContenu, type);
        return ResponseEntity.ok(compte);
    }

    // Retirer des favoris
    @DeleteMapping("/{compteId}/favoris/{idContenu}")
    public ResponseEntity<Compte> retirerFavori(
            @PathVariable String compteId,
            @PathVariable String idContenu) {

        Compte compte = compteService.retirerFavori(compteId, idContenu);
        return ResponseEntity.ok(compte);
    }

    // Passer en compte payant
    @PutMapping("/{compteId}/payer")
    public ResponseEntity<Compte> passerEnPayant(
            @PathVariable String compteId,
            @RequestParam int dureeMois) {

        Compte compte = compteService.passerEnPayant(compteId, dureeMois);
        return ResponseEntity.ok(compte);
    }

    // Passer en compte non payant
    @PutMapping("/{compteId}/non-payant")
    public ResponseEntity<Compte> passerEnNonPayant(@PathVariable String compteId) {
        Compte compte = compteService.passerEnNonPayant(compteId);
        return ResponseEntity.ok(compte);
    }

    // Supprimer compte
    @DeleteMapping("/{compteId}")
    public ResponseEntity<?> supprimerCompte(@PathVariable String compteId) {
        compteService.supprimerCompte(compteId);
        return ResponseEntity.ok().build();
    }
}