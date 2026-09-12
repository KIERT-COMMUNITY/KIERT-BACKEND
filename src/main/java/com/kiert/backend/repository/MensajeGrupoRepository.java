package com.kiert.backend.repository;

import com.kiert.backend.entity.MensajeGrupo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MensajeGrupoRepository extends JpaRepository<MensajeGrupo, Long> {

    @Query("SELECT m FROM MensajeGrupo m " +
            "JOIN FETCH m.emisor " +
            "WHERE m.grupo.id = :grupoId " +
            "AND m.eliminado = false " +
            "ORDER BY m.fechaEnvio ASC")
    List<MensajeGrupo> findMensajesDeGrupo(@Param("grupoId") Long grupoId);
}