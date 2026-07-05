package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Request-Body für POST /api/stammdaten/kunden */
public class KundeRequest {

    @NotBlank(message = "Firmenname ist Pflichtfeld")
    private String firmenname;

    private String ansprechpartner;

    @Email
    private String email;

    private String adresse;

    private Integer abstimmkonto = 120000;

    /** Optional: verknüpft den Kunden mit einem Kreditkontrollbereich (Kreditlimit-Prüfung). */
    private Long kreditkontrollbereichId;

    public String getFirmenname() { return firmenname; }
    public void setFirmenname(String firmenname) { this.firmenname = firmenname; }
    public String getAnsprechpartner() { return ansprechpartner; }
    public void setAnsprechpartner(String ansprechpartner) { this.ansprechpartner = ansprechpartner; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }
    public Integer getAbstimmkonto() { return abstimmkonto; }
    public void setAbstimmkonto(Integer abstimmkonto) { this.abstimmkonto = abstimmkonto; }
    public Long getKreditkontrollbereichId() { return kreditkontrollbereichId; }
    public void setKreditkontrollbereichId(Long kreditkontrollbereichId) { this.kreditkontrollbereichId = kreditkontrollbereichId; }
}
