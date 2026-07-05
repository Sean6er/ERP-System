package com.example.demo.repository;

import com.example.demo.entity.Buchungskreis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository
public interface BuchungskreisRepository extends JpaRepository<Buchungskreis, Long> {
    Optional<Buchungskreis> findByBuchungskreisNr(String buchungskreisNr);
}