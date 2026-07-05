package com.example.demo.entity;


import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Rechnung – Ausgangs- oder Eingangsrechnung. Aus dem ERM: Rechnung-Tabelle. */
@Entity
@Table(name = "RECHNUNG")
public class Rechnung {

    public enum RechnungStatus {
        OFFEN, BEZAHLT, STORNIERT, UEBERFAELLIG
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rechnung_id")
    private Long rechnungId;

    @NotNull
    @Column(name = "rechnungsdatum", nullable = false)
    private LocalDate rechnungsdatum;

    @Column(name = "faelligkeit")
    private LocalDate faelligkeit;

    @NotNull
    @Positive
    @Column(name = "betrag", nullable = false, precision = 15, scale = 2)
    private BigDecimal betrag;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private RechnungStatus status = RechnungStatus.OFFEN;

    /** ÄNDERUNG: @JsonIgnoreProperties ergänzt, damit die neuen Rechnung-Endpunkte
     *  (RechnungController) kein unnötig tiefes/aufgeblähtes JSON liefern –
     *  die rechnungen-Liste des Geschäftsfalls und seine (aktuell ungenutzten)
     *  Positionen sind für die Rechnung-Ansicht irrelevant. */
    @JsonIgnoreProperties({"rechnungen", "positionen"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "geschaeftsf_id", nullable = false)
    private Geschaeftsfall geschaeftsfall;

    @JsonIgnoreProperties("rechnung")
    @OneToMany(mappedBy = "rechnung", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Buchung> buchungen = new ArrayList<>();
    
    // ─── Constructors ─────────────────────────────────────────────
    public Rechnung() {}

    public Rechnung(LocalDate rechnungsdatum, LocalDate faelligkeit,
                    BigDecimal betrag, Geschaeftsfall geschaeftsfall) {
        this.rechnungsdatum = rechnungsdatum;
        this.faelligkeit = faelligkeit;
        this.betrag = betrag;
        this.geschaeftsfall = geschaeftsfall;
        this.status = RechnungStatus.OFFEN;
    }

    // ─── Getters / Setters ────────────────────────────────────────
    public Long getRechnungId()                              { return rechnungId; }
    public void setRechnungId(Long rechnungId)               { this.rechnungId = rechnungId; }
    public LocalDate getRechnungsdatum()                     { return rechnungsdatum; }
    public void setRechnungsdatum(LocalDate rechnungsdatum)  { this.rechnungsdatum = rechnungsdatum; }
    public LocalDate getFaelligkeit()                        { return faelligkeit; }
    public void setFaelligkeit(LocalDate faelligkeit)        { this.faelligkeit = faelligkeit; }
    public BigDecimal getBetrag()                            { return betrag; }
    public void setBetrag(BigDecimal betrag)                 { this.betrag = betrag; }
    public RechnungStatus getStatus()                        { return status; }
    public void setStatus(RechnungStatus status)             { this.status = status; }
    public Geschaeftsfall getGeschaeftsfall()                { return geschaeftsfall; }
    public void setGeschaeftsfall(Geschaeftsfall g)          { this.geschaeftsfall = g; }
    public List<Buchung> getBuchungen()                      { return buchungen; }
    public void setBuchungen(List<Buchung> buchungen)        { this.buchungen = buchungen; }

    @Override
    public String toString() {
        return "Rechnung{" + rechnungId + " Betrag=" + betrag + " Status=" + status + "}";
    }
}