package com.kiert.backend.repository;

import com.kiert.backend.entity.MensajeGrupo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MensajeGrupoRepository extends JpaRepository<MensajeGrupo, Long> {

    @Query("""
            select distinct m
            from MensajeGrupo m
            join fetch m.emisor
            left join fetch m.archivos
            where m.grupo.id = :grupoId
              and m.eliminado = false
            order by m.fechaEnvio asc
            """)
    List<MensajeGrupo> findMensajesDeGrupo(@Param("grupoId") Long grupoId);
}
