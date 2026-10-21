// src/main/java/com/kiert/backend/repository/MensajeGrupoRepository.java
package com.kiert.backend.repository;

import com.kiert.backend.entity.MensajeGrupo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface MensajeGrupoRepository extends JpaRepository<MensajeGrupo, Long> {

    @Query("""
            SELECT m FROM MensajeGrupo m
            JOIN FETCH m.emisor
            WHERE m.grupo.id = :grupoId
              AND m.eliminado = false
            ORDER BY m.fechaEnvio ASC
            """)
    List<MensajeGrupo> findMensajesDeGrupo(@Param("grupoId") Long grupoId);

    @Query("""
            SELECT m FROM MensajeGrupo m
            JOIN FETCH m.emisor
            WHERE m.grupo.id = :grupoId
              AND m.eliminado = false
            ORDER BY m.fechaEnvio DESC
            """)
    Page<MensajeGrupo> findMensajesDeGrupoPaginado(
            @Param("grupoId") Long grupoId,
            Pageable pageable
    );

    @Query("""
            SELECT m FROM MensajeGrupo m
            JOIN FETCH m.emisor
            WHERE m.grupo.id = :grupoId
              AND m.eliminado = false
              AND m.fechaEnvio > :desde
            ORDER BY m.fechaEnvio ASC
            """)
    List<MensajeGrupo> findMensajesDesde(
            @Param("grupoId") Long grupoId,
            @Param("desde") Instant desde
    );

    @Query("""
            SELECT m FROM MensajeGrupo m
            JOIN FETCH m.emisor
            WHERE m.grupo.id = :grupoId
              AND m.eliminado = false
              AND m.fechaEnvio BETWEEN :desde AND :hasta
            ORDER BY m.fechaEnvio ASC
            """)
    List<MensajeGrupo> findMensajesEnRango(
            @Param("grupoId") Long grupoId,
            @Param("desde") Instant desde,
            @Param("hasta") Instant hasta
    );

    @Query("""
            SELECT m FROM MensajeGrupo m
            JOIN FETCH m.emisor
            WHERE m.grupo.id = :grupoId
              AND m.eliminado = false
            ORDER BY m.fechaEnvio DESC
            """)
    List<MensajeGrupo> findUltimoMensaje(
            @Param("grupoId") Long grupoId,
            Pageable pageable
    );

    @Query(value = """
            SELECT m.*
            FROM mensajes_grupo m
            INNER JOIN (
                SELECT grupo_id, MAX(fecha_envio) AS max_fecha
                FROM mensajes_grupo
                WHERE eliminado = false
                  AND grupo_id IN (:grupoIds)
                GROUP BY grupo_id
            ) ultimos
            ON m.grupo_id = ultimos.grupo_id
            AND m.fecha_envio = ultimos.max_fecha
            WHERE m.eliminado = false
            ORDER BY m.fecha_envio DESC
            """, nativeQuery = true)
    List<MensajeGrupo> findUltimosMensajesDeGrupos(@Param("grupoIds") List<Long> grupoIds);

    @Query("""
            SELECT COUNT(m) FROM MensajeGrupo m
            WHERE m.grupo.id = :grupoId
              AND m.eliminado = false
            """)
    long countByGrupoId(@Param("grupoId") Long grupoId);

    @Query("""
            SELECT COUNT(m) FROM MensajeGrupo m
            WHERE m.grupo.id = :grupoId
              AND m.emisor.id = :emisorId
              AND m.eliminado = false
            """)
    long countByGrupoAndEmisor(
            @Param("grupoId") Long grupoId,
            @Param("emisorId") Long emisorId
    );

    @Query("""
            SELECT m.grupo.id, COUNT(m)
            FROM MensajeGrupo m
            WHERE m.grupo.id IN :grupoIds
              AND m.eliminado = false
            GROUP BY m.grupo.id
            """)
    List<Object[]> contarMensajesPorGrupos(@Param("grupoIds") List<Long> grupoIds);

    @Query("""
            SELECT m FROM MensajeGrupo m
            JOIN FETCH m.emisor
            WHERE m.grupo.id = :grupoId
              AND m.emisor.id = :emisorId
              AND m.eliminado = false
            ORDER BY m.fechaEnvio DESC
            """)
    Page<MensajeGrupo> findByGrupoAndEmisor(
            @Param("grupoId") Long grupoId,
            @Param("emisorId") Long emisorId,
            Pageable pageable
    );

    @Query("""
            SELECT m FROM MensajeGrupo m
            JOIN FETCH m.emisor
            WHERE m.grupo.id = :grupoId
              AND m.eliminado = false
              AND m.urlArchivo IS NOT NULL
            ORDER BY m.fechaEnvio DESC
            """)
    Page<MensajeGrupo> findMensajesConArchivos(
            @Param("grupoId") Long grupoId,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(m) FROM MensajeGrupo m
            WHERE m.grupo.id = :grupoId
              AND m.eliminado = false
              AND m.urlArchivo IS NOT NULL
            """)
    long countArchivosByGrupo(@Param("grupoId") Long grupoId);

    @Query("""
            SELECT m FROM MensajeGrupo m
            JOIN FETCH m.emisor
            WHERE m.grupo.id = :grupoId
              AND m.eliminado = false
              AND LOWER(m.contenido) LIKE LOWER(CONCAT('%', :query, '%'))
            ORDER BY m.fechaEnvio DESC
            """)
    Page<MensajeGrupo> buscarEnGrupo(
            @Param("grupoId") Long grupoId,
            @Param("query") String query,
            Pageable pageable
    );

    @Modifying
    @Query("""
            UPDATE MensajeGrupo m
            SET m.eliminado = true
            WHERE m.grupo.id = :grupoId
              AND m.eliminado = false
            """)
    int eliminarPorGrupo(@Param("grupoId") Long grupoId);

    @Modifying
    @Query("""
            UPDATE MensajeGrupo m
            SET m.eliminado = true
            WHERE m.grupo.id = :grupoId
              AND m.emisor.id = :emisorId
              AND m.eliminado = false
            """)
    int eliminarPorGrupoYEmisor(
            @Param("grupoId") Long grupoId,
            @Param("emisorId") Long emisorId
    );

    @Modifying
    @Query(value = """
            DELETE FROM mensajes_grupo
            WHERE eliminado = true
              AND fecha_envio < :limite
            """, nativeQuery = true)
    int eliminarMensajesBorradosAntiguos(@Param("limite") Instant limite);

    @Query("SELECT COUNT(m) FROM MensajeGrupo m WHERE m.eliminado = false")
    long countActivosGlobales();

    @Query("""
            SELECT m.grupo.id, COUNT(m) as total
            FROM MensajeGrupo m
            WHERE m.eliminado = false
            GROUP BY m.grupo.id
            ORDER BY total DESC
            """)
    List<Object[]> topGruposConMasMensajes(Pageable pageable);

    // ============================================================
    // SEGURIDAD: Validar que el usuario es miembro del grupo
    // ============================================================

    /**
     * NUEVO: Verifica si un usuario puede ver los mensajes de un grupo.
     * Solo si es miembro activo.
     */
    @Query("""
            SELECT COUNT(m) > 0 FROM MensajeGrupo m
            WHERE m.grupo.id = :grupoId
              AND EXISTS (
                SELECT 1 FROM MiembroGrupo mg
                WHERE mg.grupo.id = :grupoId
                  AND mg.usuario.id = :usuarioId
                  AND mg.estado = 'ACTIVO'
              )
            """)
    boolean usuarioEsMiembro(
            @Param("grupoId") Long grupoId,
            @Param("usuarioId") Long usuarioId
    );
}