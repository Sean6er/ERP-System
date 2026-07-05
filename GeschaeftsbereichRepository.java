package com.example.demo.repository;

import com.example.demo.entity.Geschaeftsbereich;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository
public interface GeschaeftsbereichRepository extends JpaRepository<Geschaeftsbereich, Long> {
    Optional<Geschaeftsbereich> findByBereichNr(String bereichNr);
}