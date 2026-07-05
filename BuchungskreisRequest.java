package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Request-Body für POST /api/orgstruktur/buchungskreise */
public class BuchungskreisRequest {

    @NotBlank(message = "Buchungskreis-Nr ist Pflichtfeld")
    private String buchungskreisNr;

    @NotBlank(message = "Bezeichnung ist Pflichtfeld")
    private String bezeichnung;

    private String land;

    private String waehrung = "EUR";

    @NotNull(message = "Mandant ist Pflichtfeld")
    private Long mandantId;

    private Long kontenplanId;
    private Long kkbId;

    public String getBuchungskreisNr() { return buchungskreisNr; }
    public void setBuchungskreisNr(String buchungskreisNr) { this.buchungskreisNr = buchungskreisNr; }
    public String getBezeichnung() { return bezeichnung; }
    public void setBezeichnung(String bezeichnung) { this.bezeichnung = bezeichnung; }
    public String getLand() { return land; }
    public void setLand(String land) { this.land = land; }
    public String getWaehrung() { return waehrung; }
    public void setWaehrung(String waehrung) { this.waehrung = waehrung; }
    public Long getMandantId() { return mandantId; }
    public void setMandantId(Long mandantId) { this.mandantId = mandantId; }
    public Long getKontenplanId() { return kontenplanId; }
    public void setKontenplanId(Long kontenplanId) { this.kontenplanId = kontenplanId; }
    public Long getKkbId() { return kkbId; }
    public void setKkbId(Long kkbId) { this.kkbId = kkbId; }
}