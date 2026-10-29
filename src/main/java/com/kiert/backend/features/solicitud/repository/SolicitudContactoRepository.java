// src/main/java/com/kiert/backend/features/solicitud/repository/SolicitudContactoRepository.java
package com.kiert.backend.features.solicitud.repository;

import com.kiert.backend.entity.SolicitudContacto;
import com.kiert.backend.entity.SolicitudContacto.EstadoSolicitud;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SolicitudContactoRepository extends JpaRepository<SolicitudContacto, Long> {

    // ============================================================
    // SOLICITUDES RECIBIDAS
    // ============================================================

    @Query("""
            SELECT s FROM SolicitudContacto s
            LEFT JOIN FETCH s.emisor
            LEFT JOIN FETCH s.receptor
            WHERE s.receptor.id = :receptorId
              AND s.estado = :estado
            ORDER BY s.fechaSolicitud DESC
            """)
    List<SolicitudContacto> findByReceptorIdAndEstado(
            @Param("receptorId") Long receptorId,
            @Param("estado") EstadoSolicitud estado
    );

    @Query(value = """
            SELECT s FROM SolicitudContacto s
            LEFT JOIN FETCH s.emisor
            LEFT JOIN FETCH s.receptor
            WHERE s.receptor.id = :receptorId
              AND s.estado = :estado
            ORDER BY s.fechaSolicitud DESC
            """,
            countQuery = """
            SELECT COUNT(s) FROM SolicitudContacto s
            WHERE s.receptor.id = :receptorId
              AND s.estado = :estado
            """)
    Page<SolicitudContacto> findByReceptorIdAndEstadoPaginado(
            @Param("receptorId") Long receptorId,
            @Param("estado") EstadoSolicitud estado,
            Pageable pageable
    );

    // ============================================================
    // SOLICITUDES ENVIADAS
    // ============================================================

    @Query("""
            SELECT s FROM SolicitudContacto s
            LEFT JOIN FETCH s.emisor
            LEFT JOIN FETCH s.receptor
            WHERE s.emisor.id = :emisorId
              AND s.estado = :estado
            ORDER BY s.fechaSolicitud DESC
            """)
    List<SolicitudContacto> findByEmisorIdAndEstado(
            @Param("emisorId") Long emisorId,
            @Param("estado") EstadoSolicitud estado
    );

    @Query(value = """
            SELECT s FROM SolicitudContacto s
            LEFT JOIN FETCH s.emisor
            LEFT JOIN FETCH s.receptor
            WHERE s.emisor.id = :emisorId
              AND s.estado = :estado
            ORDER BY s.fechaSolicitud DESC
            """,
            countQuery = """
            SELECT COUNT(s) FROM SolicitudContacto s
            WHERE s.emisor.id = :emisorId
              AND s.estado = :estado
            """)
    Page<SolicitudContacto> findByEmisorIdAndEstadoPaginado(
            @Param("emisorId") Long emisorId,
            @Param("estado") EstadoSolicitud estado,
            Pageable pageable
    );

    // ============================================================
    // TODAS LAS SOLICITUDES DEL USUARIO
    // ============================================================

    @Query("""
            SELECT s FROM SolicitudContacto s
            LEFT JOIN FETCH s.emisor
            LEFT JOIN FETCH s.receptor
            WHERE (s.emisor.id = :usuarioId OR s.receptor.id = :usuarioId)
              AND s.estado = :estado
            ORDER BY s.fechaSolicitud DESC
            """)
    List<SolicitudContacto> findAllByUsuarioIdAndEstado(
            @Param("usuarioId") Long usuarioId,
            @Param("estado") EstadoSolicitud estado
    );

    // ============================================================
    // BUSCAR SOLICITUD ESPECIFICA
    // ============================================================

    @Query("""
            SELECT s FROM SolicitudContacto s
            LEFT JOIN FETCH s.emisor
            LEFT JOIN FETCH s.receptor
            WHERE s.emisor.id = :emisorId
              AND s.receptor.id = :receptorId
              AND s.estado = :estado
            """)
    Optional<SolicitudContacto> findByEmisorIdAndReceptorIdAndEstado(
            @Param("emisorId") Long emisorId,
            @Param("receptorId") Long receptorId,
            @Param("estado") EstadoSolicitud estado
    );

    @Query("""
            SELECT s FROM SolicitudContacto s
            LEFT JOIN FETCH s.emisor
            LEFT JOIN FETCH s.receptor
            WHERE (
                (s.emisor.id = :usuarioA AND s.receptor.id = :usuarioB)
                OR
                (s.emisor.id = :usuarioB AND s.receptor.id = :usuarioA)
              )
              AND s.estado = :estado
            """)
    Optional<SolicitudContacto> findByUsuariosYEstado(
            @Param("usuarioA") Long usuarioA,
            @Param("usuarioB") Long usuarioB,
            @Param("estado") EstadoSolicitud estado
    );

    boolean existsByEmisorIdAndReceptorIdAndEstado(
            Long emisorId,
            Long receptorId,
            EstadoSolicitud estado
    );

    // ============================================================
    // VERIFICAR SI SON CONTACTOS
    // ============================================================

    @Query("""
            SELECT COUNT(s) > 0 FROM SolicitudContacto s
            WHERE (
                (s.emisor.id = :usuario1 AND s.receptor.id = :usuario2)
                OR
                (s.emisor.id = :usuario2 AND s.receptor.id = :usuario1)
              )
              AND s.estado = 'ACEPTADA'
            """)
    boolean sonContactos(
            @Param("usuario1") Long usuario1,
            @Param("usuario2") Long usuario2
    );

    // ============================================================
    // CONTADORES (badges)
    // ============================================================

    @Query("""
            SELECT COUNT(s) FROM SolicitudContacto s
            WHERE s.receptor.id = :usuarioId
              AND s.estado = 'PENDIENTE'
            """)
    long contarRecibidasPendientes(@Param("usuarioId") Long usuarioId);

    @Query("""
            SELECT COUNT(s) FROM SolicitudContacto s
            WHERE s.emisor.id = :usuarioId
              AND s.estado = 'PENDIENTE'
            """)
    long contarEnviadasPendientes(@Param("usuarioId") Long usuarioId);

    @Query("""
            SELECT COUNT(s) FROM SolicitudContacto s
            WHERE (s.emisor.id = :usuarioId OR s.receptor.id = :usuarioId)
              AND s.estado = 'ACEPTADA'
            """)
    long contarContactos(@Param("usuarioId") Long usuarioId);

    /**
     * Cuenta contactos aceptados por usuario en una lista.
     * Nota: usa UNION para evitar doble conteo cuando el usuario esta en ambos lados.
     * El resultado es una lista de [usuarioId, count].
     */
    @Query(value = """
            SELECT contacto_usuario_id, COUNT(*) FROM (
                SELECT CASE
                    WHEN s.emisor_id IN (:usuarioIds) THEN s.emisor_id
                    ELSE s.receptor_id
                END AS contacto_usuario_id
                FROM solicitudes_contacto s
                WHERE (s.emisor_id IN (:usuarioIds) OR s.receptor_id IN (:usuarioIds))
                  AND s.estado = 'ACEPTADA'
            ) sub
            GROUP BY contacto_usuario_id
            """, nativeQuery = true)
    List<Object[]> contarContactosDeUsuarios(@Param("usuarioIds") List<Long> usuarioIds);

    // ============================================================
    // LISTAR CONTACTOS
    // ============================================================

    @Query("""
            SELECT CASE
                WHEN s.emisor.id = :usuarioId THEN s.receptor.id
                ELSE s.emisor.id
            END
            FROM SolicitudContacto s
            WHERE (s.emisor.id = :usuarioId OR s.receptor.id = :usuarioId)
              AND s.estado = 'ACEPTADA'
            """)
    List<Long> findContactosIds(@Param("usuarioId") Long usuarioId);

    // ============================================================
    // ACTUALIZAR ESTADO (bulk)
    // ============================================================

    @Modifying
    @Query("""
            UPDATE SolicitudContacto s
            SET s.estado = 'ACEPTADA',
                s.fechaRespuesta = :fecha
            WHERE s.id IN :ids
              AND s.receptor.id = :usuarioId
              AND s.estado = 'PENDIENTE'
            """)
    int aceptarEnLote(
            @Param("ids") List<Long> ids,
            @Param("usuarioId") Long usuarioId,
            @Param("fecha") Instant fecha
    );

    @Modifying
    @Query("""
            UPDATE SolicitudContacto s
            SET s.estado = 'RECHAZADA',
                s.fechaRespuesta = :fecha
            WHERE s.id IN :ids
              AND s.receptor.id = :usuarioId
              AND s.estado = 'PENDIENTE'
            """)
    int rechazarEnLote(
            @Param("ids") List<Long> ids,
            @Param("usuarioId") Long usuarioId,
            @Param("fecha") Instant fecha
    );

    @Modifying
    @Query("""
            UPDATE SolicitudContacto s
            SET s.estado = 'RECHAZADA',
                s.fechaRespuesta = :fecha
            WHERE s.id IN :ids
              AND s.emisor.id = :usuarioId
              AND s.estado = 'PENDIENTE'
            """)
    int cancelarEnLote(
            @Param("ids") List<Long> ids,
            @Param("usuarioId") Long usuarioId,
            @Param("fecha") Instant fecha
    );

    // ============================================================
    // ELIMINAR
    // ============================================================

    @Modifying
    @Query("""
            DELETE FROM SolicitudContacto s
            WHERE (s.emisor.id = :usuarioId AND s.receptor.id = :otroUsuarioId)
               OR (s.emisor.id = :otroUsuarioId AND s.receptor.id = :usuarioId)
            """)
    int eliminarEntreUsuarios(
            @Param("usuarioId") Long usuarioId,
            @Param("otroUsuarioId") Long otroUsuarioId
    );

    // ============================================================
    // TOP USUARIOS (spam)
    // ============================================================

    @Query("""
            SELECT s.emisor.id, COUNT(s) as total
            FROM SolicitudContacto s
            WHERE s.fechaSolicitud >= :desde
            GROUP BY s.emisor.id
            ORDER BY total DESC
            """)
    List<Object[]> topUsuariosQueMasSolicitan(
            @Param("desde") Instant desde,
            Pageable pageable
    );

    @Query("""
            SELECT s.receptor.id, COUNT(s) as total
            FROM SolicitudContacto s
            WHERE s.fechaSolicitud >= :desde
            GROUP BY s.receptor.id
            ORDER BY total DESC
            """)
    List<Object[]> topUsuariosQueMasReciben(
            @Param("desde") Instant desde,
            Pageable pageable
    );

    // ============================================================
    // ESTADISTICAS
    // ============================================================

    @Query("""
            SELECT s.estado, COUNT(s)
            FROM SolicitudContacto s
            GROUP BY s.estado
            """)
    List<Object[]> contarPorEstado();

    @Query("SELECT COUNT(s) FROM SolicitudContacto s")
    long countTotal();

    // ============================================================
    // LIMPIEZA (para @Scheduled)
    // ============================================================

    @Modifying
    @Query("""
            DELETE FROM SolicitudContacto s
            WHERE s.estado IN ('RECHAZADA', 'CANCELADA')
              AND s.fechaRespuesta < :limite
            """)
    int eliminarRechazadasAntiguas(@Param("limite") Instant limite);

    // ============================================================
    // SEGURIDAD: Validar permisos
    // ============================================================

    @Query("""
            SELECT COUNT(s) > 0 FROM SolicitudContacto s
            WHERE s.id = :solicitudId
              AND s.receptor.id = :usuarioId
              AND s.estado = 'PENDIENTE'
            """)
    boolean esReceptorDeSolicitud(
            @Param("solicitudId") Long solicitudId,
            @Param("usuarioId") Long usuarioId
    );

    @Query("""
            SELECT COUNT(s) > 0 FROM SolicitudContacto s
            WHERE s.id = :solicitudId
              AND s.emisor.id = :usuarioId
              AND s.estado = 'PENDIENTE'
            """)
    boolean esEmisorDeSolicitud(
            @Param("solicitudId") Long solicitudId,
            @Param("usuarioId") Long usuarioId
    );
}