package com.example.demo.service;

import com.example.demo.entity.Kunde;
import com.example.demo.repository.KundeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service
@Transactional
public class KundeService {
    private final KundeRepository repo;
    public KundeService(KundeRepository repo) { this.repo = repo; }
    public List<Kunde> alle() { return repo.findAll(); }
    public Kunde byId(Long id) { return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Kunde nicht gefunden: " + id)); }
    public Kunde speichern(Kunde k) { return repo.save(k); }
    public Kunde aktualisieren(Long id, Kunde neu) {
        Kunde k = byId(id);
        k.setFirmenname(neu.getFirmenname()); k.setAnsprechpartner(neu.getAnsprechpartner());
        k.setEmail(neu.getEmail()); k.setAdresse(neu.getAdresse());
        return repo.save(k);
    }
    public List<Kunde> suchen(String name) { return repo.findByFirmennameContainingIgnoreCase(name); }
}
