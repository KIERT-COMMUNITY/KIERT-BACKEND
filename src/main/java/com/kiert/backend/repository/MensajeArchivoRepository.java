package com.kiert.backend.repository;

import com.kiert.backend.entity.MensajeArchivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MensajeArchivoRepository extends JpaRepository<MensajeArchivo, Long> {

    List<MensajeArchivo> findByMensajeIdOrderByIdAsc(Long mensajeId);

    List<MensajeArchivo> findByMensajeIdInOrderByMensajeIdAscIdAsc(
            List<Long> mensajesIds
    );

    boolean existsByPublicId(String publicId);

    void deleteByMensajeId(Long mensajeId);
}
