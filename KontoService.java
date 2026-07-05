package com.example.demo.service;

import com.example.demo.entity.Konto;
import com.example.demo.repository.KontoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service
@Transactional
public class KontoService {
    private final KontoRepository repo;
    public KontoService(KontoRepository repo) { this.repo = repo; }
    public List<Konto> alle() { return repo.findAllSorted(); }
    public Konto byId(Long id) { return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Konto nicht gefunden: " + id)); }
    public Konto byNummer(Integer nr) { return repo.findByKontonummer(nr).orElseThrow(() -> new IllegalArgumentException("Konto nicht gefunden: " + nr)); }
    public Konto speichern(Konto k) { return repo.save(k); }
    public List<Konto> byTyp(Konto.Kontotyp typ) { return repo.findByKontotypOrderByKontonummer(typ); }
}