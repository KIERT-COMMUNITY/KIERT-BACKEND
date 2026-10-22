// src/main/java/com/kiert/backend/repository/InvitacionLinkRepository.java
package com.kiert.backend.features.invitacion.repository;

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

    @Query("""
            SELECT i FROM InvitacionLink i
            LEFT JOIN FETCH i.grupo g
            LEFT JOIN FETCH i.creador
            WHERE i.token = :token
            """)
    Optional<InvitacionLink> findByToken(@Param("token") String token);

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

    @Query("""
            SELECT i FROM InvitacionLink i
            LEFT JOIN FETCH i.creador
            WHERE i.grupo.id = :grupoId
              AND i.activo = true
            ORDER BY i.fechaCreacion DESC
            """)
    List<InvitacionLink> findActivosByGrupo(@Param("grupoId") Long grupoId);

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

    @Query("""
            SELECT COUNT(i) > 0 FROM InvitacionLink i
            WHERE i.token = :token
              AND i.activo = true
            """)
    boolean existeTokenActivo(@Param("token") String token);

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

    @Query("""
            SELECT COUNT(i) FROM InvitacionLink i
            WHERE i.grupo.id = :grupoId
              AND i.activo = true
            """)
    long countActivosByGrupo(@Param("grupoId") Long grupoId);

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

    @Modifying
    @Query("""
            UPDATE InvitacionLink i
            SET i.activo = false
            WHERE i.grupo.id = :grupoId
              AND i.activo = true
            """)
    int desactivarPorGrupo(@Param("grupoId") Long grupoId);

    @Modifying
    @Query("""
            UPDATE InvitacionLink i
            SET i.activo = false
            WHERE i.id = :linkId
            """)
    int desactivar(@Param("linkId") Long linkId);

    @Modifying
    @Query("""
            DELETE FROM InvitacionLink i
            WHERE i.expiraEn IS NOT NULL
              AND i.expiraEn < :limite
            """)
    int eliminarExpirados(@Param("limite") Instant limite);

    @Modifying
    @Query("""
            DELETE FROM InvitacionLink i
            WHERE i.usosMaximos > 0
              AND i.usosActuales >= i.usosMaximos
              AND i.fechaUltimoUso IS NOT NULL
              AND i.fechaUltimoUso < :limite
            """)
    int eliminarAgotados(@Param("limite") Instant limite);

    @Query("SELECT COUNT(i) FROM InvitacionLink i WHERE i.activo = true")
    long countActivosGlobales();

    @Query("""
            SELECT i FROM InvitacionLink i
            LEFT JOIN FETCH i.grupo
            LEFT JOIN FETCH i.creador
            WHERE i.usosActuales > 0
            ORDER BY i.usosActuales DESC
            """)
    List<InvitacionLink> findMasUsados(Pageable pageable);

    @Query("""
            SELECT i.grupo.id, SUM(i.usosActuales)
            FROM InvitacionLink i
            WHERE i.grupo.id IN :grupoIds
            GROUP BY i.grupo.id
            """)
    List<Object[]> contarUsosPorGrupos(@Param("grupoIds") List<Long> grupoIds);

    // ============================================================
    // SEGURIDAD: Validar permisos
    // ============================================================

    /**
     * NUEVO: Verifica si el usuario puede gestionar los links de un grupo.
     * Solo el creador del link o un admin del grupo.
     */
    @Query("""
            SELECT COUNT(i) > 0 FROM InvitacionLink i
            WHERE i.id = :linkId
              AND (
                i.creador.id = :usuarioId
                OR EXISTS (
                  SELECT 1 FROM MiembroGrupo m
                  WHERE m.grupo.id = i.grupo.id
                    AND m.usuario.id = :usuarioId
                    AND m.rol = 'ADMIN'
                    AND m.estado = 'ACTIVO'
                )
              )
            """)
    boolean puedeGestionarLink(
            @Param("linkId") Long linkId,
            @Param("usuarioId") Long usuarioId
    );
}