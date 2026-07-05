package com.example.demo.service;

import com.example.demo.entity.Lieferant;
import com.example.demo.repository.LieferantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service
@Transactional
public class LieferantService {
    private final LieferantRepository repo;
    public LieferantService(LieferantRepository repo) { this.repo = repo; }
    public List<Lieferant> alle() { return repo.findAll(); }
    public Lieferant byId(Long id) { return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Lieferant nicht gefunden: " + id)); }
    public Lieferant speichern(Lieferant l) { return repo.save(l); }
    public List<Lieferant> suchen(String name) { return repo.findByFirmennameContainingIgnoreCase(name); }
}
