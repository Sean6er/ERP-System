package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Buchung – FI-Buchungssatz / Journaleintrag.
 *
 * SAP-Belegprinzip:
 *  - Jeder Geschäftsvorfall erzeugt einen eindeutig nummerierten Beleg.
 *  - Einmal gespeichert, kann der Beleg NICHT gelöscht werden (GoBD-Konformität).
 *  - Stornierungen erzeugen neue Gegenbelege (storniertDurch / stornoVon).
 *
 * Entspricht ACDOCA (Universal Journal) in S/4HANA.
 *
 * ÄNDERUNG: buchungskreis ist jetzt eine echte FK auf Buchungskreis
 * (vorher ein loser String "DE00"). Dadurch lässt sich Reporting und
 * Journal tatsächlich nach Buchungskreis filtern.
 */
@Entity
@Table(name = "BUCHUNG")
public class Buchung {

    public enum SollHaben {
        SOLL, HABEN
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "buchung_id")
    private Long buchungId;

    @NotNull
    @Column(name = "belegnummer", nullable = false, unique = true)
    private Long belegnummer;          // Eindeutige Belegnummer (auto-generiert)

    @NotNull
    @Column(name = "buchungsdatum", nullable = false)
    private LocalDate buchungsdatum;

    @Column(name = "erfassungsdatum", nullable = false, updatable = false)
    private LocalDateTime erfassungsdatum;  // Zeitstempel der Erfassung (GoBD)

    @NotNull
    @Positive
    @Column(name = "betrag", nullable = false, precision = 10, scale = 2)
    private BigDecimal betrag;

    @Column(name = "belegwaehrung", length = 5)
    private String belegwaehrung = "EUR";

    @Column(name = "hauswaehrung", length = 5)
    private String hauswaehrung = "EUR";

    @Column(name = "wechselkurs", precision = 10, scale = 6)
    private BigDecimal wechselkurs = BigDecimal.ONE;

    @Column(name = "betrag_hauswaehrung", precision = 15, scale = 2)
    private BigDecimal betragHauswaehrung;

    @Column(name = "buchungstext", length = 100)
    private String buchungstext;

    @Enumerated(EnumType.STRING)
    @Column(name = "soll_haben", length = 10, nullable = false)
    private SollHaben sollHaben;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buchungskreis_id")
    @JsonIgnoreProperties({"mandant"})
    private Buchungskreis buchungskreis;

    @Column(name = "erfasst_von", length = 50)
    private String erfasstVon;             // Buchhalter (Audit-Trail)

    // ─── Verknüpfungen (Belegprinzip-Struktur wie BKPF/ACDOCA) ────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rechnung_rechnung_id")
    @JsonIgnoreProperties({"buchungen", "geschaeftsfall"})
    private Rechnung rechnung;             // Zugehörige Rechnung (FK aus ERM)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "konto_konto_id", nullable = false)
    private Konto konto;                   // Bebuchtes Sachkonto

    // ─── Storno-Logik ─────────────────────────────────────────────

    @Column(name = "storniert")
    private Boolean storniert = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storno_von_id")
    private Buchung stornoVon;             // Welchen Beleg storniert diese Buchung?

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bkpf_id")
    @JsonIgnoreProperties({"buchungszeilen"})
    private BkpfBeleg bkpfBeleg;

    // ─── Constructors ─────────────────────────────────────────────
    public Buchung() {}

    public Buchung(Long belegnummer, LocalDate buchungsdatum, BigDecimal betrag,
                   String buchungstext, SollHaben sollHaben, Konto konto,
                   Rechnung rechnung, String erfasstVon) {
        this.belegnummer = belegnummer;
        this.buchungsdatum = buchungsdatum;
        this.erfassungsdatum = LocalDateTime.now();
        this.betrag = betrag;
        this.buchungstext = buchungstext;
        this.sollHaben = sollHaben;
        this.konto = konto;
        this.rechnung = rechnung;
        this.erfasstVon = erfasstVon;
        this.storniert = false;
    }

    @PrePersist
    public void prePersist() {
        if (this.erfassungsdatum == null) {
            this.erfassungsdatum = LocalDateTime.now();
        }
        if (this.betragHauswaehrung == null && this.betrag != null && this.wechselkurs != null) {
            this.betragHauswaehrung = this.betrag.multiply(this.wechselkurs);
        }
    }

    // ─── Getters / Setters ────────────────────────────────────────
    public Long getBuchungId()                             { return buchungId; }
    public void setBuchungId(Long buchungId)               { this.buchungId = buchungId; }
    public Long getBelegnummer()                           { return belegnummer; }
    public void setBelegnummer(Long belegnummer)           { this.belegnummer = belegnummer; }
    public LocalDate getBuchungsdatum()                    { return buchungsdatum; }
    public void setBuchungsdatum(LocalDate buchungsdatum)  { this.buchungsdatum = buchungsdatum; }
    public LocalDateTime getErfassungsdatum()              { return erfassungsdatum; }
    public void setErfassungsdatum(LocalDateTime t)        { this.erfassungsdatum = t; }
    public BigDecimal getBetrag()                          { return betrag; }
    public void setBetrag(BigDecimal betrag)               { this.betrag = betrag; }
    public String getBuchungstext()                        { return buchungstext; }
    public void setBuchungstext(String buchungstext)       { this.buchungstext = buchungstext; }
    public SollHaben getSollHaben()                        { return sollHaben; }
    public void setSollHaben(SollHaben sollHaben)          { this.sollHaben = sollHaben; }
    public Buchungskreis getBuchungskreis()                { return buchungskreis; }
    public void setBuchungskreis(Buchungskreis buchungskreis) { this.buchungskreis = buchungskreis; }
    public String getErfasstVon()                          { return erfasstVon; }
    public void setErfasstVon(String erfasstVon)           { this.erfasstVon = erfasstVon; }
    public Rechnung getRechnung()                          { return rechnung; }
    public void setRechnung(Rechnung rechnung)             { this.rechnung = rechnung; }
    public Konto getKonto()                                { return konto; }
    public void setKonto(Konto konto)                      { this.konto = konto; }
    public Boolean getStorniert()                          { return storniert; }
    public void setStorniert(Boolean storniert)            { this.storniert = storniert; }
    public Buchung getStornoVon()                          { return stornoVon; }
    public void setStornoVon(Buchung stornoVon)            { this.stornoVon = stornoVon; }
    public String getBelegwaehrung() { return belegwaehrung; }
    public void setBelegwaehrung(String belegwaehrung) { this.belegwaehrung = belegwaehrung; }
    public String getHauswaehrung() { return hauswaehrung; }
    public void setHauswaehrung(String hauswaehrung) { this.hauswaehrung = hauswaehrung; }
    public BigDecimal getWechselkurs() { return wechselkurs; }
    public void setWechselkurs(BigDecimal wechselkurs) { this.wechselkurs = wechselkurs; }
    public BigDecimal getBetragHauswaehrung() { return betragHauswaehrung; }
    public void setBetragHauswaehrung(BigDecimal b) { this.betragHauswaehrung = b; }
    public BkpfBeleg getBkpfBeleg() { return bkpfBeleg; }
    public void setBkpfBeleg(BkpfBeleg bkpfBeleg) { this.bkpfBeleg = bkpfBeleg; }


    @Override
    public String toString() {
        return "Buchung{Beleg=" + belegnummer + " " + sollHaben + " " + betrag + " EUR – " + buchungstext + "}";
    }
}
