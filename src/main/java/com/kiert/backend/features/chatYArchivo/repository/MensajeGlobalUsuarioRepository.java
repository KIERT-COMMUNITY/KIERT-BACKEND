package com.kiert.backend.features.chatYArchivo.repository;

import com.kiert.backend.entity.MensajeGlobalUsuario;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MensajeGlobalUsuarioRepository extends JpaRepository<MensajeGlobalUsuario, Long> {

    @Query("""
            SELECT m FROM MensajeGlobalUsuario m
            JOIN FETCH m.emisor
            WHERE m.eliminado = false
            ORDER BY m.fechaEnvio ASC
            """)
    List<MensajeGlobalUsuario> findUltimos(Pageable pageable);

    @Query("""
            SELECT m FROM MensajeGlobalUsuario m
            JOIN FETCH m.emisor
            WHERE m.eliminado = false
              AND m.id > :desdeId
            ORDER BY m.fechaEnvio ASC
            """)
    List<MensajeGlobalUsuario> findDesdeId(@Param("desdeId") Long desdeId);

    long countByEliminadoFalse();
}