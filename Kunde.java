package com.example.demo.entity;


import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

/**
 * Kunde – Debitor (Accounts Receivable). Aus dem ERM: Kunde-Tabelle.
 *
 * ÄNDERUNG: Verknüpfung zu Kreditkontrollbereich ergänzt. Vorher gab es
 * keinerlei fachliche Verbindung zwischen Kunde und KKB – ein Kreditlimit
 * konnte also nie einem konkreten Debitor zugeordnet werden.
 *
 * BUGFIX: @JsonIgnoreProperties("kunde") auf geschaeftsfaelle ergänzt.
 * Ohne diese Annotation lief die JSON-Serialisierung in eine Endlosschleife,
 * sobald ein Kunde mindestens einen Geschäftsfall hatte (z.B. nach einer
 * SD-Faktura über /api/integration/sd/vollstaendig):
 *   Kunde -> geschaeftsfaelle -> Geschaeftsfall -> kunde -> geschaeftsfaelle -> ...
 * Das führte zu einem abgeschnittenen/ungültigen JSON-Response beim Aufruf
 * von GET /api/stammdaten/kunden ("... is not valid JSON" im Frontend).
 */
@Entity
@Table(name = "KUNDE")
public class Kunde {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "kunden_id")
    private Long kundenId;

    @NotBlank
    @Column(name = "firmenname", nullable = false, length = 100)
    private String firmenname;

    @Column(name = "ansprechpartner", length = 100)
    private String ansprechpartner;

    @Email
    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "adresse", length = 80)
    private String adresse;

    @Column(name = "abstimmkonto")
    private Integer abstimmkonto = 120000;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kkb_id")
    private Kreditkontrollbereich kreditkontrollbereich;

    @JsonIgnoreProperties("kunde")
    @OneToMany(mappedBy = "kunde", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Geschaeftsfall> geschaeftsfaelle = new ArrayList<>();

    // ─── Constructors ─────────────────────────────────────────────
    public Kunde() {}

    public Kunde(String firmenname, String ansprechpartner, String email, String adresse) {
        this.firmenname = firmenname;
        this.ansprechpartner = ansprechpartner;
        this.email = email;
        this.adresse = adresse;
    }

    // ─── Getters / Setters ────────────────────────────────────────
    public Long getKundenId()                              { return kundenId; }
    public void setKundenId(Long kundenId)                 { this.kundenId = kundenId; }
    public String getFirmenname()                          { return firmenname; }
    public void setFirmenname(String firmenname)           { this.firmenname = firmenname; }
    public String getAnsprechpartner()                     { return ansprechpartner; }
    public void setAnsprechpartner(String ansprechpartner) { this.ansprechpartner = ansprechpartner; }
    public String getEmail()                               { return email; }
    public void setEmail(String email)                     { this.email = email; }
    public String getAdresse()                             { return adresse; }
    public void setAdresse(String adresse)                 { this.adresse = adresse; }
    public List<Geschaeftsfall> getGeschaeftsfaelle()      { return geschaeftsfaelle; }
    public void setGeschaeftsfaelle(List<Geschaeftsfall> g){ this.geschaeftsfaelle = g; }
    public Integer getAbstimmkonto() { return abstimmkonto; }
    public void setAbstimmkonto(Integer abstimmkonto) { this.abstimmkonto = abstimmkonto; }
    public Kreditkontrollbereich getKreditkontrollbereich() { return kreditkontrollbereich; }
    public void setKreditkontrollbereich(Kreditkontrollbereich kkb) { this.kreditkontrollbereich = kkb; }

    @Override
    public String toString() {
        return "Kunde{" + kundenId + " – " + firmenname + "}";
    }
}