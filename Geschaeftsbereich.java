package com.example.demo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

/** Geschäftsbereich – Sparte/Segment für interne Berichterstattung (z.B. "Bike", "Zubehör"). */
@Entity
@Table(name = "GESCHAEFTSBEREICH")
public class Geschaeftsbereich {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "geschaeftsbereich_id")
    private Long geschaeftsbereichId;

    @NotBlank
    @Column(name = "bereich_nr", unique = true, nullable = false, length = 10)
    private String bereichNr;       // z.B. "BIKE", "ZUB"

    @NotBlank
    @Column(name = "bezeichnung", nullable = false, length = 100)
    private String bezeichnung;

    public Geschaeftsbereich() {}
    public Geschaeftsbereich(String bereichNr, String bezeichnung) {
        this.bereichNr = bereichNr; this.bezeichnung = bezeichnung;
    }

    public Long getGeschaeftsbereichId() { return geschaeftsbereichId; }
    public void setGeschaeftsbereichId(Long id) { this.geschaeftsbereichId = id; }
    public String getBereichNr() { return bereichNr; }
    public void setBereichNr(String bereichNr) { this.bereichNr = bereichNr; }
    public String getBezeichnung() { return bezeichnung; }
    public void setBezeichnung(String bezeichnung) { this.bezeichnung = bezeichnung; }

    @Override
    public String toString() { return "Geschaeftsbereich{" + bereichNr + " – " + bezeichnung + "}"; }
}