package com.example.demo.controller;

import com.example.demo.dto.BuchungskreisRequest;
import com.example.demo.dto.KontengruppeRequest;
import com.example.demo.dto.KreditkontrollbereichRequest;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/orgstruktur")
public class OrgStrukturController {

    private final MandantRepository mandantRepo;
    private final BuchungskreisRepository buchungskreisRepo;
    private final GeschaeftsbereichRepository geschaeftsbereichRepo;
    private final KreditkontrollbereichRepository kkbRepo;
    private final KontenplanRepository kontenplanRepo;
    private final KontengruppeRepository kontengruppeRepo;

    public OrgStrukturController(MandantRepository mandantRepo, BuchungskreisRepository buchungskreisRepo,
                                  GeschaeftsbereichRepository geschaeftsbereichRepo,
                                  KreditkontrollbereichRepository kkbRepo,
                                  KontenplanRepository kontenplanRepo,
                                  KontengruppeRepository kontengruppeRepo) {
        this.mandantRepo = mandantRepo;
        this.buchungskreisRepo = buchungskreisRepo;
        this.geschaeftsbereichRepo = geschaeftsbereichRepo;
        this.kkbRepo = kkbRepo;
        this.kontenplanRepo = kontenplanRepo;
        this.kontengruppeRepo = kontengruppeRepo;
    }

    // ── MANDANT ──────────────────────────────────────────────
    @GetMapping("/mandanten")
    public ResponseEntity<List<Mandant>> mandanten() { return ResponseEntity.ok(mandantRepo.findAll()); }

    @PostMapping("/mandanten")
    public ResponseEntity<Mandant> mandantAnlegen(@Valid @RequestBody Mandant m) {
        if (m.getMandantNr() != null && mandantRepo.findByMandantNr(m.getMandantNr()).isPresent()) {
            throw new IllegalArgumentException("Mandant-Nr bereits vergeben: " + m.getMandantNr());
        }
        return ResponseEntity.ok(mandantRepo.save(m));
    }

    // ── BUCHUNGSKREIS ────────────────────────────────────────
    @GetMapping("/buchungskreise")
    public ResponseEntity<List<Buchungskreis>> buchungskreise() { return ResponseEntity.ok(buchungskreisRepo.findAll()); }

    @PostMapping("/buchungskreise")
    public ResponseEntity<Buchungskreis> buchungskreisAnlegen(@Valid @RequestBody BuchungskreisRequest req) {
        if (buchungskreisRepo.findByBuchungskreisNr(req.getBuchungskreisNr()).isPresent()) {
            throw new IllegalArgumentException("Buchungskreis-Nr bereits vergeben: " + req.getBuchungskreisNr());
        }

        Mandant mandant = mandantRepo.findById(req.getMandantId())
                .orElseThrow(() -> new IllegalArgumentException("Mandant nicht gefunden: " + req.getMandantId()));

        Buchungskreis b = new Buchungskreis(
                req.getBuchungskreisNr(),
                req.getBezeichnung(),
                req.getLand(),
                (req.getWaehrung() == null || req.getWaehrung().isBlank()) ? "EUR" : req.getWaehrung(),
                mandant);

        if (req.getKontenplanId() != null) {
            Kontenplan kp = kontenplanRepo.findById(req.getKontenplanId())
                    .orElseThrow(() -> new IllegalArgumentException("Kontenplan nicht gefunden: " + req.getKontenplanId()));
            b.setKontenplan(kp);
        }

        if (req.getKkbId() != null) {
            Kreditkontrollbereich kkb = kkbRepo.findById(req.getKkbId())
                    .orElseThrow(() -> new IllegalArgumentException("Kreditkontrollbereich nicht gefunden: " + req.getKkbId()));
            b.setKreditkontrollbereich(kkb);
        }

        return ResponseEntity.ok(buchungskreisRepo.save(b));
    }

    // ── GESCHÄFTSBEREICH ─────────────────────────────────────
    @GetMapping("/geschaeftsbereiche")
    public ResponseEntity<List<Geschaeftsbereich>> geschaeftsbereiche() { return ResponseEntity.ok(geschaeftsbereichRepo.findAll()); }

    @PostMapping("/geschaeftsbereiche")
    public ResponseEntity<Geschaeftsbereich> geschaeftsbereichAnlegen(@RequestBody Geschaeftsbereich g) {
        return ResponseEntity.ok(geschaeftsbereichRepo.save(g));
    }

    // ── KREDITKONTROLLBEREICH ────────────────────────────────
    @GetMapping("/kreditkontrollbereiche")
    public ResponseEntity<List<Kreditkontrollbereich>> kkb() { return ResponseEntity.ok(kkbRepo.findAll()); }

    @PostMapping("/kreditkontrollbereiche")
    public ResponseEntity<Kreditkontrollbereich> kkbAnlegen(@Valid @RequestBody KreditkontrollbereichRequest req) {
        if (kkbRepo.findAll().stream().anyMatch(k -> k.getKkbNr().equalsIgnoreCase(req.getKkbNr()))) {
            throw new IllegalArgumentException("KKB-Nr bereits vergeben: " + req.getKkbNr());
        }
        Kreditkontrollbereich kkb = new Kreditkontrollbereich(
                req.getKkbNr(), req.getBezeichnung(),
                (req.getWaehrung() == null || req.getWaehrung().isBlank()) ? "EUR" : req.getWaehrung(),
                req.getKreditlimit());
        return ResponseEntity.ok(kkbRepo.save(kkb));
    }

    @GetMapping("/kontenplaene")
    public ResponseEntity<List<Kontenplan>> kontenplaene() { return ResponseEntity.ok(kontenplanRepo.findAll()); }

    // ── KONTENGRUPPEN (SAP Account Group, T077S) ─────────────
    // ÄNDERUNG (Kontengruppen-Feature): neue Endpunkte für die Verwaltung der
    // Kontengruppen (GL00–GL05), die Sachkonten über einen Nummernkreis
    // klassifizieren – siehe Kontengruppe.java-Javadoc zur Abgrenzung vom
    // Kontenplan.
    @GetMapping("/kontengruppen")
    public ResponseEntity<List<Kontengruppe>> kontengruppen() { return ResponseEntity.ok(kontengruppeRepo.findAll()); }

    @PostMapping("/kontengruppen")
    public ResponseEntity<Kontengruppe> kontengruppeAnlegen(@Valid @RequestBody KontengruppeRequest req) {
        if (kontengruppeRepo.findByGruppenNr(req.getGruppenNr()).isPresent()) {
            throw new IllegalArgumentException("Kontengruppen-Nr bereits vergeben: " + req.getGruppenNr());
        }
        if (req.getBisNummer() < req.getVonNummer()) {
            throw new IllegalArgumentException(
                    "bisNummer (" + req.getBisNummer() + ") muss größer oder gleich vonNummer (" +
                    req.getVonNummer() + ") sein.");
        }
        boolean ueberlappt = kontengruppeRepo.findAll().stream().anyMatch(kg ->
                req.getVonNummer() <= kg.getBisNummer() && req.getBisNummer() >= kg.getVonNummer());
        if (ueberlappt) {
            throw new IllegalArgumentException(
                    "Nummernkreis " + req.getVonNummer() + "–" + req.getBisNummer() +
                    " überschneidet sich mit einer bereits bestehenden Kontengruppe.");
        }
        Kontengruppe kg = new Kontengruppe(req.getGruppenNr(), req.getBezeichnung(),
                req.getVonNummer(), req.getBisNummer());
        return ResponseEntity.ok(kontengruppeRepo.save(kg));
    }
}