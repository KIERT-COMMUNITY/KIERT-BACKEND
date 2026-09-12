package com.kiert.backend.repository;

import com.kiert.backend.entity.MiembroGrupo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MiembroGrupoRepository extends JpaRepository<MiembroGrupo, Long> {

    // Miembros activos de un grupo
    @Query("SELECT m FROM MiembroGrupo m " +
            "JOIN FETCH m.usuario " +
            "WHERE m.grupo.id = :grupoId " +
            "AND m.estado = 'ACTIVO' " +
            "ORDER BY m.fechaUnion ASC")
    List<MiembroGrupo> findMiembrosActivos(@Param("grupoId") Long grupoId);

    // Buscar membresía específica
    Optional<MiembroGrupo> findByGrupoIdAndUsuarioId(Long grupoId, Long usuarioId);

    // Invitaciones pendientes para un usuario
    @Query("SELECT m FROM MiembroGrupo m " +
            "JOIN FETCH m.grupo g " +
            "JOIN FETCH m.invitadoPor " +
            "WHERE m.usuario.id = :usuarioId " +
            "AND m.estado = 'PENDIENTE' " +
            "ORDER BY m.fechaInvitacion DESC")
    List<MiembroGrupo> findInvitacionesPendientes(@Param("usuarioId") Long usuarioId);

    // Contar miembros activos
    @Query("SELECT COUNT(m) FROM MiembroGrupo m " +
            "WHERE m.grupo.id = :grupoId AND m.estado = 'ACTIVO'")
    long countMiembrosActivos(@Param("grupoId") Long grupoId);

    // Verificar si el usuario es miembro activo
    @Query("SELECT COUNT(m) > 0 FROM MiembroGrupo m " +
            "WHERE m.grupo.id = :grupoId " +
            "AND m.usuario.id = :usuarioId " +
            "AND m.estado = 'ACTIVO'")
    boolean esMiembroActivo(
            @Param("grupoId") Long grupoId,
            @Param("usuarioId") Long usuarioId
    );
}