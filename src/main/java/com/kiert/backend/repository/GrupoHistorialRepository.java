// src/main/java/com/kiert/backend/repository/GrupoHistorialRepository.java
package com.kiert.backend.repository;

import com.kiert.backend.entity.GrupoHistorial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GrupoHistorialRepository extends JpaRepository<GrupoHistorial, Long> {

    List<GrupoHistorial> findByGrupoIdOrderByFechaDesc(Long grupoId);
}