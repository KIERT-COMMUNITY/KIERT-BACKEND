// src/main/java/com/kiert/backend/repository/GrupoChatRepository.java
package com.kiert.backend.repository;

import com.kiert.backend.entity.GrupoChat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface GrupoChatRepository extends JpaRepository<GrupoChat, Long> {

    // ============================================================
    // GRUPOS DEL USUARIO
    // ============================================================

    /**
     * Lista los grupos donde el usuario es creador O miembro ACTIVO.
     * ✅ MEJORA: incluye JOIN FETCH del creador para evitar N+1.
     */
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

    /**
     * ✅ NUEVO: Versión paginada de grupos del usuario.
     */
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

    /**
     * ✅ NUEVO: Grupos donde el usuario es ADMIN.
     */
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

    /**
     * ✅ NUEVO: Cuenta los grupos activos de un usuario.
     */
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

    // ============================================================
    // GRUPOS PÚBLICOS DISPONIBLES (excluye los que ya es miembro)
    // ============================================================

    /**
     * ✅ MEJORA: usa `NOT EXISTS` en lugar de `NOT IN` (más rápido).
     * `NOT IN` con subqueries grandes es lento; `NOT EXISTS` usa índices.
     */
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

    /**
     * ✅ NUEVO: Versión paginada.
     */
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

    // ============================================================
    // BÚSQUEDA
    // ============================================================

    /**
     * Busca grupos por nombre (LIKE).
     */
    @Query("""
            SELECT g FROM GrupoChat g
            LEFT JOIN FETCH g.creador
            WHERE LOWER(g.nombre) LIKE LOWER(CONCAT('%', :query, '%'))
              AND g.activo = true
            ORDER BY g.fechaCreacion DESC
            """)
    List<GrupoChat> buscarPorNombre(@Param("query") String query);

    /**
     * ✅ NUEVO: Búsqueda avanzada (nombre + descripción).
     */
    @Query("""
            SELECT g FROM GrupoChat g
            LEFT JOIN FETCH g.creador
            WHERE g.activo = true
              AND (
                LOWER(g.nombre) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(g.descripcion) LIKE LOWER(CONCAT('%', :query, '%'))
              )
            ORDER BY g.fechaCreacion DESC
            """)
    Page<GrupoChat> buscarAvanzado(
            @Param("query") String query,
            Pageable pageable
    );

    /**
     * ✅ NUEVO: Busca grupos donde el usuario es miembro.
     */
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

    // ============================================================
    // GRUPOS POPULARES (home, recomendaciones)
    // ============================================================

    /**
     * ✅ NUEVO: Top N grupos públicos con más miembros activos.
     */
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

    /**
     * ✅ NUEVO: Grupos recientes (últimos creados).
     */
    @Query("""
            SELECT g FROM GrupoChat g
            LEFT JOIN FETCH g.creador
            WHERE g.activo = true
              AND g.tipo = 'PUBLICO'
            ORDER BY g.fechaCreacion DESC
            """)
    List<GrupoChat> findGruposRecientes(Pageable pageable);

    // ============================================================
    // OBTENER POR ID
    // ============================================================

    /**
     * ✅ NUEVO: Obtiene un grupo activo con su creador cargado.
     */
    @Query("""
            SELECT g FROM GrupoChat g
            LEFT JOIN FETCH g.creador
            WHERE g.id = :id
              AND g.activo = true
            """)
    Optional<GrupoChat> findActivoById(@Param("id") Long id);

    /**
     * Busca grupo por ID y creador (para validaciones de permisos).
     */
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

    // ============================================================
    // CONTADORES Y ESTADÍSTICAS
    // ============================================================

    /**
     * ✅ NUEVO: Cuenta miembros activos de un grupo.
     */
    @Query("""
            SELECT COUNT(m) FROM MiembroGrupo m
            WHERE m.grupo.id = :grupoId
              AND m.estado = 'ACTIVO'
            """)
    long countMiembrosActivos(@Param("grupoId") Long grupoId);

    /**
     * ✅ NUEVO: Cuenta miembros activos agrupados por grupos.
     * Devuelve [grupoId, count].
     * Útil para el listado sin N+1.
     */
    @Query("""
            SELECT m.grupo.id, COUNT(m)
            FROM MiembroGrupo m
            WHERE m.grupo.id IN :grupoIds
              AND m.estado = 'ACTIVO'
            GROUP BY m.grupo.id
            """)
    List<Object[]> contarMiembrosPorGrupos(@Param("grupoIds") List<Long> grupoIds);

    /**
     * ✅ NUEVO: Total de grupos activos.
     */
    @Query("SELECT COUNT(g) FROM GrupoChat g WHERE g.activo = true")
    long countActivos();

    /**
     * ✅ NUEVO: Total de grupos públicos activos.
     */
    @Query("""
            SELECT COUNT(g) FROM GrupoChat g
            WHERE g.activo = true
              AND g.tipo = 'PUBLICO'
            """)
    long countPublicosActivos();

    // ============================================================
    // VALIDACIONES
    // ============================================================

    /**
     * ✅ NUEVO: Verifica si ya existe un grupo con ese nombre
     * creado por el mismo usuario.
     */
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

    // ============================================================
    // BULK UPDATES
    // ============================================================

    /**
     * ✅ NUEVO: Desactiva varios grupos en 1 query.
     */
    @Modifying
    @Query("""
            UPDATE GrupoChat g
            SET g.activo = false
            WHERE g.id IN :ids
            """)
    int desactivarEnLote(@Param("ids") List<Long> ids);
}