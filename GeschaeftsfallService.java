package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service
@Transactional
public class GeschaeftsfallService {
    private final GeschaeftsfallRepository repo;
    private final KundeRepository kundeRepo;
    private final MitarbeiterRepository mitarbeiterRepo;
    public GeschaeftsfallService(GeschaeftsfallRepository repo, KundeRepository kundeRepo, MitarbeiterRepository mitarbeiterRepo) {
        this.repo = repo; this.kundeRepo = kundeRepo; this.mitarbeiterRepo = mitarbeiterRepo;
    }
    public List<Geschaeftsfall> alle() { return repo.findAll(); }
    public Geschaeftsfall byId(Long id) { return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("GeschÃ¤ftsfall nicht gefunden: " + id)); }
    public Geschaeftsfall erstellen(Geschaeftsfall g, Long kundeId, Long mitarbeiterId) {
        if (kundeId != null) g.setKunde(kundeRepo.findById(kundeId).orElseThrow());
        if (mitarbeiterId != null) g.setMitarbeiter(mitarbeiterRepo.findById(mitarbeiterId).orElseThrow());
        return repo.save(g);
    }
    public Geschaeftsfall abschliessen(Long id) {
        Geschaeftsfall g = byId(id);
        g.setStatus(Geschaeftsfall.GeschaeftsfallStatus.ABGESCHLOSSEN);
        return repo.save(g);
    }
}