// src/main/java/com/kiert/backend/repository/MensajeArchivoRepository.java
package com.kiert.backend.features.mensajes.repository;

import com.kiert.backend.entity.MensajeArchivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    // ============================================================
    // SEGURIDAD: Validar pertenencia del archivo
    // ============================================================

    /**
     * NUEVO: Verifica si un usuario es emisor o receptor del mensaje
     * al que pertenece el archivo.
     */
    @Query("""
            SELECT COUNT(a) > 0 FROM MensajeArchivo a
            WHERE a.id = :archivoId
              AND (
                a.mensaje.emisor.id = :usuarioId
                OR a.mensaje.receptor.id = :usuarioId
              )
            """)
    boolean usuarioPuedeVerArchivo(
            @Param("archivoId") Long archivoId,
            @Param("usuarioId") Long usuarioId
    );
}