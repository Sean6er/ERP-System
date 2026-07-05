package com.example.demo.entity;


import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

/**
 * Lieferant – Kreditor (Accounts Payable). Aus dem ERM: Lieferant-Tabelle.
 *
 * BUGFIX: @JsonIgnoreProperties("lieferant") auf produkte ergänzt. Sonst
 * entsteht dieselbe Art von Endlosschleife wie bei Kunde/Geschaeftsfall:
 *   Lieferant -> produkte -> Produkt -> lieferant -> produkte -> ...
 * Aktuell noch nicht akut ausgelöst (GeschaeftsfallProdukt wird derzeit
 * nirgends befüllt), aber latent vorhanden und über
 * Geschaeftsfall.positionen -> GeschaeftsfallProdukt -> produkt -> lieferant
 * erreichbar, sobald dort mal Positionen angelegt werden.
 */
@Entity
@Table(name = "LIEFERANT")
public class Lieferant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lieferanten_id")
    private Long lieferantenId;

    @NotBlank
    @Column(name = "firmenname", nullable = false, length = 100)
    private String firmenname;

    @Email
    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "telefon", length = 30)
    private String telefon;

    @Column(name = "adresse", length = 100)
    private String adresse;
    
    // ÄNDERUNG (Kontenplan-Bereinigung): Default-Abstimmkonto jetzt 300000
    // ("Verbindlichkeiten aLuL", Klasse 3) statt 400000 – 400000 ist nach
    // der Klassenbereinigung das Materialaufwand-Konto (Klasse 4-7) und
    // darf kein Abstimmkonto mehr sein (falscher Kontotyp: AUFWAND statt PASSIV).
    @Column(name = "abstimmkonto")
    private Integer abstimmkonto = 300000;

    @JsonIgnoreProperties("lieferant")
    @OneToMany(mappedBy = "lieferant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Produkt> produkte = new ArrayList<>();

    // ─── Constructors ─────────────────────────────────────────────
    public Lieferant() {}

    public Lieferant(String firmenname, String email, String telefon, String adresse) {
        this.firmenname = firmenname;
        this.email = email;
        this.telefon = telefon;
        this.adresse = adresse;
    }

    // ─── Getters / Setters ────────────────────────────────────────
    public Long getLieferantenId()                         { return lieferantenId; }
    public void setLieferantenId(Long lieferantenId)       { this.lieferantenId = lieferantenId; }
    public String getFirmenname()                          { return firmenname; }
    public void setFirmenname(String firmenname)           { this.firmenname = firmenname; }
    public String getEmail()                               { return email; }
    public void setEmail(String email)                     { this.email = email; }
    public String getTelefon()                             { return telefon; }
    public void setTelefon(String telefon)                 { this.telefon = telefon; }
    public String getAdresse()                             { return adresse; }
    public void setAdresse(String adresse)                 { this.adresse = adresse; }
    public List<Produkt> getProdukte()                     { return produkte; }
    public void setProdukte(List<Produkt> produkte)        { this.produkte = produkte; }
    public Integer getAbstimmkonto() { return abstimmkonto; }
    public void setAbstimmkonto(Integer abstimmkonto) { this.abstimmkonto = abstimmkonto; }

    @Override
    public String toString() {
        return "Lieferant{" + lieferantenId + " – " + firmenname + "}";
    }
}