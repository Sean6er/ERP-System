package com.example.demo.entity;


import java.io.Serializable;
import java.util.Objects;

/** Zusammengesetzter Primärschlüssel für Relation_1 (Geschäftsfall ↔ Produkt). */
public class GeschaeftsfallProduktId implements Serializable {

    private Long geschaeftsfall;
    private Long produkt;

    public GeschaeftsfallProduktId() {}

    public GeschaeftsfallProduktId(Long geschaeftsfall, Long produkt) {
        this.geschaeftsfall = geschaeftsfall;
        this.produkt = produkt;
    }

    public Long getGeschaeftsfall() { return geschaeftsfall; }
    public void setGeschaeftsfall(Long g) { this.geschaeftsfall = g; }
    public Long getProdukt() { return produkt; }
    public void setProdukt(Long p) { this.produkt = p; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GeschaeftsfallProduktId that)) return false;
        return Objects.equals(geschaeftsfall, that.geschaeftsfall) &&
               Objects.equals(produkt, that.produkt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(geschaeftsfall, produkt);
    }
}
