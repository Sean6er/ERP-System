package com.example.demo.repository;

import com.example.demo.entity.Geschaeftsfall;
import com.example.demo.entity.Rechnung;
import com.example.demo.entity.Rechnung.RechnungStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface RechnungRepository extends JpaRepository<Rechnung, Long> {
    List<Rechnung> findByStatus(RechnungStatus status);

    List<Rechnung> findByGeschaeftsfall(Geschaeftsfall geschaeftsfall);

    @Query("SELECT COALESCE(SUM(r.betrag), 0) FROM Rechnung r WHERE r.status = 'OFFEN' AND r.geschaeftsfall.typ = 'VERKAUF'")
    BigDecimal sumOffeneForderungen();

    @Query("SELECT COALESCE(SUM(r.betrag), 0) FROM Rechnung r WHERE r.status = 'UEBERFAELLIG' AND r.geschaeftsfall.typ = 'VERKAUF'")
    BigDecimal sumUeberfaelligeForderungen();

    @Query("SELECT COALESCE(SUM(r.betrag), 0) FROM Rechnung r WHERE r.status IN ('OFFEN','UEBERFAELLIG') AND r.geschaeftsfall.typ = 'EINKAUF'")
    BigDecimal sumOffeneVerbindlichkeiten();

    @Query("""
        SELECT COALESCE(SUM(r.betrag), 0) FROM Rechnung r
        WHERE r.status IN ('OFFEN','UEBERFAELLIG') AND r.geschaeftsfall.kunde.kundenId = :kundenId
        """)
    BigDecimal sumOffenerBetragFuerKunde(@Param("kundenId") Long kundenId);

    // ── ÄNDERUNG: Buchungskreis-scoped Pendants ──────────────────────────────
    //
    // Seit Geschaeftsfall eine echte buchungskreis-FK trägt (siehe
    // Geschaeftsfall.java-Javadoc), ist das ein einfacher Join über
    // r.geschaeftsfall.buchungskreis – kein EXISTS-Subquery-Umweg über
    // Buchung mehr nötig, und kein Risiko einer Doppelzählung (Many-to-One,
    // kein Fan-out).

    @Query("""
        SELECT COALESCE(SUM(r.betrag), 0) FROM Rechnung r
        WHERE r.status = 'OFFEN' AND r.geschaeftsfall.typ = 'VERKAUF'
          AND r.geschaeftsfall.buchungskreis.buchungskreisNr = :buchungskreisNr
        """)
    BigDecimal sumOffeneForderungen(@Param("buchungskreisNr") String buchungskreisNr);

    @Query("""
        SELECT COALESCE(SUM(r.betrag), 0) FROM Rechnung r
        WHERE r.status = 'UEBERFAELLIG' AND r.geschaeftsfall.typ = 'VERKAUF'
          AND r.geschaeftsfall.buchungskreis.buchungskreisNr = :buchungskreisNr
        """)
    BigDecimal sumUeberfaelligeForderungen(@Param("buchungskreisNr") String buchungskreisNr);

    @Query("""
        SELECT COALESCE(SUM(r.betrag), 0) FROM Rechnung r
        WHERE r.status IN ('OFFEN','UEBERFAELLIG') AND r.geschaeftsfall.typ = 'EINKAUF'
          AND r.geschaeftsfall.buchungskreis.buchungskreisNr = :buchungskreisNr
        """)
    BigDecimal sumOffeneVerbindlichkeiten(@Param("buchungskreisNr") String buchungskreisNr);

    /** ÄNDERUNG: alle Rechnungen (Debitoren + Kreditoren) eines Buchungskreises. */
    List<Rechnung> findByGeschaeftsfall_Buchungskreis_BuchungskreisNr(String buchungskreisNr);

    /** ÄNDERUNG: offene Posten (OFFEN/UEBERFAELLIG) eines Buchungskreises. */
    List<Rechnung> findByGeschaeftsfall_Buchungskreis_BuchungskreisNrAndStatusIn(
            String buchungskreisNr, List<RechnungStatus> stati);
}