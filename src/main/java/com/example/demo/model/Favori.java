package com.example.demo.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Favori {

    private String idContenu; // ID de la chaîne, film, ou série
    private String nomContenu; // Nom du contenu
    private String type; // "CHAINE", "FILM", "SERIE"
    private LocalDateTime dateAjout = LocalDateTime.now();

    public Favori(String idContenu, String nomContenu, String type) {
        this.idContenu = idContenu;
        this.nomContenu = nomContenu;
        this.type = type;
        this.dateAjout = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Favori favori = (Favori) o;
        return idContenu.equals(favori.idContenu);
    }

    @Override
    public int hashCode() {
        return idContenu.hashCode();
    }
}