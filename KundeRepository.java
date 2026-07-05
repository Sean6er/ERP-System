package com.example.demo.repository;

import com.example.demo.entity.Kunde;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface KundeRepository extends JpaRepository<Kunde, Long> {
    List<Kunde> findByFirmennameContainingIgnoreCase(String name);
    boolean existsByEmail(String email);

    /** ÄNDERUNG (Nebenbuch-Feature): Basis für den Abstimmkonto-Schutz in
     *  BuchungsService – prüft, ob ein Sachkonto als Debitoren-Abstimmkonto
     *  verwendet wird. */
    boolean existsByAbstimmkonto(Integer abstimmkonto);
}