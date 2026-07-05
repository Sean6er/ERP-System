package com.example.demo.repository;

import com.example.demo.entity.Lieferant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface LieferantRepository extends JpaRepository<Lieferant, Long> {
    List<Lieferant> findByFirmennameContainingIgnoreCase(String name);

    /** ÄNDERUNG (Nebenbuch-Feature): Basis für den Abstimmkonto-Schutz in
     *  BuchungsService – prüft, ob ein Sachkonto als Kreditoren-Abstimmkonto
     *  verwendet wird. */
    boolean existsByAbstimmkonto(Integer abstimmkonto);
}