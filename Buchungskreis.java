package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

/**
 * Buchungskreis – Kleinste Einheit des externen Rechnungswesens.
 * Jeder Buchungskreis hat eine vollständige, abgeschlossene Buchhaltung
 * (Bilanz, GuV) – entspricht einer Ländergesellschaft.
 * Beispiele: US00, DE00, GB00, CA00, AU00, JP00
 *
 * ÄNDERUNG: @JsonIgnoreProperties auf mandant, damit die Serialisierung
 * nicht in eine Endlosschleife Mandant -> buchungskreise -> Buchungskreis
 * -> mandant -> buchungskreise -> ... läuft, sobald Buchungskreise
 * tatsächlich befüllt sind (z.B. beim Serialisieren einer Buchung, die
 * jetzt eine echte Buchungskreis-FK trägt).
 */
@Entity
@Table(name = "BUCHUNGSKREIS")
public class Buchungskreis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "buchungskreis_id")
    private Long buchungskreisId;

    @NotBlank
    @Column(name = "buchungskreis_nr", unique = true, nullable = false, length = 10)
    private String buchungskreisNr;    // z.B. "DE00", "US00"

    @NotBlank
    @Column(name = "bezeichnung", nullable = false, length = 100)
    private String bezeichnung;        // z.B. "Global Bike Germany GmbH"

    @Column(name = "land", length = 50)
    private String land;               // z.B. "Deutschland"

    @Column(name = "waehrung", length = 5)
    private String waehrung = "EUR";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mandant_id", nullable = false)
    @JsonIgnoreProperties({"buchungskreise"})
    private Mandant mandant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kontenplan_id")
    private Kontenplan kontenplan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kkb_id")
    private Kreditkontrollbereich kreditkontrollbereich;

    // ─── Constructors ─────────────────────────────────────────────
    public Buchungskreis() {}

    public Buchungskreis(String buchungskreisNr, String bezeichnung,
                         String land, String waehrung, Mandant mandant) {
        this.buchungskreisNr = buchungskreisNr;
        this.bezeichnung = bezeichnung;
        this.land = land;
        this.waehrung = waehrung;
        this.mandant = mandant;
    }

    // ─── Getters / Setters ────────────────────────────────────────
    public Long getBuchungskreisId()                           { return buchungskreisId; }
    public void setBuchungskreisId(Long buchungskreisId)       { this.buchungskreisId = buchungskreisId; }
    public String getBuchungskreisNr()                         { return buchungskreisNr; }
    public void setBuchungskreisNr(String buchungskreisNr)     { this.buchungskreisNr = buchungskreisNr; }
    public String getBezeichnung()                             { return bezeichnung; }
    public void setBezeichnung(String bezeichnung)             { this.bezeichnung = bezeichnung; }
    public String getLand()                                    { return land; }
    public void setLand(String land)                           { this.land = land; }
    public String getWaehrung()                                { return waehrung; }
    public void setWaehrung(String waehrung)                   { this.waehrung = waehrung; }
    public Mandant getMandant()                                { return mandant; }
    public void setMandant(Mandant mandant)                    { this.mandant = mandant; }
    public Kontenplan getKontenplan() { return kontenplan; }
    public void setKontenplan(Kontenplan kontenplan) { this.kontenplan = kontenplan; }
    public Kreditkontrollbereich getKreditkontrollbereich() { return kreditkontrollbereich; }
    public void setKreditkontrollbereich(Kreditkontrollbereich kkb) { this.kreditkontrollbereich = kkb; }

    @Override
    public String toString() {
        return "Buchungskreis{" + buchungskreisNr + " – " + bezeichnung + "}";
    }
}
