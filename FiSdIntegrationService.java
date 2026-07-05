package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * FiSdIntegrationService – Schnittstelle FI ↔ SD (Sales & Distribution).
 *
 * ÄNDERUNG: Vor dem Buchen einer Faktura wird jetzt tatsächlich das
 * Kreditlimit des Kunden geprüft (sofern der Kunde einem
 * Kreditkontrollbereich zugeordnet ist). Vorher passierte hier nichts.
 *
 * BUGFIX: bucheFaktura() wirft jetzt eine IllegalArgumentException, wenn
 * der übergebene Partnername keinem existierenden Kunden in den
 * Stammdaten zugeordnet werden kann. Vorher wurde in diesem Fall einfach
 * mit kunde=null weitergebucht – KreditkontrollService.pruefeKreditlimit()
 * überspringt die Prüfung bei kunde=null komplett (return;), wodurch ein
 * simpler Tippfehler im Namen die komplette Kreditlimit-Sperre unbemerkt
 * außer Kraft gesetzt hat.
 *
 * ÄNDERUNG: Der Geschäftsfall bekommt jetzt beim Anlegen (vollstaendiger-
 * VerkaufsprozessS) einen echten Buchungskreis zugewiesen (Default DE00,
 * per Parameter überschreibbar) – analog zu SAP, wo der Buchungskreis Teil
 * der Vertriebsbelegkopf-Daten ist. Dieser Buchungskreis vererbt sich auf
 * ALLE FI-Buchungen dieses Vorgangs (Faktura + Zahlungseingang), damit
 * Reporting und Rechnungslisten den kompletten Vorgang konsistent einem
 * Buchungskreis zuordnen können. Bereits existierende Geschäftsfälle ohne
 * Buchungskreis (z.B. über GeschaeftsfallService angelegt) werden beim
 * ersten Buchungsschritt lazy auf den Default DE00 gesetzt.
 */
@Service
@Transactional
public class FiSdIntegrationService {

    private static final String DEFAULT_BUCHUNGSKREIS = "DE00";

    // SAP FI Kontonummern (Kontenplan GL00)
    // ÄNDERUNG (Kontenplan-Bereinigung): Kontonummern folgen jetzt der
    // SKR-Klassenlogik (siehe DataLoader-Javadoc). KONTO_MATERIALAUFWAND
    // wurde entfernt – die Konstante war hier tot (nirgends referenziert,
    // vermutlich Copy-Paste-Rest aus FiMmIntegrationService) und stand
    // zusätzlich auf einer nie angelegten Kontonummer (280000).
    private static final int KONTO_BANK           = 100000;  // Klasse 0-1
    private static final int KONTO_FORDERUNGEN    = 120000;  // Klasse 0-1
    private static final int KONTO_UMSATZERLOESE  = 800000;  // Klasse 8

    private final BuchungsService buchungsService;
    private final GeschaeftsfallRepository geschaeftsfallRepo;
    private final KundeRepository kundeRepo;
    private final RechnungRepository rechnungRepo;
    private final KreditkontrollService kreditkontrollService;
    private final RechnungService rechnungService;
    private final BuchungskreisRepository buchungskreisRepo;

    public FiSdIntegrationService(BuchungsService buchungsService,
                                   GeschaeftsfallRepository geschaeftsfallRepo,
                                   KundeRepository kundeRepo,
                                   RechnungRepository rechnungRepo,
                                   KreditkontrollService kreditkontrollService,
                                   RechnungService rechnungService,
                                   BuchungskreisRepository buchungskreisRepo) {
        this.buchungsService = buchungsService;
        this.geschaeftsfallRepo = geschaeftsfallRepo;
        this.kundeRepo = kundeRepo;
        this.rechnungRepo = rechnungRepo;
        this.kreditkontrollService = kreditkontrollService;
        this.rechnungService = rechnungService;
        this.buchungskreisRepo = buchungskreisRepo;
    }

    /**
     * Löst einen Partnernamen zwingend auf einen existierenden Kunden auf.
     * Wirft eine sprechende Exception statt still mit kunde=null weiterzumachen –
     * sonst wird die Kreditlimit-Prüfung faktisch nie ausgelöst.
     */
    private Kunde findeKundeOderWirf(String kundenname) {
        return kundeRepo.findByFirmennameContainingIgnoreCase(kundenname)
                .stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Kunde nicht gefunden: \"" + kundenname + "\". " +
                        "Bitte exakten Firmennamen aus den Stammdaten verwenden " +
                        "(nicht den Ansprechpartner) oder den Kunden zuerst unter " +
                        "Stammdaten anlegen."));
    }

    /** Löst eine Buchungskreis-Nr auf; bei null/leer wird DE00 verwendet. */
    private Buchungskreis resolveBuchungskreis(String buchungskreisNr) {
        String zielNr = (buchungskreisNr == null || buchungskreisNr.isBlank())
                ? DEFAULT_BUCHUNGSKREIS : buchungskreisNr;
        return buchungskreisRepo.findByBuchungskreisNr(zielNr)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Buchungskreis nicht gefunden: " + zielNr +
                        " (bitte zuerst unter Organisationsstruktur anlegen)"));
    }

    /**
     * Stellt sicher, dass ein Geschäftsfall einen Buchungskreis trägt, und
     * liefert dessen Nr zur Weitergabe an BuchungsService.bucheSollHaben().
     * Legacy-Geschäftsfälle ohne Buchungskreis (vor dieser Änderung angelegt)
     * werden lazy auf DE00 gesetzt statt die Buchung abzulehnen.
     */
    private String sicherstellenBuchungskreis(Geschaeftsfall gf) {
        if (gf.getBuchungskreis() == null) {
            gf.setBuchungskreis(resolveBuchungskreis(null));
            geschaeftsfallRepo.save(gf);
        }
        return gf.getBuchungskreis().getBuchungskreisNr();
    }

    // ── SCHRITT 1: Faktura buchen (SD-Faktura → FI-Debitoren) ────────────────

    /**
     * Bucht eine Ausgangsrechnung aus dem SD-Modul in die FI.
     * Entspricht der automatischen Kontierung bei SD-Faktura-Erstellung.
     *
     * Buchungssatz: Forderungen (SOLL) / Umsatzerlöse (HABEN)
     */
    public Map<String, Object> bucheFaktura(Long geschaeftsfallId,
                                             BigDecimal rechnungsbetrag,
                                             String kundenname,
                                             String erfasstVon) {

        Geschaeftsfall gf = geschaeftsfallRepo.findById(geschaeftsfallId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Geschäftsfall nicht gefunden: " + geschaeftsfallId));

        if (gf.getTyp() != Geschaeftsfall.GeschaeftsfallTyp.VERKAUF) {
            throw new IllegalArgumentException(
                    "Geschäftsfall ist kein Verkauf. Typ: " + gf.getTyp());
        }

        // ÄNDERUNG: Buchungskreis des Geschäftsfalls wird an die FI-Buchung
        // weitergegeben, statt implizit im globalen Default zu landen.
        String bukrs = sicherstellenBuchungskreis(gf);

        String buchungstext = "SD-Faktura: " + kundenname +
                " | GF-" + geschaeftsfallId;

        // ÄNDERUNG: Kunde ist jetzt Pflicht – kein stiller Fallback auf kunde=null mehr.
        Kunde kunde = findeKundeOderWirf(kundenname);

        int forderungenKonto = kunde.getAbstimmkonto() != null
                ? kunde.getAbstimmkonto()
                : KONTO_FORDERUNGEN;

        // Kreditlimit-Prüfung – wirft KreditlimitUeberschrittenException,
        // wenn der Kunde einem Kreditkontrollbereich zugeordnet ist und das Limit
        // (offene Forderungen + diese Rechnung) überschritten würde.
        kreditkontrollService.pruefeKreditlimit(kunde, rechnungsbetrag);

        Rechnung rechnung = rechnungService.rechnungErstellen(gf, rechnungsbetrag);

        List<Buchung> buchungen = buchungsService.bucheSollHaben(
                forderungenKonto, KONTO_UMSATZERLOESE,
                rechnungsbetrag, buchungstext,
                LocalDate.now(), erfasstVon, rechnung.getRechnungId(), bukrs);

        // Geschäftsfall auf IN_BEARBEITUNG setzen
        gf.setStatus(Geschaeftsfall.GeschaeftsfallStatus.IN_BEARBEITUNG);
        gf.setGesamtbetrag(rechnungsbetrag);
        gf.setKunde(kunde);
        geschaeftsfallRepo.save(gf);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("typ", "SD_FAKTURA");
        result.put("geschaeftsfallId", geschaeftsfallId);
        result.put("buchungskreis", bukrs);
        result.put("kunde", kundenname);
        result.put("betrag", rechnungsbetrag);
        result.put("rechnungId", rechnung.getRechnungId());
        result.put("rechnungStatus", rechnung.getStatus());
        result.put("faelligkeit", rechnung.getFaelligkeit());
        result.put("buchungen", buchungen.stream().map(b -> Map.of(
                "belegnummer", b.getBelegnummer(),
                "sollHaben", b.getSollHaben(),
                "konto", b.getKonto().getKontonummer() + " " + b.getKonto().getKontobezeichnung()
        )).toList());
        result.put("buchungstext", buchungstext);
        result.put("status", "GEBUCHT");
        result.put("hinweis", "Forderungen SOLL / Umsatzerlöse HABEN – SD-FI-Integration (Buchungskreis " +
                bukrs + "). Kreditlimit geprüft.");
        return result;
    }

    // ── SCHRITT 2: Zahlungseingang buchen ────────────────────────────────────

    /**
     * Bucht den Zahlungseingang eines Kunden (Debitorenzahlung).
     * Entspricht SAP-Transaktion F-28.
     *
     * Buchungssatz: Bank (SOLL) / Forderungen (HABEN)
     */
    public Map<String, Object> bucheZahlungseingang(Long geschaeftsfallId,
                                                     BigDecimal zahlungsbetrag,
                                                     String kundenname,
                                                     String erfasstVon) {

        Geschaeftsfall gf = geschaeftsfallRepo.findById(geschaeftsfallId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Geschäftsfall nicht gefunden: " + geschaeftsfallId));

        // ÄNDERUNG: Buchungskreis-Vererbung – der Zahlungseingang bucht im
        // selben Buchungskreis wie die zugehörige Faktura.
        String bukrs = sicherstellenBuchungskreis(gf);

        Rechnung rechnung = rechnungService.findeOffenePostenFuerGeschaeftsfall(gf)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Keine offene Rechnung zu Geschäftsfall " + geschaeftsfallId + " gefunden. " +
                        "Zahlungseingang kann nur zu einer bereits gebuchten Faktura verbucht werden."));

        rechnungService.rechnungAusgleichen(rechnung, zahlungsbetrag);

        String buchungstext = "Zahlungseingang Debitor: " + kundenname +
                " | GF-" + geschaeftsfallId;

        List<Buchung> buchungen = buchungsService.bucheSollHaben(
                KONTO_BANK, KONTO_FORDERUNGEN,
                zahlungsbetrag, buchungstext,
                LocalDate.now(), erfasstVon, rechnung.getRechnungId(), bukrs);

        // Geschäftsfall abschließen
        gf.setStatus(Geschaeftsfall.GeschaeftsfallStatus.ABGESCHLOSSEN);
        geschaeftsfallRepo.save(gf);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("typ", "SD_ZAHLUNGSEINGANG");
        result.put("geschaeftsfallId", geschaeftsfallId);
        result.put("buchungskreis", bukrs);
        result.put("zahlungsbetrag", zahlungsbetrag);
        result.put("rechnungId", rechnung.getRechnungId());
        result.put("rechnungStatus", rechnung.getStatus());
        result.put("buchungen", buchungen.stream().map(b -> Map.of(
                "belegnummer", b.getBelegnummer(),
                "sollHaben", b.getSollHaben(),
                "konto", b.getKonto().getKontonummer() + " " + b.getKonto().getKontobezeichnung()
        )).toList());
        result.put("status", "ABGESCHLOSSEN");
        result.put("hinweis", "Bank SOLL / Forderungen HABEN – Debitorenausgleich. Rechnung " +
                rechnung.getRechnungId() + " auf BEZAHLT gesetzt.");
        return result;
    }

    // ── VOLLSTÄNDIGER SD-PROZESS (simuliert) ─────────────────────────────────

    /**
     * Simuliert den kompletten SD→FI Prozess in einem Schritt:
     * 1. Faktura buchen (Forderungen / Umsatz)
     * 2. Zahlungseingang buchen (Bank / Forderungen)
     *
     * ÄNDERUNG: nimmt jetzt zusätzlich eine Buchungskreis-Nr entgegen (Default
     * DE00, wenn null/leer), die dem neu angelegten Geschäftsfall zugewiesen
     * wird und sich auf beide Buchungsschritte vererbt.
     */
    public Map<String, Object> vollstaendigerVerkaufsprozess(
            String kundenname, BigDecimal betrag, String erfasstVon, String buchungskreisNr) {

        Kunde kunde = findeKundeOderWirf(kundenname);
        Buchungskreis buchungskreis = resolveBuchungskreis(buchungskreisNr);

        // Neuen Geschäftsfall anlegen – MIT Buchungskreis
        Geschaeftsfall gf = new Geschaeftsfall(
                LocalDate.now(),
                Geschaeftsfall.GeschaeftsfallTyp.VERKAUF,
                kunde, null);
        gf.setBuchungskreis(buchungskreis);
        gf = geschaeftsfallRepo.save(gf);
        Long gfId = gf.getGeschaeftsfallId();

        List<Map<String, Object>> schritte = new ArrayList<>();

        // Schritt 1: Faktura
        schritte.add(bucheFaktura(gfId, betrag, kundenname, erfasstVon));
        // Schritt 2: Zahlungseingang
        schritte.add(bucheZahlungseingang(gfId, betrag, kundenname, erfasstVon));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("prozess", "SD_VOLLSTAENDIG");
        result.put("geschaeftsfallId", gfId);
        result.put("buchungskreis", buchungskreis.getBuchungskreisNr());
        result.put("kunde", kundenname);
        result.put("gesamtbetrag", betrag);
        result.put("schritte", schritte);
        result.put("nettoeffekt", "Umsatzerlöse erhöht, Bankbestand erhöht, Forderungen ausgeglichen");
        return result;
    }
}