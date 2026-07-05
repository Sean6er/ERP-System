package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * SdBuchungRequest – generisches Request-DTO für FI↔SD und FI↔MM Integration.
 *
 * geschaeftsfallId ist optional: wird bei "/vollstaendig"-Endpunkten ignoriert,
 * da dort automatisch ein neuer Geschäftsfall angelegt wird.
 *
 * ÄNDERUNG: buchungskreisNr ergänzt – wird ausschließlich von den
 * "/vollstaendig"-Endpunkten ausgewertet, um den Buchungskreis des neu
 * angelegten Geschäftsfalls zu bestimmen (Default DE00, wenn leer). Bei den
 * Einzelbuchungs-Endpunkten (Faktura, Zahlungseingang, Wareneingang,
 * Zahlungsausgang) wird dieses Feld ignoriert, da der Buchungskreis dort
 * bereits über den existierenden Geschäftsfall vorgegeben ist.
 */
public class SdBuchungRequest {

    private Long geschaeftsfallId;

    @NotNull(message = "Betrag ist Pflichtfeld")
    @Positive(message = "Betrag muss positiv sein")
    private BigDecimal betrag;

    @NotBlank(message = "Partnername (Kunde/Lieferant) ist Pflichtfeld")
    private String partnername;

    private String erfasstVon = "Integration";

    private String buchungskreisNr;

    public Long getGeschaeftsfallId() { return geschaeftsfallId; }
    public void setGeschaeftsfallId(Long geschaeftsfallId) { this.geschaeftsfallId = geschaeftsfallId; }

    public BigDecimal getBetrag() { return betrag; }
    public void setBetrag(BigDecimal betrag) { this.betrag = betrag; }

    public String getPartnername() { return partnername; }
    public void setPartnername(String partnername) { this.partnername = partnername; }

    public String getErfasstVon() { return erfasstVon; }
    public void setErfasstVon(String erfasstVon) { this.erfasstVon = erfasstVon; }

    public String getBuchungskreisNr() { return buchungskreisNr; }
    public void setBuchungskreisNr(String buchungskreisNr) { this.buchungskreisNr = buchungskreisNr; }
}