package com.example.demo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Kontengruppe – entspricht dem SAP-Konzept "Kontengruppe" (Account Group,
 * Tabelle T077S). Steuert NICHT den Kontenplan (davon gibt es weiterhin nur
 * EINEN, GL00, für den gesamten Konzern – siehe Kontenplan.java), sondern
 * klassifiziert die Sachkonten INNERHALB dieses einen Kontenplans über
 * einen festen Nummernkreis (vonNummer–bisNummer).
 *
 * Hintergrund/Abgrenzung: In SAP wird die Kontenklasse (Vermögen, Kapital,
 * Aufwand, Ertrag ...) NICHT durch mehrere Kontenplan-Objekte abgebildet –
 * ein Buchungskreis hat immer genau EINEN Kontenplan. Die Klassifizierung
 * läuft entweder über den Nummernkreis selbst (SKR03/04-Logik, bereits im
 * DataLoader umgesetzt) oder – wie hier zusätzlich ergänzt – über eine
 * explizite Kontengruppe mit Nummernkreis-Validierung beim Kontoanlegen
 * (analog zur SAP-Transaktion FS00 → Feld "Kontengruppe").
 *
 * Konkrete Ausprägung für Global Bike (entspricht der gewünschten
 * GL00–GL05-Einteilung, jetzt fachlich korrekt als Kontengruppe statt als
 * eigener Kontenplan modelliert):
 *   GL00  0      – 199999   Anlage- und Umlaufvermögen (AKTIV)
 *   GL01  200000 – 299999   Eigenkapital und Rückstellungen (PASSIV)
 *   GL02  300000 – 399999   Verbindlichkeiten (PASSIV)
 *   GL03  400000 – 799999   Betriebliche Aufwendungen (AUFWAND)
 *   GL04  800000 – 899999   Erlöse (ERTRAG)
 *   GL05  900000 – 999999   Vortrags- und statistische Konten
 */
@Entity
@Table(name = "KONTENGRUPPE")
public class Kontengruppe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "kontengruppe_id")
    private Long kontengruppeId;

    @NotBlank
    @Column(name = "gruppen_nr", unique = true, nullable = false, length = 10)
    private String gruppenNr;          // z.B. "GL00".."GL05"

    @NotBlank
    @Column(name = "bezeichnung", nullable = false, length = 100)
    private String bezeichnung;

    @NotNull
    @Column(name = "von_nummer", nullable = false)
    private Integer vonNummer;

    @NotNull
    @Column(name = "bis_nummer", nullable = false)
    private Integer bisNummer;

    public Kontengruppe() {}

    public Kontengruppe(String gruppenNr, String bezeichnung, Integer vonNummer, Integer bisNummer) {
        this.gruppenNr = gruppenNr;
        this.bezeichnung = bezeichnung;
        this.vonNummer = vonNummer;
        this.bisNummer = bisNummer;
    }

    /** Prüft, ob eine Kontonummer in den Nummernkreis dieser Kontengruppe fällt. */
    public boolean enthaelt(Integer kontonummer) {
        return kontonummer != null && kontonummer >= vonNummer && kontonummer <= bisNummer;
    }

    public Long getKontengruppeId() { return kontengruppeId; }
    public void setKontengruppeId(Long id) { this.kontengruppeId = id; }
    public String getGruppenNr() { return gruppenNr; }
    public void setGruppenNr(String gruppenNr) { this.gruppenNr = gruppenNr; }
    public String getBezeichnung() { return bezeichnung; }
    public void setBezeichnung(String bezeichnung) { this.bezeichnung = bezeichnung; }
    public Integer getVonNummer() { return vonNummer; }
    public void setVonNummer(Integer vonNummer) { this.vonNummer = vonNummer; }
    public Integer getBisNummer() { return bisNummer; }
    public void setBisNummer(Integer bisNummer) { this.bisNummer = bisNummer; }

    @Override
    public String toString() {
        return "Kontengruppe{" + gruppenNr + " [" + vonNummer + "-" + bisNummer + "] – " + bezeichnung + "}";
    }
}