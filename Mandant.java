package com.example.demo.entity;


import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

/**
 * Mandant – Größte organisatorische Einheit im SAP-System.
 * Entspricht dem Gesamtkonzern (z.B. Global Bike GB00).
 */
@Entity
@Table(name = "MANDANT")
public class Mandant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mandant_id")
    private Long mandantId;

    @NotBlank
    @Column(name = "mandant_nr", unique = true, nullable = false, length = 10)
    private String mandantNr;          // z.B. "GB00"

    @NotBlank
    @Column(name = "bezeichnung", nullable = false, length = 100)
    private String bezeichnung;        // z.B. "Global Bike"

    @Column(name = "waehrung", length = 5)
    private String waehrung = "EUR";   // Hauswährung

    @JsonIgnoreProperties("mandant")
    @OneToMany(mappedBy = "mandant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Buchungskreis> buchungskreise = new ArrayList<>();
    
    // ─── Constructors ─────────────────────────────────────────────
    public Mandant() {}

    public Mandant(String mandantNr, String bezeichnung, String waehrung) {
        this.mandantNr = mandantNr;
        this.bezeichnung = bezeichnung;
        this.waehrung = waehrung;
    }

    // ─── Getters / Setters ────────────────────────────────────────
    public Long getMandantId()                     { return mandantId; }
    public void setMandantId(Long mandantId)       { this.mandantId = mandantId; }
    public String getMandantNr()                   { return mandantNr; }
    public void setMandantNr(String mandantNr)     { this.mandantNr = mandantNr; }
    public String getBezeichnung()                 { return bezeichnung; }
    public void setBezeichnung(String bezeichnung) { this.bezeichnung = bezeichnung; }
    public String getWaehrung()                    { return waehrung; }
    public void setWaehrung(String waehrung)       { this.waehrung = waehrung; }
    public List<Buchungskreis> getBuchungskreise() { return buchungskreise; }
    public void setBuchungskreise(List<Buchungskreis> buchungskreise) { this.buchungskreise = buchungskreise; }

    @Override
    public String toString() {
        return "Mandant{" + mandantNr + " – " + bezeichnung + "}";
    }
}
