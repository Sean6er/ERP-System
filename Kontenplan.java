package com.example.demo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

/** Kontenplan – Vorlage des Kontenrahmens, der Buchungskreisen zugeordnet wird (z.B. GL00). */
@Entity
@Table(name = "KONTENPLAN")
public class Kontenplan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "kontenplan_id")
    private Long kontenplanId;

    @NotBlank
    @Column(name = "kontenplan_nr", unique = true, nullable = false, length = 10)
    private String kontenplanNr;    // z.B. "GL00"

    @NotBlank
    @Column(name = "bezeichnung", nullable = false, length = 100)
    private String bezeichnung;

    public Kontenplan() {}
    public Kontenplan(String kontenplanNr, String bezeichnung) {
        this.kontenplanNr = kontenplanNr; this.bezeichnung = bezeichnung;
    }

    public Long getKontenplanId() { return kontenplanId; }
    public void setKontenplanId(Long id) { this.kontenplanId = id; }
    public String getKontenplanNr() { return kontenplanNr; }
    public void setKontenplanNr(String kontenplanNr) { this.kontenplanNr = kontenplanNr; }
    public String getBezeichnung() { return bezeichnung; }
    public void setBezeichnung(String bezeichnung) { this.bezeichnung = bezeichnung; }

    @Override
    public String toString() { return "Kontenplan{" + kontenplanNr + " – " + bezeichnung + "}"; }
}