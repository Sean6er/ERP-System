package com.example.demo.controller;

import com.example.demo.service.NebenbuchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * NebenbuchController – FI-AR/FI-AP Nebenbücher und ihre Abstimmung
 * gegen das Hauptbuch. Siehe NebenbuchService für die fachliche Logik.
 */
@RestController
@RequestMapping("/api/nebenbuch")
public class NebenbuchController {

    private final NebenbuchService nebenbuchService;

    public NebenbuchController(NebenbuchService nebenbuchService) {
        this.nebenbuchService = nebenbuchService;
    }

    @GetMapping("/debitoren")
    public ResponseEntity<List<Map<String, Object>>> debitoren(
            @RequestParam(name = "buchungskreisNr", required = false) String buchungskreisNr) {
        return ResponseEntity.ok(nebenbuchService.debitorenSalden(buchungskreisNr));
    }

    @GetMapping("/kreditoren")
    public ResponseEntity<List<Map<String, Object>>> kreditoren(
            @RequestParam(name = "buchungskreisNr", required = false) String buchungskreisNr) {
        return ResponseEntity.ok(nebenbuchService.kreditorenSalden(buchungskreisNr));
    }

    @GetMapping("/abstimmung")
    public ResponseEntity<Map<String, Object>> abstimmung(
            @RequestParam(name = "buchungskreisNr", required = false) String buchungskreisNr) {
        return ResponseEntity.ok(nebenbuchService.abstimmung(buchungskreisNr));
    }
}