package com.example.demo.repository;


import com.example.demo.entity.Buchung;
import com.example.demo.entity.Buchung.SollHaben;
import com.example.demo.entity.Konto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BuchungRepository extends JpaRepository<Buchung, Long> {

    Optional<Buchung> findByBelegnummer(Long belegnummer);

    List<Buchung> findByKontoOrderByBuchungsdatumDesc(Konto konto);

    List<Buchung> findByBuchungsdatumBetweenOrderByBuchungsdatumAsc(
            LocalDate von, LocalDate bis);

    List<Buchung> findBySollHabenAndStorniertFalseOrderByBuchungsdatumDesc(SollHaben sollHaben);

    List<Buchung> findByStorniertFalseOrderByBelegnummerDesc();

    /**
     * ÄNDERUNG: Buchungskreis-scoped Pendant zu findByKontoOrderByBuchungsdatumDesc().
     * Basis für die Buchungskreis-gefilterte Sachkontensaldenanzeige und für die
     * Bilanz-/GuV-/Cashflow-Berechnung, wenn eine buchungskreisNr übergeben wird.
     */
    List<Buchung> findByKontoAndBuchungskreis_BuchungskreisNrAndStorniertFalse(
            Konto konto, String buchungskreisNr);

    /**
     * ÄNDERUNG: Buchungskreis-scoped Pendant zu
     * findByBuchungsdatumBetweenOrderByBuchungsdatumAsc() – Basis für die
     * 30-Tage-Kennzahlen der Cashflow-Berechnung UND für den Journal-Zeitraum-
     * Filter, jeweils je Buchungskreis.
     */
    List<Buchung> findByBuchungsdatumBetweenAndBuchungskreis_BuchungskreisNrOrderByBuchungsdatumAsc(
            LocalDate von, LocalDate bis, String buchungskreisNr);

    // Nächste freie Belegnummer (SAP-Belegprinzip: eindeutig, aufsteigend)
    @Query("SELECT COALESCE(MAX(b.belegnummer), 100000000L) + 1 FROM Buchung b")
    Long naechsteBelegnummer();

    // Alle Buchungen für ein Konto in einem Zeitraum (Sachkontensaldenanzeige)
    @Query("""
        SELECT b FROM Buchung b
        WHERE b.konto = :konto
          AND b.buchungsdatum BETWEEN :von AND :bis
          AND b.storniert = false
        ORDER BY b.buchungsdatum ASC
        """)
    List<Buchung> findByKontoUndZeitraum(
            @Param("konto") Konto konto,
            @Param("von") LocalDate von,
            @Param("bis") LocalDate bis);

    // Journal – alle aktiven Buchungen
    @Query("""
        SELECT b FROM Buchung b
        WHERE b.storniert = false
        ORDER BY b.belegnummer DESC
        """)
    List<Buchung> findJournal();

    /** ÄNDERUNG: Journal, eingeschränkt auf einen Buchungskreis. */
    @Query("""
        SELECT b FROM Buchung b
        WHERE b.storniert = false AND b.buchungskreis.buchungskreisNr = :buchungskreisNr
        ORDER BY b.belegnummer DESC
        """)
    List<Buchung> findJournalByBuchungskreis(@Param("buchungskreisNr") String buchungskreisNr);

    // Audit-Trail: alle Buchungen inkl. Stornos zu einer Belegnummer
    @Query("""
        SELECT b FROM Buchung b
        WHERE b.belegnummer = :nr OR b.stornoVon.belegnummer = :nr
        ORDER BY b.erfassungsdatum ASC
        """)
    List<Buchung> findAuditTrailByBelegnummer(@Param("nr") Long belegnummer);
}