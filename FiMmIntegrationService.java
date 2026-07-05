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
 * FiMmIntegrationService – Schnittstelle FI ↔ MM (Materials Management).
 *
 * Buchungslogik MM → FI:
 *   Wareneingang (MIGO):       Materialbestand (SOLL) / WE-RE-Verrechnung (HABEN)
 *   Rechnungsprüfung (MIRO):   WE-RE-Verrechnung (SOLL) / Verbindlichkeiten (HABEN)
 *   Zahlungsausgang (F-53):    Verbindlichkeiten (SOLL) / Bank (HABEN)
 *
 * Vereinfachung V1: Wareneingang wird direkt als Materialaufwand gebucht.
 *
 * ÄNDERUNG (Nebenbuch-Feature): bucheWareneingang() löst den Lieferanten
 * jetzt genau wie FiSdIntegrationService.findeKundeOderWirf() zwingend auf
 * (Fail-Fast statt stillem Fallback auf KONTO_VERBINDLICHKEITEN) UND
 * verankert ihn als echte FK am Geschäftsfall (gf.setLieferant(...)).
 * Ohne diesen Fix:
 *   1. wurde ein Tippfehler im Lieferantennamen unbemerkt mit dem globalen
 *      Standardkonto weitergebucht (exakt der Bug, der beim Kunden schon
 *      behoben wurde),
 *   2. ließ sich ein Einkaufsvorgang NIE einem konkreten Kreditor im
 *      Nebenbuch zuordnen (siehe NebenbuchService) – die
 *      Kreditoren-Abstimmung wäre technisch unmöglich gewesen.
 */
@Service
@Transactional
public class FiMmIntegrationService {

    private static final String DEFAULT_BUCHUNGSKREIS = "DE00";

    // SAP FI Kontonummern (Kontenplan GL00)
    // ÄNDERUNG (Kontenplan-Bereinigung): Kontonummern folgen jetzt der
    // SKR-Klassenlogik (siehe DataLoader-Javadoc). WICHTIG: vorher stand
    // hier KONTO_MATERIALAUFWAND = 280000 – dieses Konto wurde vom
    // DataLoader nie angelegt (dort hieß "Materialaufwand" 750006), wodurch
    // JEDER Wareneingang mit "Sollkonto nicht gefunden: 280000" fehlschlug
    // und nie eine Buchung/Rechnung entstand. Jetzt korrekt auf das
    // tatsächlich existierende Materialaufwand-Konto (Klasse 4-7) gemappt.
    private static final int KONTO_BANK              = 100000;  // Klasse 0-1
    private static final int KONTO_VERBINDLICHKEITEN = 300000;  // Klasse 3
    private static final int KONTO_MATERIALAUFWAND   = 400000;  // Klasse 4-7

    private final BuchungsService buchungsService;
    private final GeschaeftsfallRepository geschaeftsfallRepo;
    private final LieferantRepository lieferantRepo;
    private final ProduktRepository produktRepo;
    private final RechnungService rechnungService;
    private final BuchungskreisRepository buchungskreisRepo;

    public FiMmIntegrationService(BuchungsService buchungsService,
                                   GeschaeftsfallRepository geschaeftsfallRepo,
                                   LieferantRepository lieferantRepo,
                                   ProduktRepository produktRepo,
                                   RechnungService rechnungService,
                                   BuchungskreisRepository buchungskreisRepo) {
        this.buchungsService = buchungsService;
        this.geschaeftsfallRepo = geschaeftsfallRepo;
        this.lieferantRepo = lieferantRepo;
        this.produktRepo = produktRepo;
        this.rechnungService = rechnungService;
        this.buchungskreisRepo = buchungskreisRepo;
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

    /** Analog zu FiSdIntegrationService – lazy Default-Zuweisung für Legacy-GF. */
    private String sicherstellenBuchungskreis(Geschaeftsfall gf) {
        if (gf.getBuchungskreis() == null) {
            gf.setBuchungskreis(resolveBuchungskreis(null));
            geschaeftsfallRepo.save(gf);
        }
        return gf.getBuchungskreis().getBuchungskreisNr();
    }

    /**
     * ÄNDERUNG (Nebenbuch-Feature): löst einen Lieferantennamen zwingend auf
     * einen existierenden Kreditor auf – analog zu
     * FiSdIntegrationService.findeKundeOderWirf().
     */
    private Lieferant findeLieferantOderWirf(String lieferantenname) {
        return lieferantRepo.findByFirmennameContainingIgnoreCase(lieferantenname)
                .stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Lieferant nicht gefunden: \"" + lieferantenname + "\". " +
                        "Bitte exakten Firmennamen aus den Stammdaten verwenden oder den " +
                        "Lieferanten zuerst unter Stammdaten anlegen."));
    }

    // ── SCHRITT 1: Wareneingang buchen (MIGO) ────────────────────────────────

    public Map<String, Object> bucheWareneingang(Long geschaeftsfallId,
                                                   BigDecimal wareneingangswert,
                                                   String lieferantenname,
                                                   String erfasstVon) {

        Geschaeftsfall gf = geschaeftsfallRepo.findById(geschaeftsfallId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Geschäftsfall nicht gefunden: " + geschaeftsfallId));

        if (gf.getTyp() != Geschaeftsfall.GeschaeftsfallTyp.EINKAUF) {
            throw new IllegalArgumentException(
                    "Geschäftsfall ist kein Einkauf. Typ: " + gf.getTyp());
        }

        String bukrs = sicherstellenBuchungskreis(gf);

        // ÄNDERUNG: Lieferant ist jetzt Pflicht und wird am Geschäftsfall
        // verankert (falls noch nicht geschehen) – siehe Klassen-Javadoc.
        Lieferant lieferant = findeLieferantOderWirf(lieferantenname);
        if (gf.getLieferant() == null) {
            gf.setLieferant(lieferant);
        }

        String buchungstext = "MM-Wareneingang: " + lieferantenname +
                " | GF-" + geschaeftsfallId;

        int verbindlichkeitenKonto = lieferant.getAbstimmkonto() != null
                ? lieferant.getAbstimmkonto()
                : KONTO_VERBINDLICHKEITEN;

        Rechnung rechnung = rechnungService.rechnungErstellen(gf, wareneingangswert);

        List<Buchung> buchungen = buchungsService.bucheSollHaben(
                KONTO_MATERIALAUFWAND, verbindlichkeitenKonto,
                wareneingangswert, buchungstext,
                LocalDate.now(), erfasstVon, rechnung.getRechnungId(), bukrs);

        gf.setStatus(Geschaeftsfall.GeschaeftsfallStatus.IN_BEARBEITUNG);
        gf.setGesamtbetrag(wareneingangswert);
        geschaeftsfallRepo.save(gf);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("typ", "MM_WARENEINGANG");
        result.put("geschaeftsfallId", geschaeftsfallId);
        result.put("buchungskreis", bukrs);
        result.put("lieferant", lieferantenname);
        result.put("betrag", wareneingangswert);
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
        result.put("hinweis", "Materialaufwand SOLL / Verbindlichkeiten HABEN – MM-FI-Integration (Buchungskreis " +
                bukrs + "). Offener Posten (Rechnung " + rechnung.getRechnungId() + ", fällig " + rechnung.getFaelligkeit() + ") angelegt.");
        return result;
    }

    // ── SCHRITT 2: Zahlungsausgang buchen (F-53) ─────────────────────────────

    public Map<String, Object> bucheZahlungsausgang(Long geschaeftsfallId,
                                                     BigDecimal zahlungsbetrag,
                                                     String lieferantenname,
                                                     String erfasstVon) {

        Geschaeftsfall gf = geschaeftsfallRepo.findById(geschaeftsfallId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Geschäftsfall nicht gefunden: " + geschaeftsfallId));

        String bukrs = sicherstellenBuchungskreis(gf);

        Rechnung rechnung = rechnungService.findeOffenePostenFuerGeschaeftsfall(gf)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Keine offene Eingangsrechnung zu Geschäftsfall " + geschaeftsfallId + " gefunden. " +
                        "Zahlungsausgang kann nur zu einem bereits gebuchten Wareneingang verbucht werden."));

        rechnungService.rechnungAusgleichen(rechnung, zahlungsbetrag);

        String buchungstext = "Zahlungsausgang Kreditor: " + lieferantenname +
                " | GF-" + geschaeftsfallId;

        List<Buchung> buchungen = buchungsService.bucheSollHaben(
                KONTO_VERBINDLICHKEITEN, KONTO_BANK,
                zahlungsbetrag, buchungstext,
                LocalDate.now(), erfasstVon, rechnung.getRechnungId(), bukrs);

        gf.setStatus(Geschaeftsfall.GeschaeftsfallStatus.ABGESCHLOSSEN);
        geschaeftsfallRepo.save(gf);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("typ", "MM_ZAHLUNGSAUSGANG");
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
        result.put("hinweis", "Verbindlichkeiten SOLL / Bank HABEN – Kreditorenausgleich. Rechnung " +
                rechnung.getRechnungId() + " auf BEZAHLT gesetzt.");
        return result;
    }

    // ── VOLLSTÄNDIGER MM-PROZESS (simuliert) ─────────────────────────────────

    public Map<String, Object> vollstaendigerEinkaufsprozess(
            String lieferantenname, BigDecimal betrag, String erfasstVon, String buchungskreisNr) {

        // ÄNDERUNG: Lieferant wird VOR Anlage des Geschäftsfalls aufgelöst
        // (Fail-Fast) und direkt am Geschäftsfall verankert – analog zu
        // FiSdIntegrationService.vollstaendigerVerkaufsprozess().
        Lieferant lieferant = findeLieferantOderWirf(lieferantenname);
        Buchungskreis buchungskreis = resolveBuchungskreis(buchungskreisNr);

        Geschaeftsfall gf = new Geschaeftsfall(
                LocalDate.now(),
                Geschaeftsfall.GeschaeftsfallTyp.EINKAUF,
                null, null);
        gf.setBuchungskreis(buchungskreis);
        gf.setLieferant(lieferant);
        gf = geschaeftsfallRepo.save(gf);
        Long gfId = gf.getGeschaeftsfallId();

        List<Map<String, Object>> schritte = new ArrayList<>();

        schritte.add(bucheWareneingang(gfId, betrag, lieferantenname, erfasstVon));
        schritte.add(bucheZahlungsausgang(gfId, betrag, lieferantenname, erfasstVon));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("prozess", "MM_VOLLSTAENDIG");
        result.put("geschaeftsfallId", gfId);
        result.put("buchungskreis", buchungskreis.getBuchungskreisNr());
        result.put("lieferant", lieferantenname);
        result.put("gesamtbetrag", betrag);
        result.put("schritte", schritte);
        result.put("nettoeffekt", "Materialaufwand erhöht, Bankbestand reduziert, Verbindlichkeiten ausgeglichen");
        return result;
    }
}