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

    /**
     * Lista miembros ACTIVOS de un grupo con su usuario cargado.
     * ⚠️ Para grupos grandes (500+), usa `findMiembrosActivosPaginado`.
     */
    @Query("""
            SELECT m FROM MiembroGrupo m
            JOIN FETCH m.usuario
            LEFT JOIN FETCH m.invitadoPor
            WHERE m.grupo.id = :grupoId
              AND m.estado = 'ACTIVO'
            ORDER BY m.fechaUnion ASC
            """)
    List<MiembroGrupo> findMiembrosActivos(@Param("grupoId") Long grupoId);

    /**
     * ✅ NUEVO: Versión paginada (recomendada para grupos grandes).
     */
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

    /**
     * Busca un miembro por grupo+usuario (cualquier estado).
     * ✅ MEJORA: incluye JOIN FETCH de usuario y grupo para evitar N+1.
     */
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

    /**
     * Alias de `findByGrupoIdAndUsuarioId` (retrocompatibilidad).
     */
    default Optional<MiembroGrupo> findMiembroByGrupoAndUsuario(Long grupoId, Long usuarioId) {
        return findByGrupoIdAndUsuarioId(grupoId, usuarioId);
    }

    // ============================================================
    // INVITACIONES PENDIENTES
    // ============================================================

    /**
     * Lista invitaciones PENDIENTES del usuario.
     * ✅ MEJORA: filtra por grupos activos (no muestra grupos eliminados).
     */
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

    /**
     * ✅ NUEVO: Invitaciones PENDIENTES de un grupo específico.
     */
    @Query("""
            SELECT m FROM MiembroGrupo m
            JOIN FETCH m.usuario
            LEFT JOIN FETCH m.invitadoPor
            WHERE m.grupo.id = :grupoId
              AND m.estado = 'PENDIENTE'
            ORDER BY m.fechaInvitacion DESC
            """)
    List<MiembroGrupo> findPendientesDeGrupo(@Param("grupoId") Long grupoId);

    /**
     * ✅ NUEVO: Cuenta invitaciones pendientes del usuario (para badge).
     */
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

    /**
     * ✅ NUEVO: Miembros con rol ADMIN de un grupo.
     */
    @Query("""
            SELECT m FROM MiembroGrupo m
            JOIN FETCH m.usuario
            WHERE m.grupo.id = :grupoId
              AND m.rol = 'ADMIN'
              AND m.estado = 'ACTIVO'
            ORDER BY m.fechaUnion ASC
            """)
    List<MiembroGrupo> findAdminsDeGrupo(@Param("grupoId") Long grupoId);

    /**
     * ✅ NUEVO: Cuenta admins de un grupo.
     */
    @Query("""
            SELECT COUNT(m) FROM MiembroGrupo m
            WHERE m.grupo.id = :grupoId
              AND m.rol = 'ADMIN'
              AND m.estado = 'ACTIVO'
            """)
    long countAdminsDeGrupo(@Param("grupoId") Long grupoId);

    /**
     * ✅ NUEVO: Grupos donde el usuario es ADMIN.
     */
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

    /**
     * Cuenta miembros ACTIVOS de un grupo.
     */
    @Query("""
            SELECT COUNT(m) FROM MiembroGrupo m
            WHERE m.grupo.id = :grupoId
              AND m.estado = 'ACTIVO'
            """)
    long countMiembrosActivos(@Param("grupoId") Long grupoId);

    /**
     * ✅ NUEVO: Cuenta miembros por estado.
     */
    @Query("""
            SELECT COUNT(m) FROM MiembroGrupo m
            WHERE m.grupo.id = :grupoId
              AND m.estado = :estado
            """)
    long countByGrupoAndEstado(
            @Param("grupoId") Long grupoId,
            @Param("estado") String estado
    );

    /**
     * ✅ NUEVO: Cuenta miembros agrupados por rol.
     * Devuelve [rol, count].
     */
    @Query("""
            SELECT m.rol, COUNT(m)
            FROM MiembroGrupo m
            WHERE m.grupo.id = :grupoId
              AND m.estado = 'ACTIVO'
            GROUP BY m.rol
            """)
    List<Object[]> contarPorRol(@Param("grupoId") Long grupoId);

    /**
     * ✅ NUEVO: Contar miembros activos de varios grupos en 1 query.
     * Devuelve [grupoId, count].
     * Útil para el sidebar sin N+1.
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
     * ✅ NUEVO: Cuenta cuántos grupos activos tiene el usuario como miembro.
     */
    @Query("""
            SELECT COUNT(DISTINCT m.grupo.id) FROM MiembroGrupo m
            JOIN m.grupo g
            WHERE m.usuario.id = :usuarioId
              AND m.estado = 'ACTIVO'
              AND g.activo = true
            """)
    long countGruposDeUsuario(@Param("usuarioId") Long usuarioId);

    // ============================================================
    // VERIFICACIONES
    // ============================================================

    /**
     * Verifica si el usuario es miembro ACTIVO del grupo.
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

    /**
     * ✅ NUEVO: Verifica si el usuario es ADMIN del grupo.
     */
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

    /**
     * ✅ NUEVO: Verifica si el usuario tiene una invitación PENDIENTE.
     */
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

    /**
     * ✅ NUEVO: Buscar miembros por nombre de usuario.
     */
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

    /**
     * ✅ NUEVO: IDs de usuarios que NO son miembros del grupo.
     * Útil para el modal de invitar usuarios.
     */
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

    /**
     * ✅ NUEVO: Expulsa a varios miembros en 1 query.
     */
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

    /**
     * ✅ NUEVO: Cambia el rol de un miembro.
     */
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

    /**
     * ✅ NUEVO: Elimina físicamente miembros con estado final
     * (EXPULSADO, SALIO, RECHAZADO) hace más de N días.
     */
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

    /**
     * ✅ NUEVO: Cuenta total de miembros activos en el sistema.
     */
    @Query("""
            SELECT COUNT(m) FROM MiembroGrupo m
            WHERE m.estado = 'ACTIVO'
            """)
    long countActivosGlobales();

    /**
     * ✅ NUEVO: Top grupos con más miembros.
     */
    @Query("""
            SELECT m.grupo.id, COUNT(m) as total
            FROM MiembroGrupo m
            WHERE m.estado = 'ACTIVO'
            GROUP BY m.grupo.id
            ORDER BY total DESC
            """)
    List<Object[]> topGruposConMasMiembros(Pageable pageable);
}