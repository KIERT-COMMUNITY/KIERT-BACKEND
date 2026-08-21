package com.kiert.backend.repository;

import com.kiert.backend.entity.FondoPersonalizado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FondoRepository extends JpaRepository<FondoPersonalizado, Long> {
    List<FondoPersonalizado> findByActivoTrue();
}