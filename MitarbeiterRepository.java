package com.example.demo.repository;

import com.example.demo.entity.Mitarbeiter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface MitarbeiterRepository extends JpaRepository<Mitarbeiter, Long> {
    List<Mitarbeiter> findByRolle(String rolle);
    List<Mitarbeiter> findByNachnameContainingIgnoreCase(String name);
}