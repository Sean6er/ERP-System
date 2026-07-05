package com.example.demo.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Konto – Hauptbuchkonto (General Ledger Account).
 * Kontotypen: AKTIV, PASSIV, AUFWAND, ERTRAG
 *
 * Entspricht der KONTO-Tabelle aus dem ERM.
 *
 * ÄNDERUNG: kontenplan ist eine echte FK auf die Kontenplan-Entity (EIN
 * Kontenplan GL00 für den gesamten Konzern – SAP-Standard: ein Buchungskreis
 * hat genau einen Kontenplan).
 *
 * ÄNDERUNG (Kontengruppen-Feature): zusätzliche, optionale FK auf
 * Kontengruppe ergänzt. Das ist das SAP-Konzept "Kontengruppe" (Account
 * Group, T077S) – NICHT zu verwechseln mit dem Kontenplan! Die
 * Kontengruppe klassifiziert Sachkonten über einen Nummernkreis (z.B.
 * GL00 = 0–199999 = Anlage-/Umlaufvermögen) INNERHALB des einen
 * Kontenplans GL00. Siehe Kontengruppe.java-Javadoc für die Abgrenzung.
 */
@Entity
@Table(name = "KONTO")
public class Konto {

    public enum Kontotyp {
        AKTIV,      // z.B. Bank, Forderungen, Anlagen
        PASSIV,     // z.B. Verbindlichkeiten, Eigenkapital
        AUFWAND,    // z.B. Mietaufwand, Materialaufwand
        ERTRAG      // z.B. Umsatzerlöse
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "konto_id")
    private Long kontoId;

    @NotNull
    @Column(name = "kontonummer", unique = true, nullable = false)
    private Integer kontonummer;           // z.B. 100000 (Bank), 120000 (Forderungen)

    @NotBlank
    @Column(name = "kontobezeichnung", nullable = false, length = 100)
    private String kontobezeichnung;       // z.B. "Bank", "Mietaufwand"

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "kontotyp", nullable = false, length = 20)
    private Kontotyp kontotyp;

    @Column(name = "saldo", precision = 15, scale = 2)
    private BigDecimal saldo = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kontenplan_id")
    private Kontenplan kontenplan;

    /** ÄNDERUNG (Kontengruppen-Feature): optionale Kontengruppen-FK. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kontengruppe_id")
    private Kontengruppe kontengruppe;

    // ─── Constructors ─────────────────────────────────────────────
    public Konto() {}

    public Konto(Integer kontonummer, String kontobezeichnung, Kontotyp kontotyp) {
        this.kontonummer = kontonummer;
        this.kontobezeichnung = kontobezeichnung;
        this.kontotyp = kontotyp;
        this.saldo = BigDecimal.ZERO;
    }

    // ─── Business Logic ───────────────────────────────────────────

    /**
     * Erhöht oder vermindert den Saldo je nach Kontotyp und Soll/Haben.
     * Doppelte Buchführung: Soll erhöht Aktiv/Aufwand, Haben erhöht Passiv/Ertrag.
     */
    public void aktualisiereSaldo(BigDecimal betrag, boolean isSoll) {
        if (kontotyp == Kontotyp.AKTIV || kontotyp == Kontotyp.AUFWAND) {
            this.saldo = isSoll ? this.saldo.add(betrag) : this.saldo.subtract(betrag);
        } else {
            this.saldo = isSoll ? this.saldo.subtract(betrag) : this.saldo.add(betrag);
        }
    }

    // ─── Getters / Setters ────────────────────────────────────────
    public Long getKontoId()                             { return kontoId; }
    public void setKontoId(Long kontoId)                 { this.kontoId = kontoId; }
    public Integer getKontonummer()                      { return kontonummer; }
    public void setKontonummer(Integer kontonummer)      { this.kontonummer = kontonummer; }
    public String getKontobezeichnung()                  { return kontobezeichnung; }
    public void setKontobezeichnung(String name)         { this.kontobezeichnung = name; }
    public Kontotyp getKontotyp()                        { return kontotyp; }
    public void setKontotyp(Kontotyp kontotyp)           { this.kontotyp = kontotyp; }
    public BigDecimal getSaldo()                         { return saldo; }
    public void setSaldo(BigDecimal saldo)               { this.saldo = saldo; }
    public Kontenplan getKontenplan()                    { return kontenplan; }
    public void setKontenplan(Kontenplan kontenplan)     { this.kontenplan = kontenplan; }
    public Kontengruppe getKontengruppe()                { return kontengruppe; }
    public void setKontengruppe(Kontengruppe kontengruppe) { this.kontengruppe = kontengruppe; }

    @Override
    public String toString() {
        return "Konto{" + kontonummer + " – " + kontobezeichnung + " [" + kontotyp + "] Saldo=" + saldo + "}";
    }
}