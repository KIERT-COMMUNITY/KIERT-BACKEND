// src/main/java/com/kiert/backend/repository/InvitacionLinkRepository.java
package com.kiert.backend.repository;

import com.kiert.backend.entity.InvitacionLink;
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
public interface InvitacionLinkRepository extends JpaRepository<InvitacionLink, Long> {

    // ============================================================
    // BUSCAR POR TOKEN
    // ============================================================

    /**
     * Busca un link por su token, cargando grupo y creador.
     * MEJORA: incluye JOIN FETCH para evitar N+1.
     */
    @Query("""
            SELECT i FROM InvitacionLink i
            LEFT JOIN FETCH i.grupo g
            LEFT JOIN FETCH i.creador
            WHERE i.token = :token
            """)
    Optional<InvitacionLink> findByToken(@Param("token") String token);

    /**
     * NUEVO: Busca un link VÁLIDO por token.
     * Filtra activo, no expirado, y con usos disponibles EN LA QUERY.
     */
    @Query("""
            SELECT i FROM InvitacionLink i
            LEFT JOIN FETCH i.grupo g
            LEFT JOIN FETCH i.creador
            WHERE i.token = :token
              AND i.activo = true
              AND (i.expiraEn IS NULL OR i.expiraEn > :ahora)
              AND (i.usosMaximos = 0 OR i.usosActuales < i.usosMaximos)
            """)
    Optional<InvitacionLink> findVigenteByToken(
            @Param("token") String token,
            @Param("ahora") Instant ahora
    );

    // ============================================================
    // LINKS ACTIVOS POR GRUPO
    // ============================================================

    /**
     * Lista todos los links activos de un grupo.
     * MEJORA: incluye JOIN FETCH del creador.
     */
    @Query("""
            SELECT i FROM InvitacionLink i
            LEFT JOIN FETCH i.creador
            WHERE i.grupo.id = :grupoId
              AND i.activo = true
            ORDER BY i.fechaCreacion DESC
            """)
    List<InvitacionLink> findActivosByGrupo(@Param("grupoId") Long grupoId);

    /**
     * NUEVO: Versión paginada.
     */
    @Query("""
            SELECT i FROM InvitacionLink i
            LEFT JOIN FETCH i.creador
            WHERE i.grupo.id = :grupoId
              AND i.activo = true
            ORDER BY i.fechaCreacion DESC
            """)
    Page<InvitacionLink> findActivosByGrupoPaginado(
            @Param("grupoId") Long grupoId,
            Pageable pageable
    );

    /**
     * NUEVO: Solo links vigentes de un grupo (no expirados, con usos).
     */
    @Query("""
            SELECT i FROM InvitacionLink i
            LEFT JOIN FETCH i.creador
            WHERE i.grupo.id = :grupoId
              AND i.activo = true
              AND (i.expiraEn IS NULL OR i.expiraEn > :ahora)
              AND (i.usosMaximos = 0 OR i.usosActuales < i.usosMaximos)
            ORDER BY i.fechaCreacion DESC
            """)
    List<InvitacionLink> findVigentesByGrupo(
            @Param("grupoId") Long grupoId,
            @Param("ahora") Instant ahora
    );

    // ============================================================
    // LINKS POR CREADOR
    // ============================================================

    /**
     * NUEVO: Links creados por un usuario (en todos los grupos).
     */
    @Query("""
            SELECT i FROM InvitacionLink i
            LEFT JOIN FETCH i.grupo g
            WHERE i.creador.id = :usuarioId
            ORDER BY i.fechaCreacion DESC
            """)
    Page<InvitacionLink> findByCreador(
            @Param("usuarioId") Long usuarioId,
            Pageable pageable
    );

    /**
     * NUEVO: Links vigentes creados por un usuario en un grupo específico.
     */
    @Query("""
            SELECT i FROM InvitacionLink i
            WHERE i.creador.id = :usuarioId
              AND i.grupo.id = :grupoId
              AND i.activo = true
              AND (i.expiraEn IS NULL OR i.expiraEn > :ahora)
              AND (i.usosMaximos = 0 OR i.usosActuales < i.usosMaximos)
            ORDER BY i.fechaCreacion DESC
            """)
    List<InvitacionLink> findVigentesDeUsuarioEnGrupo(
            @Param("usuarioId") Long usuarioId,
            @Param("grupoId") Long grupoId,
            @Param("ahora") Instant ahora
    );

    // ============================================================
    // VALIDACIONES
    // ============================================================

    /**
     * Verifica si un token existe y está activo.
     */
    @Query("""
            SELECT COUNT(i) > 0 FROM InvitacionLink i
            WHERE i.token = :token
              AND i.activo = true
            """)
    boolean existeTokenActivo(@Param("token") String token);

    /**
     * NUEVO: Verifica si un token es VIGENTE (activo + no expirado + con usos).
     */
    @Query("""
            SELECT COUNT(i) > 0 FROM InvitacionLink i
            WHERE i.token = :token
              AND i.activo = true
              AND (i.expiraEn IS NULL OR i.expiraEn > :ahora)
              AND (i.usosMaximos = 0 OR i.usosActuales < i.usosMaximos)
            """)
    boolean esTokenVigente(
            @Param("token") String token,
            @Param("ahora") Instant ahora
    );

    // ============================================================
    // CONTADORES
    // ============================================================

    /**
     *NUEVO: Cuenta links activos de un grupo.
     */
    @Query("""
            SELECT COUNT(i) FROM InvitacionLink i
            WHERE i.grupo.id = :grupoId
              AND i.activo = true
            """)
    long countActivosByGrupo(@Param("grupoId") Long grupoId);

    /**
     * NUEVO: Cuenta links vigentes de un grupo.
     */
    @Query("""
            SELECT COUNT(i) FROM InvitacionLink i
            WHERE i.grupo.id = :grupoId
              AND i.activo = true
              AND (i.expiraEn IS NULL OR i.expiraEn > :ahora)
              AND (i.usosMaximos = 0 OR i.usosActuales < i.usosMaximos)
            """)
    long countVigentesByGrupo(
            @Param("grupoId") Long grupoId,
            @Param("ahora") Instant ahora
    );

    // ============================================================
    // INCREMENTAR USOS (bulk update atómico)
    // ============================================================

    /**
     * NUEVO: Incrementa usos y actualiza fecha_ultimo_uso EN 1 QUERY.
     * Mucho más eficiente que cargar la entidad y guardarla.
     *
     * IMPORTANTE: usa `WHERE usos_maximos = 0 OR usos_actuales < usos_maximos`
     * para garantizar que no se exceda el límite, incluso con concurrencia.
     */
    @Modifying
    @Query("""
            UPDATE InvitacionLink i
            SET i.usosActuales = i.usosActuales + 1,
                i.fechaUltimoUso = :ahora
            WHERE i.id = :linkId
              AND i.activo = true
              AND (i.expiraEn IS NULL OR i.expiraEn > :ahora)
              AND (i.usosMaximos = 0 OR i.usosActuales < i.usosMaximos)
            """)
    int registrarUso(
            @Param("linkId") Long linkId,
            @Param("ahora") Instant ahora
    );

    /**
     * NUEVO: Desactiva todos los links de un grupo (útil al eliminar grupo).
     */
    @Modifying
    @Query("""
            UPDATE InvitacionLink i
            SET i.activo = false
            WHERE i.grupo.id = :grupoId
              AND i.activo = true
            """)
    int desactivarPorGrupo(@Param("grupoId") Long grupoId);

    /**
     * NUEVO: Desactiva un link específico.
     */
    @Modifying
    @Query("""
            UPDATE InvitacionLink i
            SET i.activo = false
            WHERE i.id = :linkId
            """)
    int desactivar(@Param("linkId") Long linkId);

    // ============================================================
    // LIMPIEZA (para @Scheduled)
    // ============================================================

    /**
     * NUEVO: Elimina links expirados hace más de N días.
     */
    @Modifying
    @Query("""
            DELETE FROM InvitacionLink i
            WHERE i.expiraEn IS NOT NULL
              AND i.expiraEn < :limite
            """)
    int eliminarExpirados(@Param("limite") Instant limite);

    /**
     * NUEVO: Elimina links agotados (usos = usos_maximos) hace más de N días.
     */
    @Modifying
    @Query("""
            DELETE FROM InvitacionLink i
            WHERE i.usosMaximos > 0
              AND i.usosActuales >= i.usosMaximos
              AND i.fechaUltimoUso IS NOT NULL
              AND i.fechaUltimoUso < :limite
            """)
    int eliminarAgotados(@Param("limite") Instant limite);

    // ============================================================
    // ESTADÍSTICAS
    // ============================================================

    /**
     * NUEVO: Total de links activos en el sistema.
     */
    @Query("SELECT COUNT(i) FROM InvitacionLink i WHERE i.activo = true")
    long countActivosGlobales();

    /**
     * NUEVO: Top N links más usados.
     */
    @Query("""
            SELECT i FROM InvitacionLink i
            LEFT JOIN FETCH i.grupo
            LEFT JOIN FETCH i.creador
            WHERE i.usosActuales > 0
            ORDER BY i.usosActuales DESC
            """)
    List<InvitacionLink> findMasUsados(Pageable pageable);

    /**
     * NUEVO: Links con más usos agrupados por grupo.
     * Devuelve [grupoId, totalUsos].
     */
    @Query("""
            SELECT i.grupo.id, SUM(i.usosActuales)
            FROM InvitacionLink i
            WHERE i.grupo.id IN :grupoIds
            GROUP BY i.grupo.id
            """)
    List<Object[]> contarUsosPorGrupos(@Param("grupoIds") List<Long> grupoIds);
}