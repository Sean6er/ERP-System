package com.example.demo.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/** Produkt – Artikel/Ware. Verbindet Lieferant und Geschäftsfall. */
@Entity
@Table(name = "PRODUKT")
public class Produkt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "produkt_id")
    private Long produktId;

    @NotBlank
    @Column(name = "bezeichnung", nullable = false, length = 100)
    private String bezeichnung;

    @NotNull
    @PositiveOrZero
    @Column(name = "einkaufspreis", nullable = false, precision = 15, scale = 2)
    private BigDecimal einkaufspreis;

    @NotNull
    @PositiveOrZero
    @Column(name = "verkaufspreis", nullable = false, precision = 15, scale = 2)
    private BigDecimal verkaufspreis;

    @PositiveOrZero
    @Column(name = "lagerbestand")
    private Integer lagerbestand = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lieferant_lieferanten_id", nullable = false)
    private Lieferant lieferant;

    // ─── Constructors ─────────────────────────────────────────────
    public Produkt() {}

    public Produkt(String bezeichnung, BigDecimal einkaufspreis,
                   BigDecimal verkaufspreis, Integer lagerbestand, Lieferant lieferant) {
        this.bezeichnung = bezeichnung;
        this.einkaufspreis = einkaufspreis;
        this.verkaufspreis = verkaufspreis;
        this.lagerbestand = lagerbestand;
        this.lieferant = lieferant;
    }

    // ─── Getters / Setters ────────────────────────────────────────
    public Long getProduktId()                             { return produktId; }
    public void setProduktId(Long produktId)               { this.produktId = produktId; }
    public String getBezeichnung()                         { return bezeichnung; }
    public void setBezeichnung(String bezeichnung)         { this.bezeichnung = bezeichnung; }
    public BigDecimal getEinkaufspreis()                   { return einkaufspreis; }
    public void setEinkaufspreis(BigDecimal einkaufspreis) { this.einkaufspreis = einkaufspreis; }
    public BigDecimal getVerkaufspreis()                   { return verkaufspreis; }
    public void setVerkaufspreis(BigDecimal verkaufspreis) { this.verkaufspreis = verkaufspreis; }
    public Integer getLagerbestand()                       { return lagerbestand; }
    public void setLagerbestand(Integer lagerbestand)      { this.lagerbestand = lagerbestand; }
    public Lieferant getLieferant()                        { return lieferant; }
    public void setLieferant(Lieferant lieferant)          { this.lieferant = lieferant; }

    @Override
    public String toString() {
        return "Produkt{" + produktId + " – " + bezeichnung + " EK=" + einkaufspreis + " VK=" + verkaufspreis + "}";
    }
}

