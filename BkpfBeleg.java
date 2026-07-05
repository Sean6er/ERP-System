package com.example.demo.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * BkpfBeleg – Belegkopf (entspricht SAP BKPF).
 * Gruppiert die Soll- und Haben-Zeilen eines Buchungssatzes zu EINEM Dokument.
 *
 * ÄNDERUNG: buchungskreis ist jetzt eine echte FK auf Buchungskreis
 * (vorher ein loser String "DE00").
 */
@Entity
@Table(name = "BKPF_BELEG")
public class BkpfBeleg {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bkpf_id")
    private Long bkpfId;

    @Column(name = "belegnummer_kopf", unique = true, nullable = false)
    private Long belegnummerKopf;       // = Belegnummer der Soll-Zeile (führend)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buchungskreis_id")
    @JsonIgnoreProperties({"mandant"})
    private Buchungskreis buchungskreis;

    @Column(name = "belegdatum", nullable = false)
    private LocalDate belegdatum;

    @Column(name = "buchungsdatum", nullable = false)
    private LocalDate buchungsdatum;

    @Column(name = "erfassungszeit", nullable = false, updatable = false)
    private LocalDateTime erfassungszeit;

    @Column(name = "belegart", length = 10)
    private String belegart = "SA";     // SA=Sachkontenbeleg, DR=Debitor, KR=Kreditor

    @Column(name = "erfasst_von", length = 50)
    private String erfasstVon;

    @Column(name = "referenz", length = 100)
    private String referenz;            // freier Referenztext (z.B. Rechnungsnummer)

    @JsonIgnoreProperties("bkpfBeleg")
    @OneToMany(mappedBy = "bkpfBeleg", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Buchung> buchungszeilen = new ArrayList<>();

    public BkpfBeleg() {}

    public BkpfBeleg(Long belegnummerKopf, Buchungskreis buchungskreis, LocalDate belegdatum,
                      LocalDate buchungsdatum, String belegart, String erfasstVon, String referenz) {
        this.belegnummerKopf = belegnummerKopf;
        this.buchungskreis = buchungskreis;
        this.belegdatum = belegdatum;
        this.buchungsdatum = buchungsdatum;
        this.belegart = belegart;
        this.erfasstVon = erfasstVon;
        this.referenz = referenz;
    }

    @PrePersist
    public void prePersist() {
        if (erfassungszeit == null) erfassungszeit = LocalDateTime.now();
    }

    public Long getBkpfId() { return bkpfId; }
    public void setBkpfId(Long bkpfId) { this.bkpfId = bkpfId; }
    public Long getBelegnummerKopf() { return belegnummerKopf; }
    public void setBelegnummerKopf(Long belegnummerKopf) { this.belegnummerKopf = belegnummerKopf; }
    public Buchungskreis getBuchungskreis() { return buchungskreis; }
    public void setBuchungskreis(Buchungskreis buchungskreis) { this.buchungskreis = buchungskreis; }
    public LocalDate getBelegdatum() { return belegdatum; }
    public void setBelegdatum(LocalDate belegdatum) { this.belegdatum = belegdatum; }
    public LocalDate getBuchungsdatum() { return buchungsdatum; }
    public void setBuchungsdatum(LocalDate buchungsdatum) { this.buchungsdatum = buchungsdatum; }
    public LocalDateTime getErfassungszeit() { return erfassungszeit; }
    public void setErfassungszeit(LocalDateTime t) { this.erfassungszeit = t; }
    public String getBelegart() { return belegart; }
    public void setBelegart(String belegart) { this.belegart = belegart; }
    public String getErfasstVon() { return erfasstVon; }
    public void setErfasstVon(String erfasstVon) { this.erfasstVon = erfasstVon; }
    public String getReferenz() { return referenz; }
    public void setReferenz(String referenz) { this.referenz = referenz; }
    public List<Buchung> getBuchungszeilen() { return buchungszeilen; }
    public void setBuchungszeilen(List<Buchung> b) { this.buchungszeilen = b; }

    @Override
    public String toString() {
        return "BkpfBeleg{" + belegnummerKopf + " " + belegart + " " + buchungsdatum + "}";
    }
}
