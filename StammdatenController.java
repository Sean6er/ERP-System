package com.example.demo.controller;

import com.example.demo.dto.KontoRequest;
import com.example.demo.dto.KundeRequest;
import com.example.demo.entity.*;
import com.example.demo.repository.KontenplanRepository;
import com.example.demo.repository.KontengruppeRepository;
import com.example.demo.repository.KreditkontrollbereichRepository;
import com.example.demo.service.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/stammdaten")
public class StammdatenController {

    private final KundeService kundeService;
    private final LieferantService lieferantService;
    private final KontoService kontoService;
    private final KontenplanRepository kontenplanRepo;
    private final KreditkontrollbereichRepository kkbRepo;
    private final KontengruppeRepository kontengruppeRepo;

    public StammdatenController(KundeService kundeService, LieferantService lieferantService,
                                 KontoService kontoService, KontenplanRepository kontenplanRepo,
                                 KreditkontrollbereichRepository kkbRepo,
                                 KontengruppeRepository kontengruppeRepo) {
        this.kundeService = kundeService;
        this.lieferantService = lieferantService;
        this.kontoService = kontoService;
        this.kontenplanRepo = kontenplanRepo;
        this.kkbRepo = kkbRepo;
        this.kontengruppeRepo = kontengruppeRepo;
    }

    @GetMapping("/kunden")
    public ResponseEntity<List<Kunde>> kunden() { return ResponseEntity.ok(kundeService.alle()); }

    /**
     * ÄNDERUNG (Nebenbuch-Feature): das Abstimmkonto wird jetzt validiert
     * (muss existieren und vom Kontotyp AKTIV sein), statt eine beliebige
     * Zahl unkontrolliert zu übernehmen. Sonst könnte ein Debitor auf ein
     * nicht existierendes oder fachlich falsches Sachkonto zeigen und die
     * Nebenbuch-Abstimmung (/api/nebenbuch) würde stillschweigend ins Leere
     * laufen bzw. nie einen passenden Hauptbuch-Saldo finden.
     */
    @PostMapping("/kunden")
    public ResponseEntity<Kunde> kundeAnlegen(@Valid @RequestBody KundeRequest req) {
        Konto abstimmkonto = kontoService.byNummer(req.getAbstimmkonto());
        if (abstimmkonto.getKontotyp() != Konto.Kontotyp.AKTIV) {
            throw new IllegalArgumentException(
                    "Abstimmkonto " + req.getAbstimmkonto() + " (" + abstimmkonto.getKontobezeichnung() +
                    ") muss vom Kontotyp AKTIV sein (Forderungen), ist aber " + abstimmkonto.getKontotyp());
        }

        Kunde k = new Kunde(req.getFirmenname(), req.getAnsprechpartner(), req.getEmail(), req.getAdresse());
        k.setAbstimmkonto(req.getAbstimmkonto());
        if (req.getKreditkontrollbereichId() != null) {
            Kreditkontrollbereich kkb = kkbRepo.findById(req.getKreditkontrollbereichId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Kreditkontrollbereich nicht gefunden: " + req.getKreditkontrollbereichId()));
            k.setKreditkontrollbereich(kkb);
        }
        return ResponseEntity.ok(kundeService.speichern(k));
    }

    @GetMapping("/lieferanten")
    public ResponseEntity<List<Lieferant>> lieferanten() { return ResponseEntity.ok(lieferantService.alle()); }

    /** ÄNDERUNG (Nebenbuch-Feature): analoge Abstimmkonto-Validierung wie beim Debitor,
     *  hier gegen Kontotyp PASSIV (Verbindlichkeiten). */
    @PostMapping("/lieferanten")
    public ResponseEntity<Lieferant> lieferantAnlegen(@RequestBody Lieferant l) {
        Integer abstimmkontoNr = l.getAbstimmkonto() != null ? l.getAbstimmkonto() : 300000;
        Konto abstimmkonto = kontoService.byNummer(abstimmkontoNr);
        if (abstimmkonto.getKontotyp() != Konto.Kontotyp.PASSIV) {
            throw new IllegalArgumentException(
                    "Abstimmkonto " + abstimmkontoNr + " (" + abstimmkonto.getKontobezeichnung() +
                    ") muss vom Kontotyp PASSIV sein (Verbindlichkeiten), ist aber " + abstimmkonto.getKontotyp());
        }
        l.setAbstimmkonto(abstimmkontoNr);
        return ResponseEntity.ok(lieferantService.speichern(l));
    }

    @GetMapping("/konten")
    public ResponseEntity<List<Konto>> konten() { return ResponseEntity.ok(kontoService.alle()); }

    /**
     * ÄNDERUNG (Kontengruppen-Feature): löst zusätzlich zum Kontenplan jetzt
     * auch die Kontengruppe auf. Wird req.getKontengruppeNr() explizit
     * angegeben, wird geprüft, dass die Kontonummer tatsächlich in deren
     * Nummernkreis liegt (SAP-Verhalten: Kontengruppe steuert den
     * Nummernkreis verbindlich, siehe FS00). Ohne Angabe wird die passende
     * Kontengruppe automatisch über den Nummernkreis ermittelt.
     */
    @PostMapping("/konten")
    public ResponseEntity<Konto> kontoAnlegen(@Valid @RequestBody KontoRequest req) {
        Kontenplan kontenplan = kontenplanRepo.findByKontenplanNr(req.getKontenplanNr())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Kontenplan nicht gefunden: " + req.getKontenplanNr() +
                        " (bitte zuerst unter Organisationsstruktur anlegen)"));

        Konto k = new Konto(req.getKontonummer(), req.getKontobezeichnung(), req.getKontotyp());
        k.setKontenplan(kontenplan);
        k.setKontengruppe(resolveKontengruppe(req.getKontengruppeNr(), req.getKontonummer()));

        return ResponseEntity.ok(kontoService.speichern(k));
    }

    private Kontengruppe resolveKontengruppe(String gruppenNr, Integer kontonummer) {
        if (gruppenNr != null && !gruppenNr.isBlank()) {
            Kontengruppe kg = kontengruppeRepo.findByGruppenNr(gruppenNr)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Kontengruppe nicht gefunden: " + gruppenNr +
                            " (bitte zuerst unter Organisationsstruktur anlegen)"));
            if (!kg.enthaelt(kontonummer)) {
                throw new IllegalArgumentException(
                        "Kontonummer " + kontonummer + " liegt nicht im Nummernkreis der Kontengruppe " +
                        kg.getGruppenNr() + " (" + kg.getVonNummer() + "–" + kg.getBisNummer() + ").");
            }
            return kg;
        }
        // Keine Gruppe angegeben: automatisch über den Nummernkreis ermitteln.
        List<Kontengruppe> treffer = kontengruppeRepo.findPassendeGruppe(kontonummer);
        return treffer.isEmpty() ? null : treffer.get(0);
    }
}