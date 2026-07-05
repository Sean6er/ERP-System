package com.example.demo.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Mitarbeiter – interner Akteur (Buchhalter, Einkauf, Vertrieb). */
@Entity
@Table(name = "MITARBEITER")
public class Mitarbeiter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mitarbeiter_id")
    private Long mitarbeiterId;

    @NotBlank
    @Column(name = "vorname", nullable = false, length = 50)
    private String vorname;

    @NotBlank
    @Column(name = "nachname", nullable = false, length = 50)
    private String nachname;

    @Column(name = "rolle", length = 100)
    private String rolle;   // z.B. "Buchhalter", "Einkauf", "Vertrieb"

    @Email
    @Column(name = "email", length = 100)
    private String email;

    // ─── Constructors ─────────────────────────────────────────────
    public Mitarbeiter() {}

    public Mitarbeiter(String vorname, String nachname, String rolle, String email) {
        this.vorname = vorname;
        this.nachname = nachname;
        this.rolle = rolle;
        this.email = email;
    }

    // ─── Getters / Setters ────────────────────────────────────────
    public Long getMitarbeiterId()                         { return mitarbeiterId; }
    public void setMitarbeiterId(Long mitarbeiterId)       { this.mitarbeiterId = mitarbeiterId; }
    public String getVorname()                             { return vorname; }
    public void setVorname(String vorname)                 { this.vorname = vorname; }
    public String getNachname()                            { return nachname; }
    public void setNachname(String nachname)               { this.nachname = nachname; }
    public String getRolle()                               { return rolle; }
    public void setRolle(String rolle)                     { this.rolle = rolle; }
    public String getEmail()                               { return email; }
    public void setEmail(String email)                     { this.email = email; }
    public String getFullName()                            { return vorname + " " + nachname; }

    @Override
    public String toString() {
        return "Mitarbeiter{" + mitarbeiterId + " – " + vorname + " " + nachname + " [" + rolle + "]}";
    }
}
