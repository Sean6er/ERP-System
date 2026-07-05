package com.example.demo.repository;

import com.example.demo.entity.Kontengruppe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KontengruppeRepository extends JpaRepository<Kontengruppe, Long> {

    Optional<Kontengruppe> findByGruppenNr(String gruppenNr);

    /**
     * Findet die Kontengruppe(n), in deren Nummernkreis eine Kontonummer fällt.
     * Basis für die automatische Kontengruppen-Ermittlung beim Kontoanlegen,
     * wenn keine Gruppe explizit angegeben wurde.
     */
    @Query("SELECT k FROM Kontengruppe k WHERE :kontonummer BETWEEN k.vonNummer AND k.bisNummer")
    List<Kontengruppe> findPassendeGruppe(@Param("kontonummer") Integer kontonummer);
}