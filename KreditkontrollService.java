package com.example.demo.service;

import com.example.demo.entity.Kreditkontrollbereich;
import com.example.demo.entity.Kunde;
import com.example.demo.exception.KreditlimitUeberschrittenException;
import com.example.demo.repository.RechnungRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * KreditkontrollService – setzt den Kreditkontrollbereich fachlich um.
 *
 * Prüft vor einer neuen Faktura, ob (bereits offene Forderungen des Kunden
 * + neuer Rechnungsbetrag) das Kreditlimit seines Kreditkontrollbereichs
 * überschreiten würde.
 *
 * Hinweis: Die Summe "offene Forderungen" basiert auf der Rechnung-Entity.
 * Solange Rechnungen im Buchungsfluss nicht angelegt werden, bleibt diese
 * Summe 0 – die Prüfung greift dann nur für den aktuell zu buchenden Betrag
 * gegen das Limit. Sobald Rechnung/offene-Posten-Verwaltung ergänzt wird,
 * greift die Prüfung vollständig kumulativ.
 */
@Service
public class KreditkontrollService {

    private final RechnungRepository rechnungRepository;

    public KreditkontrollService(RechnungRepository rechnungRepository) {
        this.rechnungRepository = rechnungRepository;
    }

    public void pruefeKreditlimit(Kunde kunde, BigDecimal neuerBetrag) {
        if (kunde == null) return;

        Kreditkontrollbereich kkb = kunde.getKreditkontrollbereich();
        if (kkb == null || kkb.getKreditlimit() == null) return;

        BigDecimal offeneForderungen = rechnungRepository.sumOffenerBetragFuerKunde(kunde.getKundenId());
        BigDecimal neueSumme = offeneForderungen.add(neuerBetrag);

        if (neueSumme.compareTo(kkb.getKreditlimit()) > 0) {
            throw new KreditlimitUeberschrittenException(
                    "Kreditlimit für " + kunde.getFirmenname() + " (KKB " + kkb.getKkbNr() + ") überschritten: " +
                    "offene Forderungen " + offeneForderungen + " + neue Rechnung " + neuerBetrag +
                    " = " + neueSumme + " > Limit " + kkb.getKreditlimit());
        }
    }
}
