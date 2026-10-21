// src/main/java/com/kiert/backend/repository/AuditoriaUsuarioRepository.java
package com.kiert.backend.repository;

import com.kiert.backend.entity.AuditoriaUsuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuditoriaUsuarioRepository extends JpaRepository<AuditoriaUsuario, Long> {

    /**
     * Registros de auditoría de un usuario específico.
     * Solo accesible por el propio usuario o un admin.
     */
    @Query("""
            SELECT a FROM AuditoriaUsuario a
            WHERE a.usuarioId = :usuarioId
            ORDER BY a.fechaEvento DESC
            """)
    Page<AuditoriaUsuario> findByUsuarioId(
            @Param("usuarioId") Long usuarioId,
            Pageable pageable
    );

    /**
     * Registros de auditoría por tipo de evento.
     */
    @Query("""
            SELECT a FROM AuditoriaUsuario a
            WHERE a.evento = :evento
            ORDER BY a.fechaEvento DESC
            """)
    Page<AuditoriaUsuario> findByEvento(
            @Param("evento") String evento,
            Pageable pageable
    );

    /**
     * Registros de auditoría en un rango de fechas.
     */
    @Query("""
            SELECT a FROM AuditoriaUsuario a
            WHERE a.fechaEvento BETWEEN :desde AND :hasta
            ORDER BY a.fechaEvento DESC
            """)
    Page<AuditoriaUsuario> findByRangoFechas(
            @Param("desde") Instant desde,
            @Param("hasta") Instant hasta,
            Pageable pageable
    );

    /**
     * Registros fallidos (para detectar ataques).
     * ✅ CORREGIDO: 'a.exito' (nombre real del atributo en la entidad).
     */
    @Query("""
            SELECT a FROM AuditoriaUsuario a
            WHERE a.exito = false
              AND a.fechaEvento >= :desde
            ORDER BY a.fechaEvento DESC
            """)
    List<AuditoriaUsuario> findEventosFallidosRecientes(@Param("desde") Instant desde);

    /**
     * Contar eventos por email en un rango (para detectar fuerza bruta).
     */
    @Query("""
            SELECT COUNT(a) FROM AuditoriaUsuario a
            WHERE a.email = :email
              AND a.evento = :evento
              AND a.fechaEvento >= :desde
            """)
    long contarEventosPorEmail(
            @Param("email") String email,
            @Param("evento") String evento,
            @Param("desde") Instant desde
    );

    /**
     * Eliminar registros antiguos (para @Scheduled).
     */
    @Modifying
    @Query("""
            DELETE FROM AuditoriaUsuario a
            WHERE a.fechaEvento < :limite
            """)
    int eliminarAnterioresA(@Param("limite") Instant limite);
}