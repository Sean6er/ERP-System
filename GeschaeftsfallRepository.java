package com.example.demo.repository;

import com.example.demo.entity.Geschaeftsfall;
import com.example.demo.entity.Geschaeftsfall.GeschaeftsfallTyp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
@Repository
public interface GeschaeftsfallRepository extends JpaRepository<Geschaeftsfall, Long> {
    List<Geschaeftsfall> findByTypOrderByDatumDesc(GeschaeftsfallTyp typ);
    List<Geschaeftsfall> findByDatumBetween(LocalDate von, LocalDate bis);

    /** ÄNDERUNG: Basis für eine künftige Geschäftsfall-Liste je Buchungskreis. */
    List<Geschaeftsfall> findByBuchungskreis_BuchungskreisNr(String buchungskreisNr);
}