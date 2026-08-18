package com.kiert.backend.repository;

import com.kiert.backend.entity.MarcoPersonalizado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarcoRepository extends JpaRepository<MarcoPersonalizado, Long> {
    List<MarcoPersonalizado> findByActivoTrue();
    List<MarcoPersonalizado> findByUsuarioId(Long usuarioId);
}