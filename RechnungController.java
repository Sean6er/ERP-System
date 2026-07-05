package com.example.demo.controller;

import com.example.demo.entity.Rechnung;
import com.example.demo.service.RechnungService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * RechnungController – offene-Posten-Verwaltung (Debitoren- UND
 * Kreditorenrechnungen).
 *
 * ÄNDERUNG: beide Listen-Endpunkte akzeptieren jetzt optional
 * ?buchungskreisNr=DE00, aufbauend auf Geschaeftsfall.buchungskreis.
 */
@RestController
@RequestMapping("/api/rechnungen")
public class RechnungController {

    private final RechnungService rechnungService;

    public RechnungController(RechnungService rechnungService) {
        this.rechnungService = rechnungService;
    }

    @GetMapping
    public ResponseEntity<List<Rechnung>> alle(
            @RequestParam(name = "buchungskreisNr", required = false) String buchungskreisNr) {
        return ResponseEntity.ok(rechnungService.alle(buchungskreisNr));
    }

    /** Offene Posten (Status OFFEN oder UEBERFAELLIG), Debitoren UND Kreditoren gemeinsam. */
    @GetMapping("/offen")
    public ResponseEntity<List<Rechnung>> offenePosten(
            @RequestParam(name = "buchungskreisNr", required = false) String buchungskreisNr) {
        rechnungService.ueberfaelligePruefen();
        return ResponseEntity.ok(rechnungService.offenePosten(buchungskreisNr));
    }

    @PostMapping("/ueberfaellig-pruefen")
    public ResponseEntity<Map<String, Object>> ueberfaelligPruefen() {
        int anzahl = rechnungService.ueberfaelligePruefen();
        return ResponseEntity.ok(Map.of("aktualisierteRechnungen", anzahl));
    }
}