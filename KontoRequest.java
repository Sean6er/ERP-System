package com.example.demo.dto;

import com.example.demo.entity.Konto.Kontotyp;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Request-Body für POST /api/stammdaten/konten */
public class KontoRequest {

    @NotNull(message = "Kontonummer ist Pflichtfeld")
    private Integer kontonummer;

    @NotBlank(message = "Kontobezeichnung ist Pflichtfeld")
    private String kontobezeichnung;

    @NotNull(message = "Kontotyp ist Pflichtfeld")
    private Kontotyp kontotyp;

    /** Kontenplan-Nr, z.B. "GL00". Default GL00, muss vorher unter Organisationsstruktur angelegt sein. */
    private String kontenplanNr = "GL00";

    /**
     * ÄNDERUNG (Kontengruppen-Feature): optionale Kontengruppen-Nr, z.B. "GL03"
     * (Betriebliche Aufwendungen). Wird sie nicht angegeben, ermittelt der
     * Server die passende Kontengruppe automatisch über den Nummernkreis der
     * Kontonummer. Wird sie angegeben, validiert der Server, dass die
     * Kontonummer tatsächlich in deren Nummernkreis fällt.
     */
    private String kontengruppeNr;

    public Integer getKontonummer() { return kontonummer; }
    public void setKontonummer(Integer kontonummer) { this.kontonummer = kontonummer; }
    public String getKontobezeichnung() { return kontobezeichnung; }
    public void setKontobezeichnung(String kontobezeichnung) { this.kontobezeichnung = kontobezeichnung; }
    public Kontotyp getKontotyp() { return kontotyp; }
    public void setKontotyp(Kontotyp kontotyp) { this.kontotyp = kontotyp; }
    public String getKontenplanNr() { return kontenplanNr; }
    public void setKontenplanNr(String kontenplanNr) { this.kontenplanNr = kontenplanNr; }
    public String getKontengruppeNr() { return kontengruppeNr; }
    public void setKontengruppeNr(String kontengruppeNr) { this.kontengruppeNr = kontengruppeNr; }
}