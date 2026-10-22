// src/main/java/com/kiert/backend/repository/GrupoHistorialRepository.java
package com.kiert.backend.features.grupo.repository;

import com.kiert.backend.entity.GrupoHistorial;
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
public interface GrupoHistorialRepository extends JpaRepository<GrupoHistorial, Long> {

    @Query("""
            SELECT h FROM GrupoHistorial h
            LEFT JOIN FETCH h.usuario
            WHERE h.grupo.id = :grupoId
            ORDER BY h.fecha DESC
            """)
    List<GrupoHistorial> findByGrupoIdOrderByFechaDesc(@Param("grupoId") Long grupoId);

    @Query("""
            SELECT h FROM GrupoHistorial h
            LEFT JOIN FETCH h.usuario
            WHERE h.grupo.id = :grupoId
            ORDER BY h.fecha DESC
            """)
    Page<GrupoHistorial> findByGrupoIdPaginado(
            @Param("grupoId") Long grupoId,
            Pageable pageable
    );

    @Query("""
            SELECT h FROM GrupoHistorial h
            LEFT JOIN FETCH h.usuario
            WHERE h.grupo.id = :grupoId
              AND h.accion = :accion
            ORDER BY h.fecha DESC
            """)
    List<GrupoHistorial> findByGrupoIdAndAccion(
            @Param("grupoId") Long grupoId,
            @Param("accion") String accion
    );

    @Query("""
            SELECT h.accion, COUNT(h)
            FROM GrupoHistorial h
            WHERE h.grupo.id = :grupoId
            GROUP BY h.accion
            ORDER BY COUNT(h) DESC
            """)
    List<Object[]> contarAccionesPorTipo(@Param("grupoId") Long grupoId);

    @Query("""
            SELECT h FROM GrupoHistorial h
            LEFT JOIN FETCH h.usuario
            WHERE h.grupo.id = :grupoId
              AND h.usuario.id = :usuarioId
            ORDER BY h.fecha DESC
            """)
    List<GrupoHistorial> findByGrupoIdAndUsuarioId(
            @Param("grupoId") Long grupoId,
            @Param("usuarioId") Long usuarioId
    );

    @Query("""
            SELECT h FROM GrupoHistorial h
            LEFT JOIN FETCH h.grupo
            WHERE h.usuario.id = :usuarioId
            ORDER BY h.fecha DESC
            """)
    Page<GrupoHistorial> findByUsuarioId(
            @Param("usuarioId") Long usuarioId,
            Pageable pageable
    );

    @Query("""
            SELECT h FROM GrupoHistorial h
            LEFT JOIN FETCH h.usuario
            WHERE h.grupo.id = :grupoId
              AND h.fecha >= :desde
              AND h.fecha < :hasta
            ORDER BY h.fecha DESC
            """)
    List<GrupoHistorial> findByGrupoIdAndFechaEntre(
            @Param("grupoId") Long grupoId,
            @Param("desde") Instant desde,
            @Param("hasta") Instant hasta
    );

    @Query("""
            SELECT h FROM GrupoHistorial h
            LEFT JOIN FETCH h.usuario
            WHERE h.grupo.id = :grupoId
            ORDER BY h.fecha DESC
            """)
    List<GrupoHistorial> findUltimasAcciones(
            @Param("grupoId") Long grupoId,
            Pageable pageable
    );

    @Query("""
            SELECT h FROM GrupoHistorial h
            LEFT JOIN FETCH h.usuario
            WHERE h.grupo.id = :grupoId
              AND LOWER(h.detalle) LIKE LOWER(CONCAT('%', :query, '%'))
            ORDER BY h.fecha DESC
            """)
    Page<GrupoHistorial> buscarPorDetalle(
            @Param("grupoId") Long grupoId,
            @Param("query") String query,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(h) FROM GrupoHistorial h
            WHERE h.grupo.id = :grupoId
            """)
    long countAccionesDeGrupo(@Param("grupoId") Long grupoId);

    @Query("""
            SELECT COUNT(h) FROM GrupoHistorial h
            WHERE h.grupo.id = :grupoId
              AND h.usuario.id = :usuarioId
            """)
    long countAccionesDeUsuarioEnGrupo(
            @Param("grupoId") Long grupoId,
            @Param("usuarioId") Long usuarioId
    );

    @Query("""
            SELECT h FROM GrupoHistorial h
            LEFT JOIN FETCH h.usuario
            LEFT JOIN FETCH h.grupo
            ORDER BY h.fecha DESC
            """)
    Page<GrupoHistorial> findAccionesRecientes(Pageable pageable);

    @Modifying
    @Query("""
            DELETE FROM GrupoHistorial h
            WHERE h.fecha < :limite
            """)
    int eliminarAccionesAntiguas(@Param("limite") Instant limite);

    @Query("""
            SELECT COUNT(h) FROM GrupoHistorial h
            WHERE h.fecha < :limite
            """)
    long countAccionesAntiguas(@Param("limite") Instant limite);

    @Query("""
            SELECT COUNT(h) > 0 FROM GrupoHistorial h
            WHERE h.grupo.id = :grupoId
              AND h.accion = :accion
            """)
    boolean existeAccion(
            @Param("grupoId") Long grupoId,
            @Param("accion") String accion
    );

    // ============================================================
    // SEGURIDAD: Validar que el usuario pertenece al grupo
    // ============================================================

    /**
     * NUEVO: Verifica si el usuario puede ver el historial de un grupo.
     * Solo si es miembro activo o creador.
     */
    @Query("""
            SELECT COUNT(h) > 0 FROM GrupoHistorial h
            WHERE h.grupo.id = :grupoId
              AND EXISTS (
                SELECT 1 FROM MiembroGrupo m
                WHERE m.grupo.id = :grupoId
                  AND m.usuario.id = :usuarioId
                  AND m.estado = 'ACTIVO'
              )
            """)
    boolean usuarioPuedeVerHistorial(
            @Param("grupoId") Long grupoId,
            @Param("usuarioId") Long usuarioId
    );
}