package com.example.demo.service;

import com.example.demo.entity.Produkt;
import com.example.demo.entity.Lieferant;
import com.example.demo.repository.ProduktRepository;
import com.example.demo.repository.LieferantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service
@Transactional
public class ProduktService {
    private final ProduktRepository repo;
    private final LieferantRepository lieferantRepo;
    public ProduktService(ProduktRepository repo, LieferantRepository lieferantRepo) {
        this.repo = repo; this.lieferantRepo = lieferantRepo;
    }
    public List<Produkt> alle() { return repo.findAll(); }
    public Produkt byId(Long id) { return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Produkt nicht gefunden: " + id)); }
    public Produkt speichern(Produkt p, Long lieferantId) {
        Lieferant l = lieferantRepo.findById(lieferantId).orElseThrow(() -> new IllegalArgumentException("Lieferant nicht gefunden: " + lieferantId));
        p.setLieferant(l); return repo.save(p);
    }
    public List<Produkt> niedrigerBestand(Integer grenze) { return repo.findByLagerbestandLessThan(grenze); }
    public List<Produkt> suchen(String name) { return repo.findByBezeichnungContainingIgnoreCase(name); }
}
