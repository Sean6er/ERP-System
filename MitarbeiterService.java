package com.example.demo.service;

import com.example.demo.entity.Mitarbeiter;
import com.example.demo.repository.MitarbeiterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service
@Transactional
public class MitarbeiterService {
    private final MitarbeiterRepository repo;
    public MitarbeiterService(MitarbeiterRepository repo) { this.repo = repo; }
    public List<Mitarbeiter> alle() { return repo.findAll(); }
    public Mitarbeiter byId(Long id) { return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Mitarbeiter nicht gefunden: " + id)); }
    public Mitarbeiter speichern(Mitarbeiter m) { return repo.save(m); }
}