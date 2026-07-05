package com.example.demo.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/** Relation_1 – Verbindungstabelle zwischen Geschäftsfall und Produkt (mit Menge). */
@Entity
@Table(name = "GESCHAEFTSFALL_PRODUKT")
@IdClass(GeschaeftsfallProduktId.class)
public class GeschaeftsfallProdukt {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "geschaeftsf_id", nullable = false)
    private Geschaeftsfall geschaeftsfall;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produkt_produkt_id", nullable = false)
    private Produkt produkt;

    @Positive
    @Column(name = "menge", nullable = false)
    private Integer menge = 1;

    @Column(name = "einzelpreis", precision = 15, scale = 2)
    private BigDecimal einzelpreis;  // Preis zum Buchungszeitpunkt

    // ─── Constructors ─────────────────────────────────────────────
    public GeschaeftsfallProdukt() {}

    public GeschaeftsfallProdukt(Geschaeftsfall geschaeftsfall, Produkt produkt,
                                  Integer menge, BigDecimal einzelpreis) {
        this.geschaeftsfall = geschaeftsfall;
        this.produkt = produkt;
        this.menge = menge;
        this.einzelpreis = einzelpreis;
    }

    // Berechneter Gesamtpreis für diese Position
    public BigDecimal getPositionsBetrag() {
        if (einzelpreis == null) return BigDecimal.ZERO;
        return einzelpreis.multiply(BigDecimal.valueOf(menge));
    }

    // ─── Getters / Setters ────────────────────────────────────────
    public Geschaeftsfall getGeschaeftsfall()                  { return geschaeftsfall; }
    public void setGeschaeftsfall(Geschaeftsfall g)            { this.geschaeftsfall = g; }
    public Produkt getProdukt()                                { return produkt; }
    public void setProdukt(Produkt produkt)                    { this.produkt = produkt; }
    public Integer getMenge()                                  { return menge; }
    public void setMenge(Integer menge)                        { this.menge = menge; }
    public BigDecimal getEinzelpreis()                         { return einzelpreis; }
    public void setEinzelpreis(BigDecimal einzelpreis)         { this.einzelpreis = einzelpreis; }
}
