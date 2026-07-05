package com.example.demo.repository;

import com.example.demo.entity.Produkt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface ProduktRepository extends JpaRepository<Produkt, Long> {
    List<Produkt> findByBezeichnungContainingIgnoreCase(String name);
    List<Produkt> findByLagerbestandLessThan(Integer grenze);
}
