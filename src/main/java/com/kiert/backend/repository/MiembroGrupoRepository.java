// src/main/java/com/kiert/backend/repository/MiembroGrupoRepository.java
package com.kiert.backend.repository;

import com.kiert.backend.entity.MiembroGrupo;
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
public interface MiembroGrupoRepository extends JpaRepository<MiembroGrupo, Long> {

    // ============================================================
    // MIEMBROS ACTIVOS
    // ============================================================

    @Query("""
            SELECT m FROM MiembroGrupo m
            JOIN FETCH m.usuario
            LEFT JOIN FETCH m.invitadoPor
            WHERE m.grupo.id = :grupoId
              AND m.estado = 'ACTIVO'
            ORDER BY m.fechaUnion ASC
            """)
    List<MiembroGrupo> findMiembrosActivos(@Param("grupoId") Long grupoId);

    @Query("""
            SELECT m FROM MiembroGrupo m
            JOIN FETCH m.usuario
            LEFT JOIN FETCH m.invitadoPor
            WHERE m.grupo.id = :grupoId
              AND m.estado = 'ACTIVO'
            ORDER BY m.fechaUnion ASC
            """)
    Page<MiembroGrupo> findMiembrosActivosPaginado(
            @Param("grupoId") Long grupoId,
            Pageable pageable
    );

    // ============================================================
    // BUSCAR MIEMBRO ESPECÍFICO
    // ============================================================

    @Query("""
            SELECT m FROM MiembroGrupo m
            JOIN FETCH m.usuario
            JOIN FETCH m.grupo
            WHERE m.grupo.id = :grupoId
              AND m.usuario.id = :usuarioId
            """)
    Optional<MiembroGrupo> findByGrupoIdAndUsuarioId(
            @Param("grupoId") Long grupoId,
            @Param("usuarioId") Long usuarioId
    );

    default Optional<MiembroGrupo> findMiembroByGrupoAndUsuario(Long grupoId, Long usuarioId) {
        return findByGrupoIdAndUsuarioId(grupoId, usuarioId);
    }

    // ============================================================
    // ✅ MÉTODO CRÍTICO — SIN @Cacheable, siempre va a BD
    // ============================================================

    /**
     * Verifica si el usuario es miembro ACTIVO del grupo.
     * ⚠️ IMPORTANTE: NO cachear este método. Los cambios de estado
     * (aceptar invitación, salir del grupo, etc.) deben reflejarse
     * de inmediato, no en 5 minutos.
     */
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

    // ============================================================
    // INVITACIONES PENDIENTES
    // ============================================================

    @Query("""
            SELECT m FROM MiembroGrupo m
            JOIN FETCH m.grupo g
            LEFT JOIN FETCH m.invitadoPor
            WHERE m.usuario.id = :usuarioId
              AND m.estado = 'PENDIENTE'
              AND g.activo = true
            ORDER BY m.fechaInvitacion DESC
            """)
    List<MiembroGrupo> findInvitacionesPendientes(@Param("usuarioId") Long usuarioId);

    @Query("""
            SELECT m FROM MiembroGrupo m
            JOIN FETCH m.usuario
            LEFT JOIN FETCH m.invitadoPor
            WHERE m.grupo.id = :grupoId
              AND m.estado = 'PENDIENTE'
            ORDER BY m.fechaInvitacion DESC
            """)
    List<MiembroGrupo> findPendientesDeGrupo(@Param("grupoId") Long grupoId);

    @Query("""
            SELECT COUNT(m) FROM MiembroGrupo m
            JOIN m.grupo g
            WHERE m.usuario.id = :usuarioId
              AND m.estado = 'PENDIENTE'
              AND g.activo = true
            """)
    long countInvitacionesPendientes(@Param("usuarioId") Long usuarioId);

    // ============================================================
    // FILTRO POR ROL
    // ============================================================

    @Query("""
            SELECT m FROM MiembroGrupo m
            JOIN FETCH m.usuario
            WHERE m.grupo.id = :grupoId
              AND m.rol = 'ADMIN'
              AND m.estado = 'ACTIVO'
            ORDER BY m.fechaUnion ASC
            """)
    List<MiembroGrupo> findAdminsDeGrupo(@Param("grupoId") Long grupoId);

    @Query("""
            SELECT COUNT(m) FROM MiembroGrupo m
            WHERE m.grupo.id = :grupoId
              AND m.rol = 'ADMIN'
              AND m.estado = 'ACTIVO'
            """)
    long countAdminsDeGrupo(@Param("grupoId") Long grupoId);

    @Query("""
            SELECT m FROM MiembroGrupo m
            JOIN FETCH m.grupo g
            WHERE m.usuario.id = :usuarioId
              AND m.rol = 'ADMIN'
              AND m.estado = 'ACTIVO'
              AND g.activo = true
            ORDER BY m.fechaUnion DESC
            """)
    List<MiembroGrupo> findGruposComoAdmin(@Param("usuarioId") Long usuarioId);

    // ============================================================
    // CONTADORES
    // ============================================================

    @Query("""
            SELECT COUNT(m) FROM MiembroGrupo m
            WHERE m.grupo.id = :grupoId
              AND m.estado = 'ACTIVO'
            """)
    long countMiembrosActivos(@Param("grupoId") Long grupoId);

    @Query("""
            SELECT COUNT(m) FROM MiembroGrupo m
            WHERE m.grupo.id = :grupoId
              AND m.estado = :estado
            """)
    long countByGrupoAndEstado(
            @Param("grupoId") Long grupoId,
            @Param("estado") String estado
    );

    @Query("""
            SELECT m.rol, COUNT(m)
            FROM MiembroGrupo m
            WHERE m.grupo.id = :grupoId
              AND m.estado = 'ACTIVO'
            GROUP BY m.rol
            """)
    List<Object[]> contarPorRol(@Param("grupoId") Long grupoId);

    @Query("""
            SELECT m.grupo.id, COUNT(m)
            FROM MiembroGrupo m
            WHERE m.grupo.id IN :grupoIds
              AND m.estado = 'ACTIVO'
            GROUP BY m.grupo.id
            """)
    List<Object[]> contarMiembrosPorGrupos(@Param("grupoIds") List<Long> grupoIds);

    @Query("""
            SELECT COUNT(DISTINCT m.grupo.id) FROM MiembroGrupo m
            JOIN m.grupo g
            WHERE m.usuario.id = :usuarioId
              AND m.estado = 'ACTIVO'
              AND g.activo = true
            """)
    long countGruposDeUsuario(@Param("usuarioId") Long usuarioId);

    // ============================================================
    // VERIFICACIONES EXTRA
    // ============================================================

    @Query("""
            SELECT COUNT(m) > 0 FROM MiembroGrupo m
            WHERE m.grupo.id = :grupoId
              AND m.usuario.id = :usuarioId
              AND m.rol = 'ADMIN'
              AND m.estado = 'ACTIVO'
            """)
    boolean esAdmin(
            @Param("grupoId") Long grupoId,
            @Param("usuarioId") Long usuarioId
    );

    @Query("""
            SELECT COUNT(m) > 0 FROM MiembroGrupo m
            WHERE m.grupo.id = :grupoId
              AND m.usuario.id = :usuarioId
              AND m.estado = 'PENDIENTE'
            """)
    boolean tieneInvitacionPendiente(
            @Param("grupoId") Long grupoId,
            @Param("usuarioId") Long usuarioId
    );

    // ============================================================
    // BÚSQUEDA DE MIEMBROS
    // ============================================================

    @Query("""
            SELECT m FROM MiembroGrupo m
            JOIN FETCH m.usuario u
            WHERE m.grupo.id = :grupoId
              AND m.estado = 'ACTIVO'
              AND LOWER(u.nombreUsuario) LIKE LOWER(CONCAT('%', :query, '%'))
            ORDER BY m.fechaUnion ASC
            """)
    List<MiembroGrupo> buscarMiembros(
            @Param("grupoId") Long grupoId,
            @Param("query") String query
    );

    // ============================================================
    // USUARIOS DISPONIBLES PARA INVITAR
    // ============================================================

    @Query("""
            SELECT u.id FROM Usuario u
            WHERE u.id NOT IN (
                SELECT m.usuario.id FROM MiembroGrupo m
                WHERE m.grupo.id = :grupoId
            )
              AND u.activo = true
              AND u.eliminado = false
            ORDER BY u.nombreUsuario ASC
            """)
    Page<Long> findUsuariosNoMiembros(
            @Param("grupoId") Long grupoId,
            Pageable pageable
    );

    // ============================================================
    // BULK UPDATES
    // ============================================================

    @Modifying
    @Query("""
            UPDATE MiembroGrupo m
            SET m.estado = 'EXPULSADO'
            WHERE m.grupo.id = :grupoId
              AND m.usuario.id IN :usuarioIds
              AND m.estado = 'ACTIVO'
              AND m.rol <> 'ADMIN'
            """)
    int expulsarEnLote(
            @Param("grupoId") Long grupoId,
            @Param("usuarioIds") List<Long> usuarioIds
    );

    @Modifying
    @Query("""
            UPDATE MiembroGrupo m
            SET m.rol = :rol
            WHERE m.grupo.id = :grupoId
              AND m.usuario.id = :usuarioId
              AND m.estado = 'ACTIVO'
            """)
    int cambiarRol(
            @Param("grupoId") Long grupoId,
            @Param("usuarioId") Long usuarioId,
            @Param("rol") String rol
    );

    // ============================================================
    // LIMPIEZA (para @Scheduled)
    // ============================================================

    @Modifying
    @Query("""
            DELETE FROM MiembroGrupo m
            WHERE m.estado IN ('EXPULSADO', 'SALIO', 'RECHAZADO')
              AND m.fechaUnion < :limite
            """)
    int eliminarMiembrosInactivos(@Param("limite") Instant limite);

    // ============================================================
    // ESTADÍSTICAS
    // ============================================================

    @Query("""
            SELECT COUNT(m) FROM MiembroGrupo m
            WHERE m.estado = 'ACTIVO'
            """)
    long countActivosGlobales();

    @Query("""
            SELECT m.grupo.id, COUNT(m) as total
            FROM MiembroGrupo m
            WHERE m.estado = 'ACTIVO'
            GROUP BY m.grupo.id
            ORDER BY total DESC
            """)
    List<Object[]> topGruposConMasMiembros(Pageable pageable);
}