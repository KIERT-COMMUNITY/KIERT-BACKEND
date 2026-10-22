// src/main/java/com/kiert/backend/repository/MensajeArchivoGrupoRepository.java
package com.kiert.backend.features.grupo.repository;

import com.kiert.backend.entity.MensajeArchivoGrupo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MensajeArchivoGrupoRepository
        extends JpaRepository<MensajeArchivoGrupo, Long> {

    List<MensajeArchivoGrupo> findByMensajeGrupoIdOrderByIdAsc(Long mensajeGrupoId);

    List<MensajeArchivoGrupo> findByMensajeGrupoIdInOrderByMensajeGrupoIdAscIdAsc(
            List<Long> mensajesGrupoIds
    );

    boolean existsByPublicId(String publicId);

    void deleteByMensajeGrupoId(Long mensajeGrupoId);

    // ============================================================
    // SEGURIDAD: Validar pertenencia del archivo
    // ============================================================

    /**
     * NUEVO: Verifica si un archivo pertenece a un mensaje de un grupo
     * donde el usuario es miembro activo.
     */
    @Query("""
            SELECT COUNT(a) > 0 FROM MensajeArchivoGrupo a
            WHERE a.id = :archivoId
              AND EXISTS (
                SELECT 1 FROM MiembroGrupo m
                WHERE m.grupo.id = a.mensajeGrupo.grupo.id
                  AND m.usuario.id = :usuarioId
                  AND m.estado = 'ACTIVO'
              )
            """)
    boolean usuarioPuedeVerArchivo(
            @Param("archivoId") Long archivoId,
            @Param("usuarioId") Long usuarioId
    );
}