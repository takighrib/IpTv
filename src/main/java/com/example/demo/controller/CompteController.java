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

    


    // Supprimer compte
    @DeleteMapping("/{compteId}")
    public ResponseEntity<?> supprimerCompte(@PathVariable String compteId) {
        compteService.supprimerCompte(compteId);
        return ResponseEntity.ok().build();
    }
}