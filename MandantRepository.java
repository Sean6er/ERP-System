package com.example.demo.repository;

import com.example.demo.entity.Mandant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository
public interface MandantRepository extends JpaRepository<Mandant, Long> {
    Optional<Mandant> findByMandantNr(String mandantNr);
}