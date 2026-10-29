// src/main/java/com/kiert/backend/features/grupo/repository/GrupoChatRepository.java
package com.kiert.backend.features.grupo.repository;

import com.kiert.backend.entity.GrupoChat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GrupoChatRepository extends JpaRepository<GrupoChat, Long> {

    @Query("""
            SELECT DISTINCT g FROM GrupoChat g
            LEFT JOIN FETCH g.creador
            WHERE g.activo = true
              AND (
                g.creador.id = :usuarioId
                OR EXISTS (
                  SELECT 1 FROM MiembroGrupo m
                  WHERE m.grupo = g
                    AND m.usuario.id = :usuarioId
                    AND m.estado = 'ACTIVO'
                )
              )
            ORDER BY g.fechaCreacion DESC
            """)
    List<GrupoChat> findGruposDeUsuario(@Param("usuarioId") Long usuarioId);

    @Query("""
            SELECT DISTINCT g FROM GrupoChat g
            LEFT JOIN FETCH g.creador
            WHERE g.activo = true
              AND (
                g.creador.id = :usuarioId
                OR EXISTS (
                  SELECT 1 FROM MiembroGrupo m
                  WHERE m.grupo = g
                    AND m.usuario.id = :usuarioId
                    AND m.estado = 'ACTIVO'
                )
              )
            ORDER BY g.fechaCreacion DESC
            """)
    Page<GrupoChat> findGruposDeUsuarioPaginado(
            @Param("usuarioId") Long usuarioId,
            Pageable pageable
    );

    @Query("""
            SELECT DISTINCT g FROM GrupoChat g
            LEFT JOIN FETCH g.creador
            WHERE g.activo = true
              AND EXISTS (
                SELECT 1 FROM MiembroGrupo m
                WHERE m.grupo = g
                  AND m.usuario.id = :usuarioId
                  AND m.rol = 'ADMIN'
                  AND m.estado = 'ACTIVO'
              )
            ORDER BY g.fechaCreacion DESC
            """)
    List<GrupoChat> findGruposDondeEsAdmin(@Param("usuarioId") Long usuarioId);

    @Query("""
            SELECT COUNT(DISTINCT g) FROM GrupoChat g
            WHERE g.activo = true
              AND (
                g.creador.id = :usuarioId
                OR EXISTS (
                  SELECT 1 FROM MiembroGrupo m
                  WHERE m.grupo = g
                    AND m.usuario.id = :usuarioId
                    AND m.estado = 'ACTIVO'
                )
              )
            """)
    long countGruposDeUsuario(@Param("usuarioId") Long usuarioId);

    @Query("""
            SELECT g FROM GrupoChat g
            LEFT JOIN FETCH g.creador
            WHERE g.tipo = 'PUBLICO'
              AND g.activo = true
              AND NOT EXISTS (
                SELECT 1 FROM MiembroGrupo m
                WHERE m.grupo = g
                  AND m.usuario.id = :usuarioId
                  AND m.estado IN ('ACTIVO', 'PENDIENTE')
              )
            ORDER BY g.fechaCreacion DESC
            """)
    List<GrupoChat> findGruposPublicosDisponibles(@Param("usuarioId") Long usuarioId);

    @Query("""
            SELECT g FROM GrupoChat g
            LEFT JOIN FETCH g.creador
            WHERE g.tipo = 'PUBLICO'
              AND g.activo = true
              AND NOT EXISTS (
                SELECT 1 FROM MiembroGrupo m
                WHERE m.grupo = g
                  AND m.usuario.id = :usuarioId
                  AND m.estado IN ('ACTIVO', 'PENDIENTE')
              )
            ORDER BY g.fechaCreacion DESC
            """)
    Page<GrupoChat> findGruposPublicosDisponiblesPaginado(
            @Param("usuarioId") Long usuarioId,
            Pageable pageable
    );

    @Query("""
            SELECT g FROM GrupoChat g
            LEFT JOIN FETCH g.creador
            WHERE g.activo = true
              AND (
                g.tipo = 'PUBLICO'
                OR EXISTS (
                  SELECT 1 FROM MiembroGrupo m
                  WHERE m.grupo = g
                    AND m.usuario.id = :usuarioId
                    AND m.estado = 'ACTIVO'
                )
              )
              AND LOWER(g.nombre) LIKE LOWER(CONCAT('%', :query, '%'))
            ORDER BY g.fechaCreacion DESC
            """)
    List<GrupoChat> buscarPorNombreSeguro(
            @Param("usuarioId") Long usuarioId,
            @Param("query") String query
    );

    @Query("""
            SELECT g FROM GrupoChat g
            LEFT JOIN FETCH g.creador
            WHERE g.activo = true
              AND (
                LOWER(g.nombre) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(g.descripcion) LIKE LOWER(CONCAT('%', :query, '%'))
              )
              AND g.tipo = 'PUBLICO'
            ORDER BY g.fechaCreacion DESC
            """)
    Page<GrupoChat> buscarAvanzado(
            @Param("query") String query,
            Pageable pageable
    );

    @Query("""
            SELECT DISTINCT g FROM GrupoChat g
            LEFT JOIN FETCH g.creador
            WHERE g.activo = true
              AND EXISTS (
                SELECT 1 FROM MiembroGrupo m
                WHERE m.grupo = g
                  AND m.usuario.id = :usuarioId
                  AND m.estado = 'ACTIVO'
              )
              AND LOWER(g.nombre) LIKE LOWER(CONCAT('%', :query, '%'))
            ORDER BY g.fechaCreacion DESC
            """)
    List<GrupoChat> buscarEnMisGrupos(
            @Param("usuarioId") Long usuarioId,
            @Param("query") String query
    );

    @Query("""
            SELECT g FROM GrupoChat g
            LEFT JOIN FETCH g.creador
            WHERE g.tipo = 'PUBLICO'
              AND g.activo = true
            ORDER BY (
                SELECT COUNT(m) FROM MiembroGrupo m
                WHERE m.grupo = g AND m.estado = 'ACTIVO'
            ) DESC, g.fechaCreacion DESC
            """)
    List<GrupoChat> findGruposPopulares(Pageable pageable);

    @Query("""
            SELECT g FROM GrupoChat g
            LEFT JOIN FETCH g.creador
            WHERE g.activo = true
              AND g.tipo = 'PUBLICO'
            ORDER BY g.fechaCreacion DESC
            """)
    List<GrupoChat> findGruposRecientes(Pageable pageable);

    @Query("""
            SELECT g FROM GrupoChat g
            LEFT JOIN FETCH g.creador
            WHERE g.id = :id
              AND g.activo = true
            """)
    Optional<GrupoChat> findActivoById(@Param("id") Long id);

    @Query("""
            SELECT g FROM GrupoChat g
            WHERE g.id = :grupoId
              AND g.creador.id = :usuarioId
              AND g.activo = true
            """)
    Optional<GrupoChat> findByIdAndCreador(
            @Param("grupoId") Long grupoId,
            @Param("usuarioId") Long usuarioId
    );

    @Query("""
            SELECT COUNT(m) FROM MiembroGrupo m
            WHERE m.grupo.id = :grupoId
              AND m.estado = 'ACTIVO'
            """)
    long countMiembrosActivos(@Param("grupoId") Long grupoId);

    @Query("""
            SELECT m.grupo.id, COUNT(m)
            FROM MiembroGrupo m
            WHERE m.grupo.id IN :grupoIds
              AND m.estado = 'ACTIVO'
            GROUP BY m.grupo.id
            """)
    List<Object[]> contarMiembrosPorGrupos(@Param("grupoIds") List<Long> grupoIds);

    @Query("SELECT COUNT(g) FROM GrupoChat g WHERE g.activo = true")
    long countActivos();

    @Query("""
            SELECT COUNT(g) FROM GrupoChat g
            WHERE g.activo = true
              AND g.tipo = 'PUBLICO'
            """)
    long countPublicosActivos();

    @Query("""
            SELECT COUNT(g) > 0 FROM GrupoChat g
            WHERE g.activo = true
              AND g.creador.id = :usuarioId
              AND LOWER(g.nombre) = LOWER(:nombre)
            """)
    boolean existePorCreadorYNombre(
            @Param("usuarioId") Long usuarioId,
            @Param("nombre") String nombre
    );

    @Modifying
    @Query("""
            UPDATE GrupoChat g
            SET g.activo = false
            WHERE g.id IN :ids
            """)
    int desactivarEnLote(@Param("ids") List<Long> ids);

    // ============================================================
    // SEGURIDAD: Verificar membresia
    // ============================================================

    @Query("""
            SELECT COUNT(m) > 0 FROM MiembroGrupo m
            WHERE m.grupo.id = :grupoId
              AND m.usuario.id = :usuarioId
              AND m.estado = 'ACTIVO'
            """)
    boolean esMiembroActivo(
            @Param("grupoId") Long grupoId,
            @Param("usuarioId") Long usuarioId
    );

    @Query("""
            SELECT COUNT(g) > 0 FROM GrupoChat g
            WHERE g.id = :grupoId
              AND g.activo = true
              AND (
                g.tipo = 'PUBLICO'
                OR EXISTS (
                  SELECT 1 FROM MiembroGrupo m
                  WHERE m.grupo = g
                    AND m.usuario.id = :usuarioId
                    AND m.estado = 'ACTIVO'
                )
              )
            """)
    boolean puedeAccederAlGrupo(
            @Param("grupoId") Long grupoId,
            @Param("usuarioId") Long usuarioId
    );

    @Query("""
            SELECT COUNT(g) > 0 FROM GrupoChat g
            WHERE g.id = :grupoId
              AND g.activo = true
              AND (
                g.tipo = 'PUBLICO'
                OR EXISTS (
                  SELECT 1 FROM MiembroGrupo m
                  WHERE m.grupo = g
                    AND m.usuario.id = :usuarioId
                    AND m.estado = 'ACTIVO'
                )
              )
            """)
    boolean puedeVerMiembros(
            @Param("grupoId") Long grupoId,
            @Param("usuarioId") Long usuarioId
    );
}