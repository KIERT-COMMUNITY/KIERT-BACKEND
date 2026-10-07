// src/main/java/com/kiert/backend/repository/SolicitudContactoRepository.java
package com.kiert.backend.repository;

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

    /**
     * Lista solicitudes recibidas por un usuario con un estado.
     *  MEJORA: incluye JOIN FETCH del emisor para evitar N+1.
     */
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

    /**
     * NUEVO: Versión paginada (recomendada para usuarios con muchas solicitudes).
     */
    @Query("""
            SELECT s FROM SolicitudContacto s
            LEFT JOIN FETCH s.emisor
            LEFT JOIN FETCH s.receptor
            WHERE s.receptor.id = :receptorId
              AND s.estado = :estado
            ORDER BY s.fechaSolicitud DESC
            """)
    Page<SolicitudContacto> findByReceptorIdAndEstadoPaginado(
            @Param("receptorId") Long receptorId,
            @Param("estado") EstadoSolicitud estado,
            Pageable pageable
    );

    // ============================================================
    // SOLICITUDES ENVIADAS
    // ============================================================

    /**
     * Lista solicitudes enviadas por un usuario con un estado.
     * MEJORA: incluye JOIN FETCH del receptor.
     */
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

    /**
     * NUEVO: Versión paginada.
     */
    @Query("""
            SELECT s FROM SolicitudContacto s
            LEFT JOIN FETCH s.emisor
            LEFT JOIN FETCH s.receptor
            WHERE s.emisor.id = :emisorId
              AND s.estado = :estado
            ORDER BY s.fechaSolicitud DESC
            """)
    Page<SolicitudContacto> findByEmisorIdAndEstadoPaginado(
            @Param("emisorId") Long emisorId,
            @Param("estado") EstadoSolicitud estado,
            Pageable pageable
    );

    // ============================================================
    // TODAS LAS SOLICITUDES DEL USUARIO (emisor O receptor)
    // ============================================================

    /**
     * Lista solicitudes donde el usuario es emisor O receptor.
     * MEJORA: incluye JOIN FETCH de ambos.
     */
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
    // BUSCAR SOLICITUD ESPECÍFICA
    // ============================================================

    /**
     * Busca una solicitud entre dos usuarios con un estado específico.
     * MEJORA: incluye JOIN FETCH.
     */
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

    /**
     *NUEVO: Busca solicitud en CUALQUIER dirección (A→B o B→A).
     * Útil para verificar si ya hay una solicitud pendiente.
     */
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

    /**
     * Verifica si existe una solicitud entre dos usuarios con un estado.
     */
    boolean existsByEmisorIdAndReceptorIdAndEstado(
            Long emisorId,
            Long receptorId,
            EstadoSolicitud estado
    );

    // ============================================================
    // VERIFICAR SI SON CONTACTOS
    // ============================================================

    /**
     * Verifica si dos usuarios son contactos (tienen una solicitud ACEPTADA).
     */
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
    // CONTADORES (para badges)
    // ============================================================

    /**
     *NUEVO: Cuenta solicitudes recibidas pendientes (badge navbar).
     */
    @Query("""
            SELECT COUNT(s) FROM SolicitudContacto s
            WHERE s.receptor.id = :usuarioId
              AND s.estado = 'PENDIENTE'
            """)
    long contarRecibidasPendientes(@Param("usuarioId") Long usuarioId);

    /**
     * NUEVO: Cuenta solicitudes enviadas pendientes.
     */
    @Query("""
            SELECT COUNT(s) FROM SolicitudContacto s
            WHERE s.emisor.id = :usuarioId
              AND s.estado = 'PENDIENTE'
            """)
    long contarEnviadasPendientes(@Param("usuarioId") Long usuarioId);

    /**
     *NUEVO: Cuenta total de contactos (solicitudes ACEPTADAS).
     */
    @Query("""
            SELECT COUNT(s) FROM SolicitudContacto s
            WHERE (s.emisor.id = :usuarioId OR s.receptor.id = :usuarioId)
              AND s.estado = 'ACEPTADA'
            """)
    long contarContactos(@Param("usuarioId") Long usuarioId);

    /**
     * NUEVO: Cuenta contactos de varios usuarios (bulk).
     * Devuelve [usuarioId, count].
     */
    @Query("""
            SELECT
                CASE WHEN s.emisor.id IN :usuarioIds THEN s.emisor.id
                     ELSE s.receptor.id
                END AS usuarioId,
                COUNT(s)
            FROM SolicitudContacto s
            WHERE (s.emisor.id IN :usuarioIds OR s.receptor.id IN :usuarioIds)
              AND s.estado = 'ACEPTADA'
            GROUP BY usuarioId
            """)
    List<Object[]> contarContactosDeUsuarios(@Param("usuarioIds") List<Long> usuarioIds);

    // ============================================================
    // LISTAR CONTACTOS
    // ============================================================

    /**
     * NUEVO: Lista los contactos (usuarios) de un usuario.
     * Devuelve los IDs de los contactos.
     */
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

    /**
     * NUEVO: Acepta varias solicitudes en 1 query.
     */
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

    /**
     * NUEVO: Rechaza varias solicitudes en 1 query.
     */
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

    /**
     *NUEVO: Cancela solicitudes pendientes enviadas por un usuario.
     */
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

    /**
     * NUEVO: Elimina la solicitud entre dos usuarios (al eliminar contacto).
     */
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
    // TOP USUARIOS (para detectar spam)
    // ============================================================

    /**
     *NUEVO: Top usuarios que más solicitudes envían (posible spam).
     * Devuelve [usuarioId, count].
     */
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

    /**
     *NUEVO: Top usuarios que más solicitudes reciben.
     */
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
    // ESTADÍSTICAS
    // ============================================================

    /**
     *NUEVO: Cuenta solicitudes agrupadas por estado.
     * Devuelve [estado, count].
     */
    @Query("""
            SELECT s.estado, COUNT(s)
            FROM SolicitudContacto s
            GROUP BY s.estado
            """)
    List<Object[]> contarPorEstado();

    /**
     * NUEVO: Total de solicitudes en el sistema.
     */
    @Query("SELECT COUNT(s) FROM SolicitudContacto s")
    long countTotal();

    // ============================================================
    // LIMPIEZA (para @Scheduled)
    // ============================================================

    /**
     *NUEVO: Elimina solicitudes rechazadas o canceladas antiguas.
     */
    @Modifying
    @Query("""
            DELETE FROM SolicitudContacto s
            WHERE s.estado IN ('RECHAZADA', 'CANCELADA')
              AND s.fechaRespuesta < :limite
            """)
    int eliminarRechazadasAntiguas(@Param("limite") Instant limite);
}