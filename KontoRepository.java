package com.example.demo.repository;

import com.example.demo.entity.Konto;
import com.example.demo.entity.Konto.Kontotyp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface KontoRepository extends JpaRepository<Konto, Long> {

    Optional<Konto> findByKontonummer(Integer kontonummer);

    List<Konto> findByKontotypOrderByKontonummer(Kontotyp kontotyp);

    List<Konto> findByKontobezeichnungContainingIgnoreCase(String name);

    /** ÄNDERUNG: Konten eines bestimmten Kontenplans finden (jetzt über echte FK statt String-Vergleich). */
    List<Konto> findByKontenplan_KontenplanNr(String kontenplanNr);

    // Bilanz: Summe Aktiva und Passiva
    @Query("SELECT SUM(k.saldo) FROM Konto k WHERE k.kontotyp = 'AKTIV'")
    BigDecimal sumSaldoAktiv();

    @Query("SELECT SUM(k.saldo) FROM Konto k WHERE k.kontotyp = 'PASSIV'")
    BigDecimal sumSaldoPassiv();

    // GuV: Erlöse und Aufwendungen
    @Query("SELECT SUM(k.saldo) FROM Konto k WHERE k.kontotyp = 'ERTRAG'")
    BigDecimal sumSaldoErtrag();

    @Query("SELECT SUM(k.saldo) FROM Konto k WHERE k.kontotyp = 'AUFWAND'")
    BigDecimal sumSaldoAufwand();

    // Alle Konten nach Typ, sortiert
    @Query("SELECT k FROM Konto k ORDER BY k.kontotyp, k.kontonummer")
    List<Konto> findAllSorted();
}
