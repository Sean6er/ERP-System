package com.example.demo.entity;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Geschäftsfall – Zentrales Transaktionsobjekt.
 * Verbindet Kunde, Mitarbeiter, Rechnung und Produkte.
 * Entspricht dem Beleg im SAP-Sinne (Kopf).
 *
 * ÄNDERUNG: buchungskreis ist jetzt eine echte FK auf Buchungskreis.
 * Analog zu SAP wird der Buchungskreis EINMALIG beim Anlegen des
 * Geschäftsfalls fixiert (siehe FiSdIntegrationService /
 * FiMmIntegrationService) und vererbt sich auf alle nachfolgenden
 * FI-Buchungen desselben Vorgangs (Faktura → Zahlungseingang bzw.
 * Wareneingang → Zahlungsausgang). Das ist die Voraussetzung dafür, dass
 * Rechnungen/offene Posten überhaupt nach Buchungskreis filterbar sind –
 * Rechnung selbst trägt bewusst KEINE eigene Buchungskreis-FK, sondern
 * erbt sie über geschaeftsfall.buchungskreis.
 *
 * ÄNDERUNG (Nebenbuch-Feature): echte Lieferant-FK ergänzt, analog zu
 * kunde. Vorher gab es KEINE Verknüpfung zwischen einem EINKAUF-
 * Geschäftsfall und dem tatsächlichen Kreditor (Lieferant) – der
 * Lieferantenname wurde nur als Freitext im Buchungstext mitgeführt.
 * Damit ließ sich eine Kreditoren-Nebenbuch-Abstimmung (siehe
 * NebenbuchService) nie umsetzen. Bewusst UNIDIREKTIONAL (kein
 * List<Geschaeftsfall> auf Lieferant-Seite) – dadurch keine
 * Serialisierungs-Schleife und keine Änderung an Lieferant.java nötig.
 *
 * BUGFIX: @JsonIgnoreProperties an drei Stellen ergänzt, um Endlosschleifen
 * bei der JSON-Serialisierung zu verhindern (traten auf, sobald ein
 * Geschäftsfall tatsächlich befüllt war, z.B. nach einer FI-SD/FI-MM-Buchung):
 *   - kunde:       Geschaeftsfall -> kunde -> geschaeftsfaelle -> Geschaeftsfall -> ...
 *   - rechnungen:  Geschaeftsfall -> rechnungen -> Rechnung -> geschaeftsfall -> ...
 *   - positionen:  Geschaeftsfall -> positionen -> GeschaeftsfallProdukt -> geschaeftsfall -> ...
 */
@Entity
@Table(name = "GESCHAEFTSFALL")
public class Geschaeftsfall {

    public enum GeschaeftsfallStatus {
        OFFEN, IN_BEARBEITUNG, ABGESCHLOSSEN, STORNIERT
    }

    public enum GeschaeftsfallTyp {
        VERKAUF,        // SD → FI
        EINKAUF,        // MM → FI
        ZAHLUNG,        // Bank → FI
        INTERN          // interne Umbuchung
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "geschaeftsfall_id")
    private Long geschaeftsfallId;

    @NotNull
    @Column(name = "datum", nullable = false)
    private LocalDate datum;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30)
    private GeschaeftsfallStatus status = GeschaeftsfallStatus.OFFEN;

    @Column(name = "gesamtbetrag", precision = 19, scale = 2)
    private BigDecimal gesamtbetrag = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "typ", length = 30)
    private GeschaeftsfallTyp typ;

    // ─── Beziehungen ──────────────────────────────────────────────

    @JsonIgnoreProperties("geschaeftsfaelle")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kunde_kunden_id")
    private Kunde kunde;

    /** ÄNDERUNG (Nebenbuch-Feature): echte Kreditor-FK – siehe Klassen-Javadoc oben. */
    @JsonIgnoreProperties({"produkte"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lieferant_id")
    private Lieferant lieferant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mitarbeiter_mitarbeiter_id")
    private Mitarbeiter mitarbeiter;

    @JsonIgnoreProperties("geschaeftsfall")
    @OneToMany(mappedBy = "geschaeftsfall", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Rechnung> rechnungen = new ArrayList<>();

    @JsonIgnoreProperties("geschaeftsfall")
    @OneToMany(mappedBy = "geschaeftsfall", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<GeschaeftsfallProdukt> positionen = new ArrayList<>();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "geschaeftsbereich_id")
    private Geschaeftsbereich geschaeftsbereich;

    /** ÄNDERUNG: echte Buchungskreis-FK – siehe Klassen-Javadoc oben. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buchungskreis_id")
    @JsonIgnoreProperties({"mandant"})
    private Buchungskreis buchungskreis;

    // ─── Constructors ─────────────────────────────────────────────
    public Geschaeftsfall() {}

    public Geschaeftsfall(LocalDate datum, GeschaeftsfallTyp typ,
                          Kunde kunde, Mitarbeiter mitarbeiter) {
        this.datum = datum;
        this.typ = typ;
        this.kunde = kunde;
        this.mitarbeiter = mitarbeiter;
        this.status = GeschaeftsfallStatus.OFFEN;
    }

    // ─── Getters / Setters ────────────────────────────────────────
    public Long getGeschaeftsfallId()                              { return geschaeftsfallId; }
    public void setGeschaeftsfallId(Long id)                       { this.geschaeftsfallId = id; }
    public LocalDate getDatum()                                    { return datum; }
    public void setDatum(LocalDate datum)                          { this.datum = datum; }
    public GeschaeftsfallStatus getStatus()                        { return status; }
    public void setStatus(GeschaeftsfallStatus status)             { this.status = status; }
    public BigDecimal getGesamtbetrag()                            { return gesamtbetrag; }
    public void setGesamtbetrag(BigDecimal gesamtbetrag)           { this.gesamtbetrag = gesamtbetrag; }
    public GeschaeftsfallTyp getTyp()                              { return typ; }
    public void setTyp(GeschaeftsfallTyp typ)                      { this.typ = typ; }
    public Kunde getKunde()                                        { return kunde; }
    public void setKunde(Kunde kunde)                              { this.kunde = kunde; }
    public Lieferant getLieferant()                                { return lieferant; }
    public void setLieferant(Lieferant lieferant)                  { this.lieferant = lieferant; }
    public Mitarbeiter getMitarbeiter()                            { return mitarbeiter; }
    public void setMitarbeiter(Mitarbeiter mitarbeiter)            { this.mitarbeiter = mitarbeiter; }
    public List<Rechnung> getRechnungen()                          { return rechnungen; }
    public void setRechnungen(List<Rechnung> rechnungen)           { this.rechnungen = rechnungen; }
    public List<GeschaeftsfallProdukt> getPositionen()             { return positionen; }
    public void setPositionen(List<GeschaeftsfallProdukt> pos)     { this.positionen = pos; }
    public Geschaeftsbereich getGeschaeftsbereich() 			   { return geschaeftsbereich; }
    public void setGeschaeftsbereich(Geschaeftsbereich g) 		   { this.geschaeftsbereich = g; }
    public Buchungskreis getBuchungskreis()                        { return buchungskreis; }
    public void setBuchungskreis(Buchungskreis buchungskreis)      { this.buchungskreis = buchungskreis; }

    @Override
    public String toString() {
        return "Geschaeftsfall{" + geschaeftsfallId + " Typ=" + typ + " Status=" + status + " Betrag=" + gesamtbetrag + "}";
    }
}