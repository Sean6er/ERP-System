package com.example.demo.service;


import com.example.demo.entity.Buchung;
import com.example.demo.entity.Konto;
import com.example.demo.entity.Konto.Kontotyp;
import com.example.demo.repository.BuchungRepository;
import com.example.demo.repository.BuchungskreisRepository;
import com.example.demo.repository.KontoRepository;
import com.example.demo.repository.RechnungRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ReportingService – FI-Berichte nach SAP-Logik.
 *
 * Berichte:
 *  - Bilanz        (Aktiva/Passiva)
 *  - GuV           (Erlöse - Aufwendungen)
 *  - Cash Flow     (Liquiditätsübersicht)
 *  - Sachkontensaldenanzeige (Kontendetail)
 *
 * ÄNDERUNG: Alle Berichte akzeptieren jetzt optional eine Buchungskreis-Nr.
 *
 * WICHTIG zum Hintergrund: Konto.saldo ist ein einziges, buchungskreis-
 * übergreifend geteiltes Feld – jede Buchung (unabhängig vom Buchungskreis)
 * aktualisiert denselben Kontosaldo (siehe BuchungsService.journalAktualisieren()).
 * Ein simples Filtern der Konten-Liste nach Buchungskreis wäre deshalb
 * fachlich falsch: DE00 und US00 buchen im DataLoader auf denselben
 * Kontenplan GL00 und dieselben Konten.
 *
 * Wird eine buchungskreisNr übergeben, wird der Saldo daher NICHT aus
 * Konto.saldo gelesen, sondern live aus den tatsächlichen, nicht
 * stornierten Buchungszeilen dieses Buchungskreises nachgerechnet (identische
 * Soll/Haben-Logik wie Konto.aktualisiereSaldo(), nur ohne Persistenz).
 * Ohne Parameter bleibt das bisherige globale Verhalten unverändert –
 * bestehende Aufrufer (z.B. das Frontend ohne Filter) sind nicht betroffen.
 *
 * BEWUSST NICHT Teil dieser Änderung (Scope-Grenze):
 *  - Kein Umbau des Persistenzmodells von Konto.saldo selbst.
 *  - Keine 1:1-Restrukturierung Kontenplan↔Buchungskreis.
 */
@Service
@Transactional(readOnly = true)
public class ReportingService {

    private final KontoRepository kontoRepository;
    private final BuchungRepository buchungRepository;
    private final RechnungRepository rechnungRepository;
    private final RechnungService rechnungService;
    private final BuchungskreisRepository buchungskreisRepository;

    public ReportingService(KontoRepository kontoRepository,
                            BuchungRepository buchungRepository,
                            RechnungRepository rechnungRepository,
                            RechnungService rechnungService,
                            BuchungskreisRepository buchungskreisRepository) {
        this.kontoRepository = kontoRepository;
        this.buchungRepository = buchungRepository;
        this.rechnungRepository = rechnungRepository;
        this.rechnungService = rechnungService;
        this.buchungskreisRepository = buchungskreisRepository;
    }

    // ─── BUCHUNGSKREIS-VALIDIERUNG ─────────────────────────────────────────────

    /**
     * Prüft, dass ein übergebener Buchungskreis existiert. Bei null/leer wird
     * null zurückgegeben (= "ungefiltert / global", bisheriges Verhalten).
     * Existiert die Nr nicht, wird eine sprechende Exception geworfen statt
     * stillschweigend auf "global" zurückzufallen – ein Tippfehler im Filter
     * soll nicht unbemerkt die gesamte Filterung aushebeln.
     */
    private String validierterBuchungskreis(String buchungskreisNr) {
        if (buchungskreisNr == null || buchungskreisNr.isBlank()) return null;
        buchungskreisRepository.findByBuchungskreisNr(buchungskreisNr)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Buchungskreis nicht gefunden: " + buchungskreisNr));
        return buchungskreisNr;
    }

    // ─── BILANZ ───────────────────────────────────────────────────────────────

    public Map<String, Object> getBilanz() {
        return getBilanz(null);
    }

    /**
     * Erstellt eine Bilanz: Aktiva = Passiva + Eigenkapital.
     *
     * @param buchungskreisNr optional – wenn gesetzt, wird die Bilanz auf
     *                        genau diesen Buchungskreis eingeschränkt
     *                        (live aus den Buchungszeilen nachgerechnet).
     */
    public Map<String, Object> getBilanz(String buchungskreisNr) {
        String bukrs = validierterBuchungskreis(buchungskreisNr);

        List<Konto> aktivKonten = kontoRepository.findByKontotypOrderByKontonummer(Kontotyp.AKTIV);
        List<Konto> passivKonten = kontoRepository.findByKontotypOrderByKontonummer(Kontotyp.PASSIV);

        List<Map<String, Object>> aktivaMaps = kontenMitSaldo(aktivKonten, bukrs);
        List<Map<String, Object>> passivaMaps = kontenMitSaldo(passivKonten, bukrs);

        BigDecimal sumAktiva = summeAusMaps(aktivaMaps);
        BigDecimal sumPassiva = summeAusMaps(passivaMaps);

        // Gewinn/Verlust aus GuV → fließt in Eigenkapital (im selben Buchungskreis!)
        Map<String, Object> guv = getGuV(bukrs);
        BigDecimal ergebnis = (BigDecimal) guv.get("ergebnis");

        Map<String, Object> bilanz = new LinkedHashMap<>();
        bilanz.put("stichtag", LocalDate.now().toString());
        bilanz.put("buchungskreis", bukrs != null ? bukrs : "ALLE");
        bilanz.put("aktiva", aktivaMaps);
        bilanz.put("sumAktiva", sumAktiva);
        bilanz.put("passiva", passivaMaps);
        bilanz.put("sumPassiva", sumPassiva);
        bilanz.put("jahresergebnis", ergebnis);
        bilanz.put("sumPassivaMitErgebnis", sumPassiva.add(ergebnis));
        bilanz.put("ausgeglichen", sumAktiva.compareTo(sumPassiva.add(ergebnis)) == 0);
        bilanz.put("hinweis", bukrs != null
                ? "Bilanz nach SAP FI-Logik, eingeschränkt auf Buchungskreis " + bukrs + ". Alle Werte in EUR."
                : "Bilanz nach SAP FI-Logik. Alle Buchungskreise. Alle Werte in EUR.");

        return bilanz;
    }

    // ─── GEWINN- UND VERLUSTRECHNUNG ──────────────────────────────────────────

    public Map<String, Object> getGuV() {
        return getGuV(null);
    }

    public Map<String, Object> getGuV(String buchungskreisNr) {
        String bukrs = validierterBuchungskreis(buchungskreisNr);

        List<Konto> ertragsKonten = kontoRepository.findByKontotypOrderByKontonummer(Kontotyp.ERTRAG);
        List<Konto> aufwandKonten = kontoRepository.findByKontotypOrderByKontonummer(Kontotyp.AUFWAND);

        List<Map<String, Object>> erloeseMaps = kontenMitSaldo(ertragsKonten, bukrs);
        List<Map<String, Object>> aufwandMaps = kontenMitSaldo(aufwandKonten, bukrs);

        BigDecimal gesamtErloese = summeAusMaps(erloeseMaps);
        BigDecimal gesamtAufwand = summeAusMaps(aufwandMaps);
        BigDecimal ergebnis = gesamtErloese.subtract(gesamtAufwand);

        Map<String, Object> guv = new LinkedHashMap<>();
        guv.put("zeitraum", LocalDate.now().getYear());
        guv.put("buchungskreis", bukrs != null ? bukrs : "ALLE");
        guv.put("erloese", erloeseMaps);
        guv.put("gesamtErloese", gesamtErloese);
        guv.put("aufwendungen", aufwandMaps);
        guv.put("gesamtAufwand", gesamtAufwand);
        guv.put("ergebnis", ergebnis);
        guv.put("ergebnisTyp", ergebnis.compareTo(BigDecimal.ZERO) >= 0 ? "GEWINN" : "VERLUST");
        guv.put("hinweis", bukrs != null
                ? "GuV-Berechnung nach SAP FI-Logik, eingeschränkt auf Buchungskreis " + bukrs + "."
                : "GuV-Berechnung nach SAP FI-Logik. Erlöse − Aufwendungen = Ergebnis. Alle Buchungskreise.");

        return guv;
    }

    // ─── KAPITALFLUSSRECHNUNG (CASH FLOW) ─────────────────────────────────────

    public Map<String, Object> getCashFlow() {
        return getCashFlow(null);
    }

    public Map<String, Object> getCashFlow(String buchungskreisNr) {
        String bukrs = validierterBuchungskreis(buchungskreisNr);

        rechnungService.ueberfaelligePruefen();

        List<Konto> alleAktiv = kontoRepository.findByKontotypOrderByKontonummer(Kontotyp.AKTIV);
        List<Konto> bankKonten = alleAktiv.stream()
                .filter(k -> k.getKontonummer() >= 100000 && k.getKontonummer() < 101000)
                .collect(Collectors.toList());

        List<Map<String, Object>> bankKontenMaps = kontenMitSaldo(bankKonten, bukrs);
        BigDecimal kassenbestand = summeAusMaps(bankKontenMaps);

        BigDecimal offeneForderungen = bukrs != null
                ? rechnungRepository.sumOffeneForderungen(bukrs)
                : rechnungRepository.sumOffeneForderungen();
        BigDecimal ueberfaelligeForderungen = bukrs != null
                ? rechnungRepository.sumUeberfaelligeForderungen(bukrs)
                : rechnungRepository.sumUeberfaelligeForderungen();
        BigDecimal offeneVerbindlichkeiten = bukrs != null
                ? rechnungRepository.sumOffeneVerbindlichkeiten(bukrs)
                : rechnungRepository.sumOffeneVerbindlichkeiten();

        LocalDate bis = LocalDate.now();
        LocalDate von = bis.minusDays(30);
        List<Buchung> letzteBuchungen = bukrs != null
                ? buchungRepository.findByBuchungsdatumBetweenAndBuchungskreis_BuchungskreisNrOrderByBuchungsdatumAsc(von, bis, bukrs)
                : buchungRepository.findByBuchungsdatumBetweenOrderByBuchungsdatumAsc(von, bis);

        BigDecimal einnahmen = letzteBuchungen.stream()
                .filter(b -> b.getSollHaben() == Buchung.SollHaben.SOLL
                        && !b.getStorniert()
                        && bankKonten.contains(b.getKonto()))
                .map(Buchung::getBetrag)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal ausgaben = letzteBuchungen.stream()
                .filter(b -> b.getSollHaben() == Buchung.SollHaben.HABEN
                        && !b.getStorniert()
                        && bankKonten.contains(b.getKonto()))
                .map(Buchung::getBetrag)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> cashFlow = new LinkedHashMap<>();
        cashFlow.put("stichtag", bis.toString());
        cashFlow.put("buchungskreis", bukrs != null ? bukrs : "ALLE");
        cashFlow.put("zeitraum", "Letzte 30 Tage (" + von + " bis " + bis + ")");
        cashFlow.put("kassenbestand", kassenbestand);
        cashFlow.put("einnahmen30Tage", einnahmen);
        cashFlow.put("ausgaben30Tage", ausgaben);
        cashFlow.put("nettoFluss", einnahmen.subtract(ausgaben));
        cashFlow.put("offeneForderungen", offeneForderungen != null ? offeneForderungen : BigDecimal.ZERO);
        cashFlow.put("ueberfaelligeForderungen", ueberfaelligeForderungen != null ? ueberfaelligeForderungen : BigDecimal.ZERO);
        cashFlow.put("offeneVerbindlichkeiten", offeneVerbindlichkeiten != null ? offeneVerbindlichkeiten : BigDecimal.ZERO);
        cashFlow.put("bankKonten", bankKontenMaps);
        cashFlow.put("hinweis", bukrs != null
                ? "Vereinfachte Kapitalflussrechnung V1, eingeschränkt auf Buchungskreis " + bukrs + "."
                : "Vereinfachte Kapitalflussrechnung V1. Basiert auf Bankkonten (100000-100999). Alle Buchungskreise.");

        return cashFlow;
    }

    // ─── SACHKONTENSALDENANZEIGE ──────────────────────────────────────────────

    public Map<String, Object> getSachkontensaldo(Integer kontonummer) {
        return getSachkontensaldo(kontonummer, null);
    }

    /**
     * Zeigt Soll/Haben und Saldo eines Kontos – äquivalent zur
     * SAP-Transaktion FS10N (Sachkontensaldenanzeige), optional auf einen
     * Buchungskreis eingeschränkt.
     */
    public Map<String, Object> getSachkontensaldo(Integer kontonummer, String buchungskreisNr) {
        String bukrs = validierterBuchungskreis(buchungskreisNr);

        Konto konto = kontoRepository.findByKontonummer(kontonummer)
                .orElseThrow(() -> new IllegalArgumentException("Konto nicht gefunden: " + kontonummer));

        List<Buchung> buchungen = bukrs != null
                ? buchungRepository.findByKontoAndBuchungskreis_BuchungskreisNrAndStorniertFalse(konto, bukrs)
                : buchungRepository.findByKontoOrderByBuchungsdatumDesc(konto);

        BigDecimal summeSoll = buchungen.stream()
                .filter(b -> b.getSollHaben() == Buchung.SollHaben.SOLL && !b.getStorniert())
                .map(Buchung::getBetrag)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal summeHaben = buchungen.stream()
                .filter(b -> b.getSollHaben() == Buchung.SollHaben.HABEN && !b.getStorniert())
                .map(Buchung::getBetrag)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ÄNDERUNG: bei gesetztem Filter Saldo live aus den gefilterten
        // Buchungszeilen nachrechnen statt aus dem globalen Konto.saldo.
        BigDecimal saldo = bukrs != null
                ? saldoAusBuchungen(konto, buchungen)
                : konto.getSaldo();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("kontonummer", konto.getKontonummer());
        result.put("kontobezeichnung", konto.getKontobezeichnung());
        result.put("kontotyp", konto.getKontotyp());
        result.put("buchungskreis", bukrs != null ? bukrs : "ALLE");
        result.put("summeSoll", summeSoll);
        result.put("summeHaben", summeHaben);
        result.put("saldo", saldo);
        result.put("anzahlBuchungen", buchungen.size());

        return result;
    }

    // ─── ALLE KONTEN ──────────────────────────────────────────────────────────

    public List<Map<String, Object>> getKontenUebersicht() {
        return getKontenUebersicht(null);
    }

    /** ÄNDERUNG: optionaler Buchungskreis-Filter, analog zu den übrigen Berichten. */
    public List<Map<String, Object>> getKontenUebersicht(String buchungskreisNr) {
        String bukrs = validierterBuchungskreis(buchungskreisNr);
        List<Konto> alle = kontoRepository.findAllSorted();
        return kontenMitSaldo(alle, bukrs);
    }

    // ─── HILFSMETHODEN ────────────────────────────────────────────────────────

    /**
     * Liefert die übergebenen Konten als Map-Liste, jeweils mit dem Saldo
     * entweder aus Konto.saldo (global, bukrs == null) oder live nachgerechnet
     * aus den Buchungszeilen des angegebenen Buchungskreises. Konten ohne
     * Buchungen in diesem Buchungskreis erscheinen mit Saldo 0 (nicht
     * ausgeblendet) – konsistent mit FS10N-Verhalten in SAP.
     */
    private List<Map<String, Object>> kontenMitSaldo(List<Konto> konten, String bukrs) {
        if (bukrs == null) {
            return konten.stream().map(this::kontoZuMap).collect(Collectors.toList());
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Konto k : konten) {
            List<Buchung> buchungen = buchungRepository
                    .findByKontoAndBuchungskreis_BuchungskreisNrAndStorniertFalse(k, bukrs);
            BigDecimal saldo = saldoAusBuchungen(k, buchungen);
            result.add(kontoZuMap(k, saldo));
        }
        return result;
    }

    /**
     * Rechnet den Saldo eines Kontos ausschließlich aus den übergebenen
     * Buchungszeilen nach – identische Logik zu Konto.aktualisiereSaldo(),
     * nur ohne Persistenz (rein für die Reporting-Ansicht).
     */
    private BigDecimal saldoAusBuchungen(Konto konto, List<Buchung> buchungen) {
        BigDecimal saldo = BigDecimal.ZERO;
        boolean erhoehtImSoll = konto.getKontotyp() == Kontotyp.AKTIV || konto.getKontotyp() == Kontotyp.AUFWAND;
        for (Buchung b : buchungen) {
            if (Boolean.TRUE.equals(b.getStorniert())) continue;
            boolean isSoll = b.getSollHaben() == Buchung.SollHaben.SOLL;
            if (erhoehtImSoll) {
                saldo = isSoll ? saldo.add(b.getBetrag()) : saldo.subtract(b.getBetrag());
            } else {
                saldo = isSoll ? saldo.subtract(b.getBetrag()) : saldo.add(b.getBetrag());
            }
        }
        return saldo;
    }

    private BigDecimal summeAusMaps(List<Map<String, Object>> konten) {
        BigDecimal summe = BigDecimal.ZERO;
        for (Map<String, Object> m : konten) {
            summe = summe.add((BigDecimal) m.get("saldo"));
        }
        return summe;
    }

    private Map<String, Object> kontoZuMap(Konto k) {
        return kontoZuMap(k, k.getSaldo());
    }

    private Map<String, Object> kontoZuMap(Konto k, BigDecimal saldo) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("kontonummer", k.getKontonummer());
        m.put("bezeichnung", k.getKontobezeichnung());
        m.put("typ", k.getKontotyp());
        m.put("saldo", saldo);
        return m;
    }
}