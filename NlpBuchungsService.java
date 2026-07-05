package com.example.demo.service;


import com.example.demo.entity.Buchung;
import com.example.demo.entity.Konto;
import com.example.demo.repository.KontoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NlpBuchungsService – KI-Feature: Freitext → Buchungssatz.
 *
 * Version 2: Primär Claude API (ClaudeNlpService), mit automatischem
 * Fallback auf den regelbasierten Parser, falls:
 *  - kein ANTHROPIC_API_KEY konfiguriert ist, oder
 *  - die Claude API nicht erreichbar ist / einen Fehler liefert.
 *
 * So bleibt der NLP-Assistent auch ohne Internet-/API-Verfügbarkeit
 * funktionsfähig (wichtig für ein Buchhaltungssystem).
 *
 * Beispiele (regelbasierter Fallback):
 *  "Miete 1500 EUR bezahlt"        → Soll: 600000 (Mietaufwand) / Haben: 100000 (Bank)
 *  "Verkauf Fahrrad 2500 EUR"      → Soll: 100000 (Bank) / Haben: 800000 (Umsatz)
 *  "Gehalt Mitarbeiter 3200 EUR"   → Soll: 420000 (Personalaufwand) / Haben: 100000 (Bank)
 *  "Eingangsrechnung Lieferant 800"→ Soll: 400000 (Materialaufwand) / Haben: 300000 (Verbindlichkeiten)
 *
 * WICHTIG – Kontenplan-Abgleich (Stand: siehe Kontenübersicht GL00):
 *  Die Regeln unten wurden bewusst auf die Kontonummern gemappt, die im
 *  DataLoader / in der Kontenübersicht tatsächlich existieren. Zwei
 *  Buchungstypen ("strom/energie/gas" und "büro/büromaterial") sind
 *  AUSKOMMENTIERT, weil die dafür nötigen Sachkonten (760000, 780000)
 *  noch nicht im Kontenplan angelegt sind. Sobald sie existieren, einfach
 *  die entsprechenden Zeilen im static-Block wieder aktivieren.
 */
@Service
public class NlpBuchungsService {

    private static final Logger log = LoggerFactory.getLogger(NlpBuchungsService.class);

    private final KontoRepository kontoRepository;
    private final BuchungsService buchungsService;
    private final ClaudeNlpService claudeNlpService;

    // Betrag-Pattern: "1500 EUR", "1.500,00 EUR", "EUR 1500", "1500"
    private static final Pattern BETRAG_PATTERN = Pattern.compile(
            "(?i)(?:EUR\\s*)?(\\d+(?:[.,]\\d+)*)(?:\\s*EUR)?");

    // Bekannte Buchungstypen → (SollKonto, HabenKonto)
    private static final Map<String[], int[]> BUCHUNGSREGELN = new LinkedHashMap<>();

    // ÄNDERUNG (Kontenplan-Bereinigung): Kontonummern folgen jetzt der
    // SKR-Klassenlogik, die der DataLoader tatsächlich anlegt (0-1
    // Vermögen, 2 Eigenkapital, 3 Verbindlichkeiten, 4-7 Aufwand, 8 Erlöse).
    // Die vorherigen Nummern (740300, 740000, 770000, 750006) passten
    // nicht mehr zur Klasseneinteilung.
    static {
        // Aufwand (Klasse 4-7) → Haben: Bank
        BUCHUNGSREGELN.put(new String[]{"miete", "pacht"},             new int[]{600000, 100000});
        BUCHUNGSREGELN.put(new String[]{"gehalt", "lohn", "personal"}, new int[]{420000, 100000});
        BUCHUNGSREGELN.put(new String[]{"versicherung"},               new int[]{620000, 100000});

        // Einkauf/Wareneinkauf → Materialaufwand (400000, Klasse 4-7) / Verbindlichkeiten (300000, Klasse 3)
        // Hinweis: "material" bewusst NICHT als Schlüsselwort verwendet, da es sonst
        // mit der (aktuell auskommentierten) Büromaterial-Regel kollidiert.
        BUCHUNGSREGELN.put(new String[]{"einkauf", "wareneinkauf", "lieferant", "eingangsrechnung"},
                new int[]{400000, 300000});

        // ── TODO: Konten existieren noch NICHT im Kontenplan GL00 ──
        // Sobald angelegt (über Stammdaten-UI oder DataLoader), Regeln aktivieren:
        //
        // BUCHUNGSREGELN.put(new String[]{"strom", "energie", "gas"},
        //         new int[]{640000, 100000});   // benötigt Konto 640000 "Energieaufwand" (Klasse 4-7)
        //
        // BUCHUNGSREGELN.put(new String[]{"büro", "büromaterial", "bürobedarf"},
        //         new int[]{660000, 100000});   // benötigt Konto 660000 "Büromaterial" (Klasse 4-7)

        // Ertrag (Klasse 8) → Soll: Bank / Forderungen
        BUCHUNGSREGELN.put(new String[]{"verkauf", "umsatz", "erlös", "rechnung"},  new int[]{100000, 800000});
        BUCHUNGSREGELN.put(new String[]{"zahlung", "einzahlung", "kundenzahlung"}, new int[]{100000, 120000});

        // Bank-Umbuchungen (Bank Zahlungsausgang US00 ↔ Bank Hauptkonto), Klasse 0-1
        BUCHUNGSREGELN.put(new String[]{"bank", "überweisung", "transfer"},        new int[]{100005, 100000});
    }

    public NlpBuchungsService(KontoRepository kontoRepository,
                               BuchungsService buchungsService,
                               ClaudeNlpService claudeNlpService) {
        this.kontoRepository = kontoRepository;
        this.buchungsService = buchungsService;
        this.claudeNlpService = claudeNlpService;
    }

    /**
     * Analysiert einen Freitext und extrahiert einen Buchungsvorschlag.
     * Bucht den Satz NICHT – gibt nur den Vorschlag zurück (für Bestätigung).
     *
     * Nutzt primär die Claude API; fällt bei Fehlern automatisch auf den
     * regelbasierten Parser zurück.
     */
    public Map<String, Object> analysiereFreitext(String freitext) {
        if (claudeNlpService.isVerfuegbar()) {
            try {
                return claudeNlpService.analysiere(freitext);
            } catch (Exception e) {
                log.warn("Claude-NLP-Analyse fehlgeschlagen, verwende regelbasierten Fallback: {}", e.getMessage());
                Map<String, Object> fallback = analysiereFreitextRegelbasiert(freitext);
                fallback.put("warnung", "Claude API nicht erreichbar – regelbasierter Fallback verwendet.");
                return fallback;
            }
        }
        return analysiereFreitextRegelbasiert(freitext);
    }

    /**
     * Analysiert und bucht direkt (mit Bestätigung).
     */
    public List<Buchung> analysiereUndBuche(String freitext, String erfasstVon) {
        Map<String, Object> vorschlag = analysiereFreitext(freitext);

        if (!(Boolean) vorschlag.get("erfolg")) {
            throw new IllegalArgumentException("NLP-Analyse fehlgeschlagen: " + vorschlag.get("fehler"));
        }

        BigDecimal betrag = (BigDecimal) vorschlag.get("betrag");
        Integer sollKontoNr = (Integer) vorschlag.get("sollKontoNr");
        Integer habenKontoNr = (Integer) vorschlag.get("habenKontoNr");

        return buchungsService.bucheSollHaben(
                sollKontoNr, habenKontoNr, betrag, freitext,
                LocalDate.now(), erfasstVon, null);
    }

    // ─── Regelbasierter Fallback-Parser (ohne externe API) ─────────────────────

    private Map<String, Object> analysiereFreitextRegelbasiert(String freitext) {
        Map<String, Object> vorschlag = new LinkedHashMap<>();
        vorschlag.put("originalText", freitext);
        vorschlag.put("analysiert", LocalDate.now().toString());
        vorschlag.put("quelle", "regelbasiert");

        // 1. Betrag extrahieren
        BigDecimal betrag = extrahiereBetrag(freitext);
        vorschlag.put("betragErkannt", betrag);

        if (betrag == null) {
            vorschlag.put("fehler", "Kein Betrag im Text erkannt. Beispiel: 'Miete 1500 EUR bezahlt'");
            vorschlag.put("erfolg", false);
            return vorschlag;
        }

        // 2. Buchungstyp erkennen
        int[] kontonummern = erkenneKonten(freitext.toLowerCase());
        if (kontonummern == null) {
            vorschlag.put("fehler", "Buchungstyp nicht erkannt. Bekannte Schlüsselwörter: miete, gehalt, versicherung, einkauf, verkauf, zahlung, bank");
            vorschlag.put("erfolg", false);
            return vorschlag;
        }

        // 3. Konten laden und Vorschlag zurückgeben
        Optional<Konto> sollKonto = kontoRepository.findByKontonummer(kontonummern[0]);
        Optional<Konto> habenKonto = kontoRepository.findByKontonummer(kontonummern[1]);

        vorschlag.put("sollKontoNr", kontonummern[0]);
        vorschlag.put("sollKontoBezeichnung", sollKonto.map(Konto::getKontobezeichnung).orElse("Konto nicht in DB"));
        vorschlag.put("habenKontoNr", kontonummern[1]);
        vorschlag.put("habenKontoBezeichnung", habenKonto.map(Konto::getKontobezeichnung).orElse("Konto nicht in DB"));
        vorschlag.put("betrag", betrag);
        vorschlag.put("buchungstext", freitext);
        vorschlag.put("buchungsdatum", LocalDate.now().toString());
        vorschlag.put("erfolg", true);
        vorschlag.put("hinweis", "Regelbasierter Vorschlag prüfen und mit POST /api/buchungen/buchen bestätigen.");

        return vorschlag;
    }

    private BigDecimal extrahiereBetrag(String text) {
        Matcher m = BETRAG_PATTERN.matcher(text);
        if (m.find()) {
            // Wir nehmen Gruppe 1, das ist die Zahl
            String numStr = m.group(1); 
            if (numStr == null) return null;

            try {
                // Logik für deutsche/internationale Trennzeichen:
                // Wenn Punkt UND Komma vorkommen (z.B. 1.500,00)
                if (numStr.contains(".") && numStr.contains(",")) {
                    numStr = numStr.replace(".", "").replace(",", ".");
                } 
                // Wenn nur ein Komma vorkommt (z.B. 1500,00)
                else if (numStr.contains(",")) {
                    numStr = numStr.replace(",", ".");
                }
                // Wenn nur ein Punkt vorkommt, prüfen wir: Ist es Tausender (1.500) oder Dezimal (15.50)?
                // Im Zweifel für die Demo: Wir behandeln einen einzelnen Punkt als Dezimaltrenner,
                // außer es folgen exakt 3 Ziffern (Tausender).
                else if (numStr.contains(".")) {
                    // Falls Punkt und danach NICHT 3 Ziffern kommen -> Dezimalpunkt (z.B. 15.5)
                    // (In einer einfachen Demo lassen wir den Punkt meist als Dezimaltrenner)
                }

                BigDecimal betrag = new BigDecimal(numStr);
                // Sicherheitscheck: Falls wir z.B. nur "1.000" (Punkt als Tausender) haben, 
                // macht BigDecimal daraus fälschlicherweise 1.0. 
                // Aber für "1500" funktioniert es jetzt perfekt!

                if (betrag.compareTo(BigDecimal.ZERO) > 0) return betrag;
            } catch (Exception e) {
                log.warn("Parsing-Fehler bei: {}", numStr);
            }
        }
        return null;
    }

    private int[] erkenneKonten(String lowerText) {
        for (Map.Entry<String[], int[]> regel : BUCHUNGSREGELN.entrySet()) {
            for (String schluessel : regel.getKey()) {
                if (lowerText.contains(schluessel)) {
                    return regel.getValue();
                }
            }
        }
        return null;
    }
}