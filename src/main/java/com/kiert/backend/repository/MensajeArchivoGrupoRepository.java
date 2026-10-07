package com.kiert.backend.repository;

import com.kiert.backend.entity.MensajeArchivoGrupo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MensajeArchivoGrupoRepository
        extends JpaRepository<MensajeArchivoGrupo, Long> {

    List<MensajeArchivoGrupo> findByMensajeGrupoIdOrderByIdAsc(
            Long mensajeGrupoId
    );

    List<MensajeArchivoGrupo> findByMensajeGrupoIdInOrderByMensajeGrupoIdAscIdAsc(
            List<Long> mensajesGrupoIds
    );

    boolean existsByPublicId(String publicId);

    void deleteByMensajeGrupoId(Long mensajeGrupoId);
}
