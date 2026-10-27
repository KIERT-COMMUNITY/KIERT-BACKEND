package com.kiert.backend.features.bienvenida.repository;

import com.kiert.backend.entity.HistorialBienvenida;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HistorialBienvenidaRepository
        extends JpaRepository<HistorialBienvenida, Long> {

    Page<HistorialBienvenida> findAllByOrderByFechaCambioDesc(Pageable pageable);
}