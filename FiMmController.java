package com.example.demo.controller;

import com.example.demo.dto.SdBuchungRequest;
import com.example.demo.service.FiMmIntegrationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * FiMmController – REST-Schnittstelle für FI↔MM-Integration (Einkauf/Lager).
 *
 * Nutzt dasselbe generische Request-DTO wie FiSdController
 * (Felder: geschaeftsfallId, betrag, partnername, erfasstVon, buchungskreisNr).
 */
@RestController
@RequestMapping("/api/integration/mm")
public class FiMmController {

    private final FiMmIntegrationService mmService;

    public FiMmController(FiMmIntegrationService mmService) {
        this.mmService = mmService;
    }

    /** Bucht einen Wareneingang (Materialaufwand / Verbindlichkeiten) */
    @PostMapping("/wareneingang")
    public ResponseEntity<Map<String, Object>> wareneingang(@Valid @RequestBody SdBuchungRequest req) {
        return ResponseEntity.ok(mmService.bucheWareneingang(
                req.getGeschaeftsfallId(), req.getBetrag(),
                req.getPartnername(), req.getErfasstVon()));
    }

    /** Bucht einen Zahlungsausgang (Verbindlichkeiten / Bank) */
    @PostMapping("/zahlungsausgang")
    public ResponseEntity<Map<String, Object>> zahlungsausgang(@Valid @RequestBody SdBuchungRequest req) {
        return ResponseEntity.ok(mmService.bucheZahlungsausgang(
                req.getGeschaeftsfallId(), req.getBetrag(),
                req.getPartnername(), req.getErfasstVon()));
    }

    /** Simuliert den vollständigen Einkaufsprozess (Wareneingang + Zahlungsausgang) */
    @PostMapping("/vollstaendig")
    public ResponseEntity<Map<String, Object>> vollstaendig(@Valid @RequestBody SdBuchungRequest req) {
        return ResponseEntity.ok(mmService.vollstaendigerEinkaufsprozess(
                req.getPartnername(), req.getBetrag(), req.getErfasstVon(), req.getBuchungskreisNr()));
    }
}