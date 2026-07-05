package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Request-Body für POST /api/orgstruktur/kontengruppen */
public class KontengruppeRequest {

    @NotBlank(message = "Gruppen-Nr ist Pflichtfeld")
    private String gruppenNr;

    @NotBlank(message = "Bezeichnung ist Pflichtfeld")
    private String bezeichnung;

    @NotNull(message = "Von-Nummer ist Pflichtfeld")
    private Integer vonNummer;

    @NotNull(message = "Bis-Nummer ist Pflichtfeld")
    private Integer bisNummer;

    public String getGruppenNr() { return gruppenNr; }
    public void setGruppenNr(String gruppenNr) { this.gruppenNr = gruppenNr; }
    public String getBezeichnung() { return bezeichnung; }
    public void setBezeichnung(String bezeichnung) { this.bezeichnung = bezeichnung; }
    public Integer getVonNummer() { return vonNummer; }
    public void setVonNummer(Integer vonNummer) { this.vonNummer = vonNummer; }
    public Integer getBisNummer() { return bisNummer; }
    public void setBisNummer(Integer bisNummer) { this.bisNummer = bisNummer; }
}