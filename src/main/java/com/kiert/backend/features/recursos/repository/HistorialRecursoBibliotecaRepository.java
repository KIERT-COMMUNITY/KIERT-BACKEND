// src/main/java/com/kiert/backend/features/recursos/repository/HistorialRecursoBibliotecaRepository.java
package com.kiert.backend.features.recursos.repository;

import com.kiert.backend.entity.HistorialRecursoBiblioteca;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialRecursoBibliotecaRepository extends JpaRepository<HistorialRecursoBiblioteca, Long> {
    List<HistorialRecursoBiblioteca> findByRecursoIdOrderByFechaAccionDesc(Long recursoId);
    List<HistorialRecursoBiblioteca> findByUsuarioIdOrderByFechaAccionDesc(Long usuarioId);
}