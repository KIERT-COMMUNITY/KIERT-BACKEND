// src/main/java/com/kiert/backend/repository/MiembroGrupoRepository.java
package com.kiert.backend.repository;

import com.kiert.backend.entity.MiembroGrupo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MiembroGrupoRepository extends JpaRepository<MiembroGrupo, Long> {

    @Query("SELECT m FROM MiembroGrupo m " +
            "JOIN FETCH m.usuario " +
            "WHERE m.grupo.id = :grupoId " +
            "AND m.estado = 'ACTIVO' " +
            "ORDER BY m.fechaUnion ASC")
    List<MiembroGrupo> findMiembrosActivos(@Param("grupoId") Long grupoId);

    Optional<MiembroGrupo> findByGrupoIdAndUsuarioId(Long grupoId, Long usuarioId);

    @Query("SELECT m FROM MiembroGrupo m " +
            "JOIN FETCH m.grupo g " +
            "JOIN FETCH m.invitadoPor " +
            "WHERE m.usuario.id = :usuarioId " +
            "AND m.estado = 'PENDIENTE' " +
            "ORDER BY m.fechaInvitacion DESC")
    List<MiembroGrupo> findInvitacionesPendientes(@Param("usuarioId") Long usuarioId);

    @Query("SELECT COUNT(m) FROM MiembroGrupo m " +
            "WHERE m.grupo.id = :grupoId AND m.estado = 'ACTIVO'")
    long countMiembrosActivos(@Param("grupoId") Long grupoId);

    @Query("SELECT COUNT(m) > 0 FROM MiembroGrupo m " +
            "WHERE m.grupo.id = :grupoId " +
            "AND m.usuario.id = :usuarioId " +
            "AND m.estado = 'ACTIVO'")
    boolean esMiembroActivo(
            @Param("grupoId") Long grupoId,
            @Param("usuarioId") Long usuarioId
    );

    // 🔥 NUEVO: Buscar miembro (cualquier estado)
    @Query("SELECT m FROM MiembroGrupo m " +
            "WHERE m.grupo.id = :grupoId " +
            "AND m.usuario.id = :usuarioId")
    Optional<MiembroGrupo> findMiembroByGrupoAndUsuario(
            @Param("grupoId") Long grupoId,
            @Param("usuarioId") Long usuarioId
    );
}