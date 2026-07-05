package com.example.demo.service;

import com.example.demo.entity.Geschaeftsfall;
import com.example.demo.entity.Rechnung;
import com.example.demo.entity.Rechnung.RechnungStatus;
import com.example.demo.repository.RechnungRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * RechnungService – verwaltet den offene-Posten-Lebenszyklus einer Rechnung
 * (OFFEN → BEZAHLT / STORNIERT / UEBERFAELLIG).
 *
 * ÄNDERUNG: alle()/offenePosten() jetzt zusätzlich mit optionalem
 * Buchungskreis-Filter, aufbauend auf der neuen Geschaeftsfall.buchungskreis-FK.
 */
@Service
@Transactional
public class RechnungService {

    /** Standard-Zahlungsziel in Tagen, falls kein individuelles Ziel übergeben wird. */
    private static final int STANDARD_ZAHLUNGSZIEL_TAGE = 30;

    private final RechnungRepository rechnungRepository;

    public RechnungService(RechnungRepository rechnungRepository) {
        this.rechnungRepository = rechnungRepository;
    }

    // ─── ANLEGEN ────────────────────────────────────────────────────────────

    public Rechnung rechnungErstellen(Geschaeftsfall geschaeftsfall, BigDecimal betrag) {
        return rechnungErstellen(geschaeftsfall, betrag, STANDARD_ZAHLUNGSZIEL_TAGE);
    }

    public Rechnung rechnungErstellen(Geschaeftsfall geschaeftsfall, BigDecimal betrag, int zahlungszielTage) {
        LocalDate heute = LocalDate.now();
        Rechnung rechnung = new Rechnung(heute, heute.plusDays(zahlungszielTage), betrag, geschaeftsfall);
        return rechnungRepository.save(rechnung);
    }

    // ─── AUSGLEICH (ZAHLUNG) ────────────────────────────────────────────────

    public Rechnung rechnungAusgleichen(Rechnung rechnung, BigDecimal zahlungsbetrag) {
        if (rechnung.getStatus() == RechnungStatus.BEZAHLT) {
            throw new IllegalArgumentException(
                    "Rechnung " + rechnung.getRechnungId() + " ist bereits vollständig bezahlt.");
        }
        if (rechnung.getStatus() == RechnungStatus.STORNIERT) {
            throw new IllegalArgumentException(
                    "Rechnung " + rechnung.getRechnungId() + " ist storniert und kann nicht ausgeglichen werden.");
        }
        if (zahlungsbetrag.compareTo(rechnung.getBetrag()) != 0) {
            throw new IllegalArgumentException(
                    "Zahlungsbetrag (" + zahlungsbetrag + ") entspricht nicht dem offenen Rechnungsbetrag (" +
                    rechnung.getBetrag() + ") von Rechnung " + rechnung.getRechnungId() +
                    ". Teilzahlungen werden aktuell nicht unterstützt (V1) – bitte exakten Betrag verwenden.");
        }
        rechnung.setStatus(RechnungStatus.BEZAHLT);
        return rechnungRepository.save(rechnung);
    }

    public Rechnung rechnungStornieren(Rechnung rechnung) {
        if (rechnung.getStatus() == RechnungStatus.BEZAHLT) {
            throw new IllegalArgumentException(
                    "Bereits bezahlte Rechnung " + rechnung.getRechnungId() + " kann nicht storniert werden.");
        }
        rechnung.setStatus(RechnungStatus.STORNIERT);
        return rechnungRepository.save(rechnung);
    }

    // ─── ABFRAGEN ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Optional<Rechnung> findeOffenePostenFuerGeschaeftsfall(Geschaeftsfall geschaeftsfall) {
        return rechnungRepository.findByGeschaeftsfall(geschaeftsfall).stream()
                .filter(r -> r.getStatus() == RechnungStatus.OFFEN || r.getStatus() == RechnungStatus.UEBERFAELLIG)
                .findFirst();
    }

    @Transactional(readOnly = true)
    public List<Rechnung> alle() {
        return rechnungRepository.findAll();
    }

    /** ÄNDERUNG: alle Rechnungen, optional auf einen Buchungskreis eingeschränkt. */
    @Transactional(readOnly = true)
    public List<Rechnung> alle(String buchungskreisNr) {
        if (buchungskreisNr == null || buchungskreisNr.isBlank()) {
            return alle();
        }
        return rechnungRepository.findByGeschaeftsfall_Buchungskreis_BuchungskreisNr(buchungskreisNr);
    }

    @Transactional(readOnly = true)
    public List<Rechnung> offenePosten() {
        List<Rechnung> offen = new java.util.ArrayList<>(rechnungRepository.findByStatus(RechnungStatus.OFFEN));
        offen.addAll(rechnungRepository.findByStatus(RechnungStatus.UEBERFAELLIG));
        return offen;
    }

    /** ÄNDERUNG: offene Posten, optional auf einen Buchungskreis eingeschränkt. */
    @Transactional(readOnly = true)
    public List<Rechnung> offenePosten(String buchungskreisNr) {
        if (buchungskreisNr == null || buchungskreisNr.isBlank()) {
            return offenePosten();
        }
        return rechnungRepository.findByGeschaeftsfall_Buchungskreis_BuchungskreisNrAndStatusIn(
                buchungskreisNr, List.of(RechnungStatus.OFFEN, RechnungStatus.UEBERFAELLIG));
    }

    // ─── ÜBERFÄLLIGKEITS-PRÜFUNG ────────────────────────────────────────────

    public int ueberfaelligePruefen() {
        LocalDate heute = LocalDate.now();
        List<Rechnung> faellige = rechnungRepository.findByStatus(RechnungStatus.OFFEN).stream()
                .filter(r -> r.getFaelligkeit() != null && r.getFaelligkeit().isBefore(heute))
                .toList();
        faellige.forEach(r -> r.setStatus(RechnungStatus.UEBERFAELLIG));
        rechnungRepository.saveAll(faellige);
        return faellige.size();
    }

    @Scheduled(cron = "0 5 0 * * *")
    public void ueberfaelligePruefenScheduled() {
        ueberfaelligePruefen();
    }
}