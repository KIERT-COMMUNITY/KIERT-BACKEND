package com.kiert.backend.repository;

import com.kiert.backend.entity.MiembroGrupo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MiembroGrupoRepository extends JpaRepository<MiembroGrupo, Long> {

    @Query("""
            select m
            from MiembroGrupo m
            join fetch m.usuario
            where m.grupo.id = :grupoId
              and m.estado = 'ACTIVO'
            order by m.fechaUnion asc
            """)
    List<MiembroGrupo> findMiembrosActivos(@Param("grupoId") Long grupoId);

    Optional<MiembroGrupo> findByGrupoIdAndUsuarioId(
            Long grupoId,
            Long usuarioId
    );

    @Query("""
            select m
            from MiembroGrupo m
            join fetch m.grupo g
            join fetch m.invitadoPor
            where m.usuario.id = :usuarioId
              and m.estado = 'PENDIENTE'
            order by m.fechaInvitacion desc
            """)
    List<MiembroGrupo> findInvitacionesPendientes(
            @Param("usuarioId") Long usuarioId
    );

    @Query("""
            select count(m)
            from MiembroGrupo m
            where m.grupo.id = :grupoId
              and m.estado = 'ACTIVO'
            """)
    long countMiembrosActivos(@Param("grupoId") Long grupoId);

    @Query("""
            select count(m)
            from MiembroGrupo m
            where m.grupo.id = :grupoId
              and m.estado = 'ACTIVO'
              and m.usuario.enLinea = true
            """)
    long countMiembrosActivosEnLinea(@Param("grupoId") Long grupoId);

    @Query("""
            select case when count(m) > 0 then true else false end
            from MiembroGrupo m
            where m.grupo.id = :grupoId
              and m.usuario.id = :usuarioId
              and m.estado = 'ACTIVO'
            """)
    boolean esMiembroActivo(
            @Param("grupoId") Long grupoId,
            @Param("usuarioId") Long usuarioId
    );

    @Query("""
            select m
            from MiembroGrupo m
            where m.grupo.id = :grupoId
              and m.usuario.id = :usuarioId
            """)
    Optional<MiembroGrupo> findMiembroByGrupoAndUsuario(
            @Param("grupoId") Long grupoId,
            @Param("usuarioId") Long usuarioId
    );
}
