package com.example.demo.service;

import com.example.demo.entity.Geschaeftsfall;
import com.example.demo.entity.Geschaeftsfall.GeschaeftsfallTyp;
import com.example.demo.entity.Kunde;
import com.example.demo.entity.Lieferant;
import com.example.demo.entity.Rechnung;
import com.example.demo.repository.KundeRepository;
import com.example.demo.repository.LieferantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * NebenbuchService – stellt die FI-Nebenbücher (Debitoren-/Kreditoren-
 * buchhaltung) dar und gleicht sie live gegen das Hauptbuch ab.
 *
 * SAP-Hintergrund: Kunde/Lieferant werden NIE direkt im Hauptbuch geführt –
 * jede Faktura/jeder Wareneingang bucht auf das Abstimmkonto
 * (Kunde.abstimmkonto / Lieferant.abstimmkonto). Der Saldo dieses
 * Sachkontos ist per Definition die Summe aller offenen Posten der
 * zugeordneten Debitoren/Kreditoren ("Nebenbuch = Kontrollsumme des
 * Abstimmkontos"). Dieser Service macht diese Summe erstmals sichtbar und
 * prüft sie gegen den tatsächlichen Hauptbuch-Saldo.
 *
 * Datengrundlage: Geschaeftsfall.kunde (VERKAUF) bzw. Geschaeftsfall.lieferant
 * (EINKAUF) + offene Rechnung-Posten (RechnungService.offenePosten()).
 * Bewusst KEINE neuen Repository-Methoden auf RechnungRepository nötig –
 * die Gruppierung nach Kunde/Lieferant erfolgt hier im Service, aufbauend
 * auf der bereits vorhandenen offenePosten()-Abfrage.
 */
@Service
@Transactional
public class NebenbuchService {

    private final RechnungService rechnungService;
    private final KundeRepository kundeRepository;
    private final LieferantRepository lieferantRepository;
    private final ReportingService reportingService;

    public NebenbuchService(RechnungService rechnungService,
                             KundeRepository kundeRepository,
                             LieferantRepository lieferantRepository,
                             ReportingService reportingService) {
        this.rechnungService = rechnungService;
        this.kundeRepository = kundeRepository;
        this.lieferantRepository = lieferantRepository;
        this.reportingService = reportingService;
    }

    // ─── DEBITOREN (FI-AR) ──────────────────────────────────────────────────

    public List<Map<String, Object>> debitorenSalden(String buchungskreisNr) {
        rechnungService.ueberfaelligePruefen();

        Map<Long, BigDecimal> offenJeKunde = new LinkedHashMap<>();
        for (Rechnung r : rechnungService.offenePosten(buchungskreisNr)) {
            Geschaeftsfall gf = r.getGeschaeftsfall();
            if (gf.getTyp() != GeschaeftsfallTyp.VERKAUF || gf.getKunde() == null) continue;
            offenJeKunde.merge(gf.getKunde().getKundenId(), r.getBetrag(), BigDecimal::add);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Kunde k : kundeRepository.findAll()) {
            BigDecimal offen = offenJeKunde.getOrDefault(k.getKundenId(), BigDecimal.ZERO);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("kundenId", k.getKundenId());
            m.put("firmenname", k.getFirmenname());
            m.put("abstimmkonto", k.getAbstimmkonto());
            m.put("offenerBetrag", offen);
            if (k.getKreditkontrollbereich() != null) {
                m.put("kkbNr", k.getKreditkontrollbereich().getKkbNr());
                BigDecimal limit = k.getKreditkontrollbereich().getKreditlimit();
                m.put("kreditlimit", limit);
                m.put("limitAusschoepfungProzent", (limit != null && limit.compareTo(BigDecimal.ZERO) > 0)
                        ? offen.multiply(BigDecimal.valueOf(100)).divide(limit, 1, RoundingMode.HALF_UP)
                        : null);
            }
            result.add(m);
        }
        return result;
    }

    // ─── KREDITOREN (FI-AP) ─────────────────────────────────────────────────

    public List<Map<String, Object>> kreditorenSalden(String buchungskreisNr) {
        rechnungService.ueberfaelligePruefen();

        Map<Long, BigDecimal> offenJeLieferant = new LinkedHashMap<>();
        for (Rechnung r : rechnungService.offenePosten(buchungskreisNr)) {
            Geschaeftsfall gf = r.getGeschaeftsfall();
            if (gf.getTyp() != GeschaeftsfallTyp.EINKAUF || gf.getLieferant() == null) continue;
            offenJeLieferant.merge(gf.getLieferant().getLieferantenId(), r.getBetrag(), BigDecimal::add);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Lieferant l : lieferantRepository.findAll()) {
            BigDecimal offen = offenJeLieferant.getOrDefault(l.getLieferantenId(), BigDecimal.ZERO);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("lieferantenId", l.getLieferantenId());
            m.put("firmenname", l.getFirmenname());
            m.put("abstimmkonto", l.getAbstimmkonto());
            m.put("offenerBetrag", offen);
            result.add(m);
        }
        return result;
    }

    // ─── ABSTIMMUNG NEBENBUCH ↔ HAUPTBUCH ───────────────────────────────────

    public Map<String, Object> abstimmung(String buchungskreisNr) {
        List<Map<String, Object>> debitoren = debitorenSalden(buchungskreisNr);
        List<Map<String, Object>> kreditoren = kreditorenSalden(buchungskreisNr);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("buchungskreis", (buchungskreisNr == null || buchungskreisNr.isBlank()) ? "ALLE" : buchungskreisNr);
        result.put("debitoren", debitoren);
        result.put("kreditoren", kreditoren);
        result.put("abstimmkontenDebitoren", abstimmkontoVergleich(debitoren, buchungskreisNr));
        result.put("abstimmkontenKreditoren", abstimmkontoVergleich(kreditoren, buchungskreisNr));
        return result;
    }

    /**
     * Gruppiert die Nebenbuch-Salden nach Abstimmkonto und vergleicht die
     * Summe mit dem tatsächlichen Hauptbuch-Saldo dieses Sachkontos
     * (ReportingService.getSachkontensaldo(), buchungskreis-aware).
     */
    private List<Map<String, Object>> abstimmkontoVergleich(List<Map<String, Object>> salden, String bukrs) {
        Map<Integer, BigDecimal> nebenbuchSummen = new LinkedHashMap<>();
        for (Map<String, Object> s : salden) {
            Integer konto = (Integer) s.get("abstimmkonto");
            if (konto == null) continue;
            nebenbuchSummen.merge(konto, (BigDecimal) s.get("offenerBetrag"), BigDecimal::add);
        }

        List<Map<String, Object>> vergleich = new ArrayList<>();
        for (Map.Entry<Integer, BigDecimal> entry : nebenbuchSummen.entrySet()) {
            Map<String, Object> hauptbuch = reportingService.getSachkontensaldo(entry.getKey(), bukrs);
            BigDecimal hauptbuchSaldo = (BigDecimal) hauptbuch.get("saldo");
            BigDecimal differenz = hauptbuchSaldo.subtract(entry.getValue());

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("abstimmkonto", entry.getKey());
            m.put("kontobezeichnung", hauptbuch.get("kontobezeichnung"));
            m.put("nebenbuchSumme", entry.getValue());
            m.put("hauptbuchSaldo", hauptbuchSaldo);
            m.put("differenz", differenz);
            m.put("abgestimmt", differenz.compareTo(BigDecimal.ZERO) == 0);
            vergleich.add(m);
        }
        return vergleich;
    }
}