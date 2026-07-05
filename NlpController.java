package com.example.demo.controller;

import com.example.demo.dto.NlpBuchungsRequest;
import com.example.demo.service.NlpBuchungsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/nlp")
public class NlpController {

    private final NlpBuchungsService nlpService;

    public NlpController(NlpBuchungsService nlpService) {
        this.nlpService = nlpService;
    }

    /** Analysiert Freitext, bucht NICHT – gibt nur Vorschlag zurück */
    @PostMapping("/analysiere")
    public ResponseEntity<Map<String, Object>> analysiere(
            @Valid @RequestBody NlpBuchungsRequest req) {
        Map<String, Object> vorschlag = nlpService.analysiereFreitext(req.getFreitext());
        return ResponseEntity.ok(vorschlag);
    }

    /** Analysiert und bucht direkt (direktBuchen=true) */
    @PostMapping("/buchen")
    public ResponseEntity<?> nlpBuchen(@Valid @RequestBody NlpBuchungsRequest req) {
        if (req.isDirektBuchen()) {
            return ResponseEntity.ok(
                nlpService.analysiereUndBuche(req.getFreitext(), req.getErfasstVon()));
        }
        return ResponseEntity.ok(nlpService.analysiereFreitext(req.getFreitext()));
    }
}