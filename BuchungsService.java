package com.example.demo.service;


import com.example.demo.entity.*;
import com.example.demo.entity.Buchung.SollHaben;
import com.example.demo.exception.BelegprinzipException;
import com.example.demo.exception.SollHabenException;
import com.example.demo.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * BuchungsService – Kernlogik des FI-Moduls.
 *
 * ÄNDERUNG (Nebenbuch-Feature): pruefeKeineDirekteAbstimmkontoBuchung()
 * ergänzt. In SAP dürfen Abstimmkonten (Debitoren-/Kreditorenbuchhaltung)
 * NIEMALS direkt im Hauptbuch bebucht werden – jede Buchung auf ein
 * solches Konto muss über die Nebenbuch-Transaktion laufen (hier:
 * FI-SD/FI-MM), damit Kunde/Lieferant und Rechnung korrekt mitgeführt
 * werden. Ohne diese Sperre konnte man über "Neue Buchung" jederzeit
 * direkt auf z.B. Konto 120000 buchen und damit Neben- und Hauptbuch
 * unbemerkt auseinanderlaufen lassen (siehe NebenbuchService für die
 * Abstimmungsprüfung). Aufgerufen wird die Prüfung NUR vom
 * BuchungsController (manuelle Erfassung) – FI-SD/FI-MM/DataLoader rufen
 * bucheSollHaben() weiterhin direkt auf und sind bewusst NICHT betroffen,
 * da sie genau die legitime Nebenbuch-Buchung sind.
 */
@Service
@Transactional
public class BuchungsService {

    private static final String DEFAULT_BUCHUNGSKREIS = "DE00";

    private final BuchungRepository buchungRepository;
    private final KontoRepository kontoRepository;
    private final RechnungRepository rechnungRepository;
    private final BkpfBelegRepository bkpfRepo;
    private final BuchungskreisRepository buchungskreisRepo;
    private final KundeRepository kundeRepository;
    private final LieferantRepository lieferantRepository;

    public BuchungsService(BuchungRepository buchungRepository,
                           KontoRepository kontoRepository,
                           RechnungRepository rechnungRepository,
                           BkpfBelegRepository bkpfRepo,
                           BuchungskreisRepository buchungskreisRepo,
                           KundeRepository kundeRepository,
                           LieferantRepository lieferantRepository) {
        this.buchungRepository = buchungRepository;
        this.kontoRepository = kontoRepository;
        this.rechnungRepository = rechnungRepository;
        this.bkpfRepo = bkpfRepo;
        this.buchungskreisRepo = buchungskreisRepo;
        this.kundeRepository = kundeRepository;
        this.lieferantRepository = lieferantRepository;
    }

    // ─── ABSTIMMKONTO-SCHUTZ (Nebenbuch-Feature) ──────────────────────────────

    /**
     * Wirft eine IllegalArgumentException, wenn Soll- oder Habenkonto ein
     * Abstimmkonto ist (d.h. von mindestens einem Kunden oder Lieferanten als
     * abstimmkonto referenziert wird). Wird ausschließlich vom
     * BuchungsController vor der manuellen Buchungserfassung aufgerufen.
     */
    @Transactional(readOnly = true)
    public void pruefeKeineDirekteAbstimmkontoBuchung(Integer sollKontoNr, Integer habenKontoNr) {
        pruefeEinzelnesKontoKeinAbstimmkonto(sollKontoNr);
        pruefeEinzelnesKontoKeinAbstimmkonto(habenKontoNr);
    }

    private void pruefeEinzelnesKontoKeinAbstimmkonto(Integer kontoNr) {
        boolean istDebitorAbstimmkonto = kundeRepository.existsByAbstimmkonto(kontoNr);
        boolean istKreditorAbstimmkonto = lieferantRepository.existsByAbstimmkonto(kontoNr);
        if (istDebitorAbstimmkonto || istKreditorAbstimmkonto) {
            throw new IllegalArgumentException(
                    "Konto " + kontoNr + " ist ein Abstimmkonto (" +
                    (istDebitorAbstimmkonto ? "Debitoren" : "Kreditoren") +
                    "buchhaltung) und darf nicht manuell über die Hauptbuchbuchung bebucht werden. " +
                    "Bitte den Geschäftsvorfall über FI-SD/FI-MM (Integration) buchen, damit das " +
                    "Nebenbuch (Kunde/Lieferant) konsistent mit dem Hauptbuch bleibt.");
        }
    }

    // ─── HAUPTPROZESS: Doppelbuchung (Soll/Haben) ─────────────────────────────

    /** Bestehende Signatur – bucht weiterhin im Default-Buchungskreis "DE00". */
    public List<Buchung> bucheSollHaben(Integer sollKontoNr, Integer habenKontoNr,
                                        BigDecimal betrag, String buchungstext,
                                        LocalDate datum, String erfasstVon,
                                        Long rechnungId) {
        return bucheSollHaben(sollKontoNr, habenKontoNr, betrag, buchungstext,
                datum, erfasstVon, rechnungId, null);
    }

    /**
     * Bucht einen vollständigen Buchungssatz (Soll + Haben) in einem konkreten Buchungskreis.
     *
     * @param buchungskreisNr Buchungskreis-Nr, z.B. "DE00" oder "US00". Bei null/leer
     *                        wird "DE00" verwendet. Existiert der Buchungskreis nicht,
     *                        wird eine IllegalArgumentException geworfen (kein stiller Fallback).
     */
    public List<Buchung> bucheSollHaben(Integer sollKontoNr, Integer habenKontoNr,
                                        BigDecimal betrag, String buchungstext,
                                        LocalDate datum, String erfasstVon,
                                        Long rechnungId, String buchungskreisNr) {

        // SCHRITT 1: Beleg erfassen – Konten und Buchungskreis laden
        Konto sollKonto = kontoRepository.findByKontonummer(sollKontoNr)
                .orElseThrow(() -> new IllegalArgumentException("Sollkonto nicht gefunden: " + sollKontoNr));

        Konto habenKonto = kontoRepository.findByKontonummer(habenKontoNr)
                .orElseThrow(() -> new IllegalArgumentException("Habenkonto nicht gefunden: " + habenKontoNr));

        Buchungskreis buchungskreis = resolveBuchungskreis(buchungskreisNr);

        Rechnung rechnung = null;
        if (rechnungId != null) {
            rechnung = rechnungRepository.findById(rechnungId)
                    .orElseThrow(() -> new IllegalArgumentException("Rechnung nicht gefunden: " + rechnungId));
        }

        // SCHRITT 2: Soll/Haben prüfen
        sollHabenPruefen(betrag);

        // SCHRITT 3: Belege speichern (GoBD-konform)
        Long naechsteNr = buchungRepository.naechsteBelegnummer();
        BkpfBeleg kopf = new BkpfBeleg(naechsteNr, buchungskreis, datum, datum, "SA", erfasstVon, buchungstext);
        kopf = bkpfRepo.save(kopf);

        Buchung sollBuchung = new Buchung(
                naechsteNr, datum, betrag, buchungstext + " [SOLL]",
                SollHaben.SOLL, sollKonto, rechnung, erfasstVon);
        sollBuchung.setBkpfBeleg(kopf);
        sollBuchung.setBuchungskreis(buchungskreis);

        Buchung habenBuchung = new Buchung(
                naechsteNr + 1, datum, betrag, buchungstext + " [HABEN]",
                SollHaben.HABEN, habenKonto, rechnung, erfasstVon);
        habenBuchung.setBkpfBeleg(kopf);
        habenBuchung.setBuchungskreis(buchungskreis);

        sollBuchung = buchungRepository.save(sollBuchung);
        habenBuchung = buchungRepository.save(habenBuchung);

        // SCHRITT 4: Journal aktualisieren (Kontosalden)
        journalAktualisieren(sollKonto, betrag, true);
        journalAktualisieren(habenKonto, betrag, false);

        List<Buchung> ergebnis = new ArrayList<>();
        ergebnis.add(sollBuchung);
        ergebnis.add(habenBuchung);

        return ergebnis;
    }

    /**
     * Löst eine Buchungskreis-Nr in die Entity auf. Bei null/leer wird "DE00"
     * verwendet. Wirft eine sprechende Exception statt eine unbekannte Nr
     * still zu ignorieren – so fallen fehlende Stammdaten sofort auf.
     */
    private Buchungskreis resolveBuchungskreis(String buchungskreisNr) {
        String zielNr = (buchungskreisNr == null || buchungskreisNr.isBlank())
                ? DEFAULT_BUCHUNGSKREIS
                : buchungskreisNr;
        return buchungskreisRepo.findByBuchungskreisNr(zielNr)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Buchungskreis nicht gefunden: " + zielNr +
                        " (bitte zuerst unter Organisationsstruktur anlegen)"));
    }

    /** Prüft nur die Existenz (für Filter-Parameter, ohne die Entity zu benötigen). */
    private void pruefeBuchungskreisExistiert(String buchungskreisNr) {
        if (buchungskreisRepo.findByBuchungskreisNr(buchungskreisNr).isEmpty()) {
            throw new IllegalArgumentException("Buchungskreis nicht gefunden: " + buchungskreisNr);
        }
    }

    // ─── STORNO (Belegprinzip) ─────────────────────────────────────────────────

    /**
     * Storniert einen Beleg durch Erstellung eines Gegenbelegs.
     * Der Original-Beleg bleibt erhalten (GoBD: keine Löschung!).
     */
    public List<Buchung> storniereBuchung(Long buchungId, String erfasstVon) {
        Buchung original = buchungRepository.findById(buchungId)
                .orElseThrow(() -> new IllegalArgumentException("Buchung nicht gefunden: " + buchungId));

        if (original.getStorniert()) {
            throw new BelegprinzipException("Buchung " + original.getBelegnummer() + " ist bereits storniert.");
        }

        Long naechsteNr = buchungRepository.naechsteBelegnummer();

        // Stornobuchung = Gegenbuchung (Soll ↔ Haben vertauscht)
        SollHaben gegenseite = original.getSollHaben() == SollHaben.SOLL ? SollHaben.HABEN : SollHaben.SOLL;

        Buchung storno = new Buchung(
                naechsteNr,
                LocalDate.now(),
                original.getBetrag(),
                "STORNO: " + original.getBuchungstext(),
                gegenseite,
                original.getKonto(),
                original.getRechnung(),
                erfasstVon);
        storno.setStornoVon(original);
        // Stornobuchung übernimmt denselben Buchungskreis wie der Originalbeleg
        storno.setBuchungskreis(original.getBuchungskreis());

        original.setStorniert(true);
        buchungRepository.save(original);
        storno = buchungRepository.save(storno);

        // Kontosaldo rückgängig machen
        boolean warSoll = original.getSollHaben() == SollHaben.SOLL;
        journalAktualisieren(original.getKonto(), original.getBetrag(), !warSoll);

        List<Buchung> ergebnis = new ArrayList<>();
        ergebnis.add(storno);
        return ergebnis;
    }

    // ─── VALIDIERUNG ──────────────────────────────────────────────────────────

    public void sollHabenPruefen(BigDecimal betrag) {
        if (betrag == null || betrag.compareTo(BigDecimal.ZERO) <= 0) {
            throw new SollHabenException("Buchungsbetrag muss größer als 0 sein. Erhalten: " + betrag);
        }
    }

    // ─── JOURNAL-UPDATE (ACDOCA-Logik) ────────────────────────────────────────

    private void journalAktualisieren(Konto konto, BigDecimal betrag, boolean isSoll) {
        konto.aktualisiereSaldo(betrag, isSoll);
        kontoRepository.save(konto);
    }

    // ─── ABFRAGEN ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Buchung> getJournal() {
        return buchungRepository.findJournal();
    }

    @Transactional(readOnly = true)
    public List<Buchung> getJournal(String buchungskreisNr) {
        if (buchungskreisNr == null || buchungskreisNr.isBlank()) {
            return getJournal();
        }
        pruefeBuchungskreisExistiert(buchungskreisNr);
        return buchungRepository.findJournalByBuchungskreis(buchungskreisNr);
    }

    @Transactional(readOnly = true)
    public List<Buchung> getBuchungenNachZeitraum(LocalDate von, LocalDate bis) {
        return buchungRepository.findByBuchungsdatumBetweenOrderByBuchungsdatumAsc(von, bis);
    }

    @Transactional(readOnly = true)
    public List<Buchung> getBuchungenNachZeitraum(LocalDate von, LocalDate bis, String buchungskreisNr) {
        if (buchungskreisNr == null || buchungskreisNr.isBlank()) {
            return getBuchungenNachZeitraum(von, bis);
        }
        pruefeBuchungskreisExistiert(buchungskreisNr);
        return buchungRepository.findByBuchungsdatumBetweenAndBuchungskreis_BuchungskreisNrOrderByBuchungsdatumAsc(
                von, bis, buchungskreisNr);
    }

    @Transactional(readOnly = true)
    public List<Buchung> getAuditTrail(Long belegnummer) {
        return buchungRepository.findAuditTrailByBelegnummer(belegnummer);
    }

    @Transactional(readOnly = true)
    public List<Buchung> getSachkontenbuchungen(Integer kontonummer, LocalDate von, LocalDate bis) {
        Konto konto = kontoRepository.findByKontonummer(kontonummer)
                .orElseThrow(() -> new IllegalArgumentException("Konto nicht gefunden: " + kontonummer));
        return buchungRepository.findByKontoUndZeitraum(konto, von, bis);
    }

    @Transactional(readOnly = true)
    public List<Buchung> getAlleBuchungen() {
        return buchungRepository.findAll();
    }

    public Buchung getBuchungById(Long id) {
        return buchungRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Buchung nicht gefunden: " + id));
    }
}