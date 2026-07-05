package com.example.demo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

/**
 * Kreditkontrollbereich – steuert das Kreditlimit-Konzept je Mandant/Buchungskreis-Gruppe.
 *
 * ÄNDERUNG: kreditlimit-Feld ergänzt. Ohne dieses Feld gab es nichts, was
 * beim Buchen/Fakturieren tatsächlich geprüft werden könnte – der KKB war
 * reine Stammdatenverwaltung ohne Wirkung.
 */
@Entity
@Table(name = "KREDITKONTROLLBEREICH")
public class Kreditkontrollbereich {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "kkb_id")
    private Long kkbId;

    @NotBlank
    @Column(name = "kkb_nr", unique = true, nullable = false, length = 10)
    private String kkbNr;          // z.B. "GB00"

    @NotBlank
    @Column(name = "bezeichnung", nullable = false, length = 100)
    private String bezeichnung;

    @Column(name = "waehrung", length = 5)
    private String waehrung = "EUR";

    @Column(name = "kreditlimit", precision = 15, scale = 2)
    private BigDecimal kreditlimit;

    public Kreditkontrollbereich() {}

    public Kreditkontrollbereich(String kkbNr, String bezeichnung, String waehrung) {
        this.kkbNr = kkbNr; this.bezeichnung = bezeichnung; this.waehrung = waehrung;
    }

    public Kreditkontrollbereich(String kkbNr, String bezeichnung, String waehrung, BigDecimal kreditlimit) {
        this.kkbNr = kkbNr; this.bezeichnung = bezeichnung; this.waehrung = waehrung;
        this.kreditlimit = kreditlimit;
    }

    public Long getKkbId() { return kkbId; }
    public void setKkbId(Long kkbId) { this.kkbId = kkbId; }
    public String getKkbNr() { return kkbNr; }
    public void setKkbNr(String kkbNr) { this.kkbNr = kkbNr; }
    public String getBezeichnung() { return bezeichnung; }
    public void setBezeichnung(String bezeichnung) { this.bezeichnung = bezeichnung; }
    public String getWaehrung() { return waehrung; }
    public void setWaehrung(String waehrung) { this.waehrung = waehrung; }
    public BigDecimal getKreditlimit() { return kreditlimit; }
    public void setKreditlimit(BigDecimal kreditlimit) { this.kreditlimit = kreditlimit; }

    @Override
    public String toString() { return "Kreditkontrollbereich{" + kkbNr + " – " + bezeichnung + " Limit=" + kreditlimit + "}"; }
}
