// src/main/java/com/kiert/backend/repository/ReporteRepository.java
package com.kiert.backend.repository;

import com.kiert.backend.entity.Reporte;
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
public interface ReporteRepository extends JpaRepository<Reporte, Long> {

    // ============================================================
    // VERIFICAR DUPLICADOS
    // ============================================================

    /**
     * Verifica si ya existe un reporte del mismo usuario para el mismo contenido.
     *
     * ️ PROBLEMA CONOCIDO: si pasas `null` en `postId` y `comentarioId`,
     * la condición `(:postId IS NULL OR r.post.id = :postId)` se cumple SIEMPRE,
     * lo cual puede dar falsos positivos.
     *
     *  SOLUCIÓN: usar la versión específica según el tipo de reporte.
     */
    @Query("""
            SELECT COUNT(r) > 0 FROM Reporte r
            WHERE r.usuarioReportante.id = :usuarioId
              AND r.tipoReporte = :tipo
              AND (:postId IS NULL OR r.post.id = :postId)
              AND (:comentarioId IS NULL OR r.comentario.id = :comentarioId)
              AND (:respuestaId IS NULL OR r.respuesta.id = :respuestaId)
              AND (:usuarioReportadoId IS NULL OR r.usuarioReportado.id = :usuarioReportadoId)
            """)
    boolean existeReporteDuplicado(
            @Param("usuarioId") Long usuarioId,
            @Param("tipo") String tipo,
            @Param("postId") Long postId,
            @Param("comentarioId") Long comentarioId,
            @Param("respuestaId") Long respuestaId,
            @Param("usuarioReportadoId") Long usuarioReportadoId
    );

    /**
     *NUEVO: Verifica duplicado de reporte a un POST (sin falsos positivos).
     */
    @Query("""
            SELECT COUNT(r) > 0 FROM Reporte r
            WHERE r.usuarioReportante.id = :usuarioId
              AND r.tipoReporte = 'POST'
              AND r.post.id = :postId
            """)
    boolean existeReporteDePost(
            @Param("usuarioId") Long usuarioId,
            @Param("postId") Long postId
    );

    /**
     * NUEVO: Verifica duplicado de reporte a un COMENTARIO.
     */
    @Query("""
            SELECT COUNT(r) > 0 FROM Reporte r
            WHERE r.usuarioReportante.id = :usuarioId
              AND r.tipoReporte = 'COMENTARIO'
              AND r.comentario.id = :comentarioId
            """)
    boolean existeReporteDeComentario(
            @Param("usuarioId") Long usuarioId,
            @Param("comentarioId") Long comentarioId
    );

    /**
     *NUEVO: Verifica duplicado de reporte a una RESPUESTA.
     */
    @Query("""
            SELECT COUNT(r) > 0 FROM Reporte r
            WHERE r.usuarioReportante.id = :usuarioId
              AND r.tipoReporte = 'RESPUESTA'
              AND r.respuesta.id = :respuestaId
            """)
    boolean existeReporteDeRespuesta(
            @Param("usuarioId") Long usuarioId,
            @Param("respuestaId") Long respuestaId
    );

    /**
     *  NUEVO: Verifica duplicado de reporte a un USUARIO.
     */
    @Query("""
            SELECT COUNT(r) > 0 FROM Reporte r
            WHERE r.usuarioReportante.id = :usuarioId
              AND r.tipoReporte = 'USUARIO'
              AND r.usuarioReportado.id = :usuarioReportadoId
            """)
    boolean existeReporteDeUsuario(
            @Param("usuarioId") Long usuarioId,
            @Param("usuarioReportadoId") Long usuarioReportadoId
    );

    // ============================================================
    // LISTAR REPORTES (admin)
    // ============================================================

    /**
     * Lista reportes con filtros (estado, tipo) y paginación.
     * Con JOIN FETCH de todas las relaciones para evitar N+1.
     */
    @Query("""
            SELECT r FROM Reporte r
            LEFT JOIN FETCH r.usuarioReportante
            LEFT JOIN FETCH r.usuarioReportado
            LEFT JOIN FETCH r.post
            LEFT JOIN FETCH r.comentario
            LEFT JOIN FETCH r.respuesta
            LEFT JOIN FETCH r.revisadoPor
            WHERE (:estado IS NULL OR r.estado = :estado)
              AND (:tipo IS NULL OR r.tipoReporte = :tipo)
            ORDER BY r.fechaCreacion DESC
            """)
    Page<Reporte> findWithFilters(
            @Param("estado") String estado,
            @Param("tipo") String tipo,
            Pageable pageable
    );

    /**
     * NUEVO: Reportes PENDIENTES (para el panel admin).
     */
    @Query("""
            SELECT r FROM Reporte r
            LEFT JOIN FETCH r.usuarioReportante
            LEFT JOIN FETCH r.usuarioReportado
            LEFT JOIN FETCH r.post
            LEFT JOIN FETCH r.comentario
            LEFT JOIN FETCH r.respuesta
            WHERE r.estado = 'PENDIENTE'
            ORDER BY r.fechaCreacion ASC
            """)
    Page<Reporte> findPendientes(Pageable pageable);

    /**
     * NUEVO: Reportes asignados a un moderador.
     */
    @Query("""
            SELECT r FROM Reporte r
            LEFT JOIN FETCH r.usuarioReportante
            LEFT JOIN FETCH r.usuarioReportado
            WHERE r.revisadoPor.id = :moderadorId
              AND r.estado IN ('REVISANDO', 'RESUELTO', 'RECHAZADO')
            ORDER BY r.fechaRevision DESC
            """)
    Page<Reporte> findRevisadosPorModerador(
            @Param("moderadorId") Long moderadorId,
            Pageable pageable
    );

    // ============================================================
    // REPORTES POR USUARIO
    // ============================================================

    /**
     * Reportes creados por un usuario.
     */
    @Query("""
            SELECT r FROM Reporte r
            LEFT JOIN FETCH r.post
            LEFT JOIN FETCH r.comentario
            LEFT JOIN FETCH r.respuesta
            LEFT JOIN FETCH r.usuarioReportado
            WHERE r.usuarioReportante.id = :usuarioId
            ORDER BY r.fechaCreacion DESC
            """)
    List<Reporte> findByUsuarioReportanteIdOrderByFechaCreacionDesc(
            @Param("usuarioId") Long usuarioId
    );

    /**
     * NUEVO: Versión paginada.
     */
    @Query("""
            SELECT r FROM Reporte r
            LEFT JOIN FETCH r.post
            LEFT JOIN FETCH r.comentario
            LEFT JOIN FETCH r.respuesta
            LEFT JOIN FETCH r.usuarioReportado
            WHERE r.usuarioReportante.id = :usuarioId
            ORDER BY r.fechaCreacion DESC
            """)
    Page<Reporte> findByUsuarioReportanteIdPaginado(
            @Param("usuarioId") Long usuarioId,
            Pageable pageable
    );

    /**
     * NUEVO: Reportes CONTRA un usuario específico.
     * Útil para ver "¿cuántas veces ha sido reportado X?".
     */
    @Query("""
            SELECT r FROM Reporte r
            LEFT JOIN FETCH r.usuarioReportante
            WHERE r.usuarioReportado.id = :usuarioReportadoId
            ORDER BY r.fechaCreacion DESC
            """)
    Page<Reporte> findByUsuarioReportadoId(
            @Param("usuarioReportadoId") Long usuarioReportadoId,
            Pageable pageable
    );

    /**
     * NUEVO: Cuenta reportes contra un usuario.
     */
    @Query("""
            SELECT COUNT(r) FROM Reporte r
            WHERE r.usuarioReportado.id = :usuarioReportadoId
            """)
    long countByUsuarioReportadoId(@Param("usuarioReportadoId") Long usuarioReportadoId);

    // ============================================================
    // OBTENER POR ID
    // ============================================================

    /**
     * NUEVO: Obtiene un reporte con todas sus relaciones cargadas.
     */
    @Query("""
            SELECT r FROM Reporte r
            LEFT JOIN FETCH r.usuarioReportante
            LEFT JOIN FETCH r.usuarioReportado
            LEFT JOIN FETCH r.post
            LEFT JOIN FETCH r.comentario
            LEFT JOIN FETCH r.respuesta
            LEFT JOIN FETCH r.revisadoPor
            WHERE r.id = :id
            """)
    Optional<Reporte> findByIdConRelaciones(@Param("id") Long id);

    // ============================================================
    // CONTADORES
    // ============================================================

    /**
     * Cuenta reportes por estado.
     * ⚠️ Se llama 4 veces en `obtenerResumen()`. Cachear en Redis.
     */
    long countByEstado(String estado);

    /**
     * NUEVO: Cuenta reportes agrupados por estado.
     * Devuelve [estado, count]. 1 query en lugar de 4.
     */
    @Query("""
            SELECT r.estado, COUNT(r)
            FROM Reporte r
            GROUP BY r.estado
            """)
    List<Object[]> contarPorEstado();

    /**
     * NUEVO: Cuenta reportes agrupados por tipo.
     * Devuelve [tipo, count].
     */
    @Query("""
            SELECT r.tipoReporte, COUNT(r)
            FROM Reporte r
            WHERE r.estado != 'RECHAZADO'
            GROUP BY r.tipoReporte
            """)
    List<Object[]> contarPorTipo();

    /**
     * NUEVO: Cuenta reportes agrupados por motivo.
     * Devuelve [motivo, count].
     */
    @Query("""
            SELECT r.motivo, COUNT(r)
            FROM Reporte r
            WHERE r.estado != 'RECHAZADO'
            GROUP BY r.motivo
            ORDER BY COUNT(r) DESC
            """)
    List<Object[]> contarPorMotivo();

    /**
     * NUEVO: Cuenta reportes por rango de fechas (para reportes).
     */
    @Query("""
            SELECT COUNT(r) FROM Reporte r
            WHERE r.fechaCreacion >= :desde
              AND r.fechaCreacion < :hasta
            """)
    long countEnRango(
            @Param("desde") Instant desde,
            @Param("hasta") Instant hasta
    );

    // ============================================================
    // TOP USUARIOS REPORTADOS (para detectar abusadores)
    // ============================================================

    /**
     * NUEVO: Top usuarios más reportados (no rechazados).
     * Devuelve [usuarioId, count].
     */
    @Query("""
            SELECT r.usuarioReportado.id, COUNT(r) as total
            FROM Reporte r
            WHERE r.usuarioReportado IS NOT NULL
              AND r.estado IN ('PENDIENTE', 'REVISANDO', 'RESUELTO')
            GROUP BY r.usuarioReportado.id
            ORDER BY total DESC
            """)
    List<Object[]> topUsuariosMasReportados(Pageable pageable);

    /**
     * NUEVO: Top usuarios que MÁS reportan (por si abusan del sistema).
     * Devuelve [usuarioId, count].
     */
    @Query("""
            SELECT r.usuarioReportante.id, COUNT(r) as total
            FROM Reporte r
            WHERE r.fechaCreacion >= :desde
            GROUP BY r.usuarioReportante.id
            ORDER BY total DESC
            """)
    List<Object[]> topUsuariosQueMasReportan(
            @Param("desde") Instant desde,
            Pageable pageable
    );

    // ============================================================
    // ACTUALIZAR ESTADO (bulk)
    // ============================================================

    /**
     * NUEVO: Cambia el estado de varios reportes en 1 query.
     * Útil para moderación en lote.
     */
    @Modifying
    @Query("""
            UPDATE Reporte r
            SET r.estado = :estado,
                r.revisadoPor.id = :moderadorId,
                r.fechaRevision = :fecha,
                r.notaModerador = :nota,
                r.accionTomada = :accion
            WHERE r.id IN :ids
              AND r.estado = 'PENDIENTE'
            """)
    int actualizarEstadoEnLote(
            @Param("ids") List<Long> ids,
            @Param("estado") String estado,
            @Param("moderadorId") Long moderadorId,
            @Param("fecha") Instant fecha,
            @Param("nota") String nota,
            @Param("accion") String accion
    );

    // ============================================================
    // LIMPIEZA (para @Scheduled)
    // ============================================================

    /**
     * NUEVO: Elimina reportes RESUELTOS/RECHAZADOS con más de N días.
     * Se ejecuta con un @Scheduled para mantener la tabla pequeña.
     */
    @Modifying
    @Query("""
            DELETE FROM Reporte r
            WHERE r.estado IN ('RESUELTO', 'RECHAZADO')
              AND r.fechaRevision < :limite
            """)
    int eliminarResueltosAntiguos(@Param("limite") Instant limite);

    /**
     * NUEVO: Cuenta cuántos reportes se eliminarán (para saber el impacto).
     */
    @Query("""
            SELECT COUNT(r) FROM Reporte r
            WHERE r.estado IN ('RESUELTO', 'RECHAZADO')
              AND r.fechaRevision < :limite
            """)
    long countResueltosAntiguos(@Param("limite") Instant limite);

    // ============================================================
    // ESTADÍSTICAS
    // ============================================================

    /**
     * NUEVO: Total de reportes en el sistema.
     */
    @Query("SELECT COUNT(r) FROM Reporte r")
    long countTotal();

    /**
     * NUEVO: Total de reportes activos (no resueltos ni rechazados).
     */
    @Query("""
            SELECT COUNT(r) FROM Reporte r
            WHERE r.estado IN ('PENDIENTE', 'REVISANDO')
            """)
    long countActivos();
}