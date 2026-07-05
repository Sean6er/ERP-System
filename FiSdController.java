package com.example.demo.controller;

import com.example.demo.dto.SdBuchungRequest;
import com.example.demo.service.FiSdIntegrationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/*
 
FiSdController – REST-Schnittstelle für FI↔SD-Integration (Vertrieb).*/
@RestController
@RequestMapping("/api/integration/sd")
public class FiSdController {

    private final FiSdIntegrationService sdService;

    public FiSdController(FiSdIntegrationService sdService) {
        this.sdService = sdService;
    }

    /* Bucht eine SD-Faktura (Forderungen / Umsatzerlöse) */
    @PostMapping("/faktura")
    public ResponseEntity<Map<String, Object>> faktura(@Valid @RequestBody SdBuchungRequest req) {
        return ResponseEntity.ok(sdService.bucheFaktura(
                req.getGeschaeftsfallId(), req.getBetrag(),
                req.getPartnername(), req.getErfasstVon()));
    }

    /** Bucht einen Zahlungseingang (Bank / Forderungen)**/
    @PostMapping("/zahlungseingang")
    public ResponseEntity<Map<String, Object>> zahlungseingang(@Valid @RequestBody SdBuchungRequest req) {
        return ResponseEntity.ok(sdService.bucheZahlungseingang(
                req.getGeschaeftsfallId(), req.getBetrag(),
                req.getPartnername(), req.getErfasstVon()));
    }

    /** Simuliert den vollständigen Verkaufsprozess (Faktura + Zahlungseingang) **/
    @PostMapping("/vollstaendig")
    public ResponseEntity<Map<String, Object>> vollstaendig(@Valid @RequestBody SdBuchungRequest req) {
        return ResponseEntity.ok(sdService.vollstaendigerVerkaufsprozess(
                req.getPartnername(), req.getBetrag(), req.getErfasstVon(), req.getBuchungskreisNr()));
    }
}