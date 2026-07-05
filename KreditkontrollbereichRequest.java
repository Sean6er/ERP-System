package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

/** Request-Body für POST /api/orgstruktur/kreditkontrollbereiche */
public class KreditkontrollbereichRequest {

    @NotBlank(message = "KKB-Nr ist Pflichtfeld")
    private String kkbNr;

    @NotBlank(message = "Bezeichnung ist Pflichtfeld")
    private String bezeichnung;

    private String waehrung = "EUR";

    private BigDecimal kreditlimit;

    public String getKkbNr() { return kkbNr; }
    public void setKkbNr(String kkbNr) { this.kkbNr = kkbNr; }
    public String getBezeichnung() { return bezeichnung; }
    public void setBezeichnung(String bezeichnung) { this.bezeichnung = bezeichnung; }
    public String getWaehrung() { return waehrung; }
    public void setWaehrung(String waehrung) { this.waehrung = waehrung; }
    public BigDecimal getKreditlimit() { return kreditlimit; }
    public void setKreditlimit(BigDecimal kreditlimit) { this.kreditlimit = kreditlimit; }
}
