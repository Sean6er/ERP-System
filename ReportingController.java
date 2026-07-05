package com.example.demo.controller;

import com.example.demo.service.ReportingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * ÄNDERUNG: alle Endpunkte akzeptieren jetzt optional ?buchungskreisNr=DE00
 * für Buchungskreis-scoped Reporting. Ohne Parameter unverändertes globales
 * Verhalten (Rückwärtskompatibilität für bestehende Aufrufer).
 */
@RestController
@RequestMapping("/api/reporting")
public class ReportingController {

    private final ReportingService reportingService;

    public ReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/bilanz")
    public ResponseEntity<Map<String, Object>> bilanz(
            @RequestParam(name = "buchungskreisNr", required = false) String buchungskreisNr) {
        return ResponseEntity.ok(reportingService.getBilanz(buchungskreisNr));
    }

    @GetMapping("/guv")
    public ResponseEntity<Map<String, Object>> guv(
            @RequestParam(name = "buchungskreisNr", required = false) String buchungskreisNr) {
        return ResponseEntity.ok(reportingService.getGuV(buchungskreisNr));
    }

    @GetMapping("/cashflow")
    public ResponseEntity<Map<String, Object>> cashflow(
            @RequestParam(name = "buchungskreisNr", required = false) String buchungskreisNr) {
        return ResponseEntity.ok(reportingService.getCashFlow(buchungskreisNr));
    }

    @GetMapping("/sachkonto/{kontonummer}")
    public ResponseEntity<Map<String, Object>> sachkonto(
            @PathVariable("kontonummer") Integer kontonummer,
            @RequestParam(name = "buchungskreisNr", required = false) String buchungskreisNr) {
        return ResponseEntity.ok(reportingService.getSachkontensaldo(kontonummer, buchungskreisNr));
    }

    @GetMapping("/konten")
    public ResponseEntity<?> kontenUebersicht(
            @RequestParam(name = "buchungskreisNr", required = false) String buchungskreisNr) {
        return ResponseEntity.ok(reportingService.getKontenUebersicht(buchungskreisNr));
    }
}