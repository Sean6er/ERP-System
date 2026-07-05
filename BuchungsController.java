package com.example.demo.controller;

import com.example.demo.dto.BuchungsRequest;
import com.example.demo.entity.Buchung;
import com.example.demo.service.BuchungsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/buchungen")
public class BuchungsController {

    private final BuchungsService buchungsService;

    public BuchungsController(BuchungsService buchungsService) {
        this.buchungsService = buchungsService;
    }

    @PostMapping("/buchen")
    public ResponseEntity<List<Buchung>> buchen(@Valid @RequestBody BuchungsRequest req) {
        // ÄNDERUNG (Nebenbuch-Feature): manuelle Direktbuchungen auf
        // Abstimmkonten (Debitoren/Kreditoren) sind nicht erlaubt – siehe
        // BuchungsService.pruefeKeineDirekteAbstimmkontoBuchung().
        buchungsService.pruefeKeineDirekteAbstimmkontoBuchung(req.getSollKontoNr(), req.getHabenKontoNr());

        List<Buchung> result = buchungsService.bucheSollHaben(
            req.getSollKontoNr(), req.getHabenKontoNr(), req.getBetrag(),
            req.getBuchungstext(), req.getBuchungsdatum(), req.getErfasstVon(),
            req.getRechnungId(), req.getBuchungskreisNr());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/journal")
    public ResponseEntity<List<Buchung>> journal(
            @RequestParam(name = "buchungskreisNr", required = false) String buchungskreisNr) {
        return ResponseEntity.ok(buchungsService.getJournal(buchungskreisNr));
    }

    @PostMapping("/{id}/storno")
    public ResponseEntity<List<Buchung>> storno(
            @PathVariable("id") Long id,
            @RequestParam(name = "erfasstVon", defaultValue = "System") String erfasstVon) {
        return ResponseEntity.ok(buchungsService.storniereBuchung(id, erfasstVon));
    }

    @GetMapping("/zeitraum")
    public ResponseEntity<List<Buchung>> zeitraum(
            @RequestParam("von") String von,
            @RequestParam("bis") String bis,
            @RequestParam(name = "buchungskreisNr", required = false) String buchungskreisNr) {
        return ResponseEntity.ok(buchungsService.getBuchungenNachZeitraum(
            LocalDate.parse(von), LocalDate.parse(bis), buchungskreisNr));
    }

    @GetMapping("/audit/{belegnummer}")
    public ResponseEntity<List<Buchung>> auditTrail(@PathVariable("belegnummer") Long belegnummer) {
        return ResponseEntity.ok(buchungsService.getAuditTrail(belegnummer));
    }
}