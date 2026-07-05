package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Request-Body für POST /api/buchungen/buchen */
public class BuchungsRequest {
    @NotNull(message = "Sollkonto-Nummer ist Pflichtfeld")
    private Integer sollKontoNr;
    @NotNull(message = "Habenkonto-Nummer ist Pflichtfeld")
    private Integer habenKontoNr;
    @NotNull @Positive(message = "Betrag muss positiv sein")
    private BigDecimal betrag;
    @NotBlank(message = "Buchungstext ist Pflichtfeld")
    private String buchungstext;
    private LocalDate buchungsdatum;
    private String erfasstVon = "System";
    private Long rechnungId;

    /** ÄNDERUNG: Buchungskreis-Nr, z.B. "DE00" oder "US00". Optional – Default wird im Service auf "DE00" aufgelöst. */
    private String buchungskreisNr;

    public Integer getSollKontoNr() { return sollKontoNr; }
    public void setSollKontoNr(Integer sollKontoNr) { this.sollKontoNr = sollKontoNr; }
    public Integer getHabenKontoNr() { return habenKontoNr; }
    public void setHabenKontoNr(Integer habenKontoNr) { this.habenKontoNr = habenKontoNr; }
    public BigDecimal getBetrag() { return betrag; }
    public void setBetrag(BigDecimal betrag) { this.betrag = betrag; }
    public String getBuchungstext() { return buchungstext; }
    public void setBuchungstext(String buchungstext) { this.buchungstext = buchungstext; }
    public LocalDate getBuchungsdatum() { return buchungsdatum != null ? buchungsdatum : LocalDate.now(); }
    public void setBuchungsdatum(LocalDate buchungsdatum) { this.buchungsdatum = buchungsdatum; }
    public String getErfasstVon() { return erfasstVon; }
    public void setErfasstVon(String erfasstVon) { this.erfasstVon = erfasstVon; }
    public Long getRechnungId() { return rechnungId; }
    public void setRechnungId(Long rechnungId) { this.rechnungId = rechnungId; }
    public String getBuchungskreisNr() { return buchungskreisNr; }
    public void setBuchungskreisNr(String buchungskreisNr) { this.buchungskreisNr = buchungskreisNr; }
}
