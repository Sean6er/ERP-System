package com.example.demo.repository;

import com.example.demo.entity.Kontenplan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface KontenplanRepository extends JpaRepository<Kontenplan, Long> {
    Optional<Kontenplan> findByKontenplanNr(String kontenplanNr);
}
