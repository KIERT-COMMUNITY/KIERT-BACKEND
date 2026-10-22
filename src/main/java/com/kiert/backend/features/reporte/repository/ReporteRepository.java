// src/main/java/com/kiert/backend/repository/ReporteRepository.java
package com.kiert.backend.features.reporte.repository;

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

    @Query("""
            SELECT COUNT(r) FROM Reporte r
            WHERE r.usuarioReportado.id = :usuarioReportadoId
            """)
    long countByUsuarioReportadoId(@Param("usuarioReportadoId") Long usuarioReportadoId);

    // ============================================================
    // OBTENER POR ID
    // ============================================================

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

    long countByEstado(String estado);

    @Query("""
            SELECT r.estado, COUNT(r)
            FROM Reporte r
            GROUP BY r.estado
            """)
    List<Object[]> contarPorEstado();

    @Query("""
            SELECT r.tipoReporte, COUNT(r)
            FROM Reporte r
            WHERE r.estado != 'RECHAZADO'
            GROUP BY r.tipoReporte
            """)
    List<Object[]> contarPorTipo();

    @Query("""
            SELECT r.motivo, COUNT(r)
            FROM Reporte r
            WHERE r.estado != 'RECHAZADO'
            GROUP BY r.motivo
            ORDER BY COUNT(r) DESC
            """)
    List<Object[]> contarPorMotivo();

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
    // TOP USUARIOS REPORTADOS
    // ============================================================

    @Query("""
            SELECT r.usuarioReportado.id, COUNT(r) as total
            FROM Reporte r
            WHERE r.usuarioReportado IS NOT NULL
              AND r.estado IN ('PENDIENTE', 'REVISANDO', 'RESUELTO')
            GROUP BY r.usuarioReportado.id
            ORDER BY total DESC
            """)
    List<Object[]> topUsuariosMasReportados(Pageable pageable);

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

    @Modifying
    @Query("""
            DELETE FROM Reporte r
            WHERE r.estado IN ('RESUELTO', 'RECHAZADO')
              AND r.fechaRevision < :limite
            """)
    int eliminarResueltosAntiguos(@Param("limite") Instant limite);

    @Query("""
            SELECT COUNT(r) FROM Reporte r
            WHERE r.estado IN ('RESUELTO', 'RECHAZADO')
              AND r.fechaRevision < :limite
            """)
    long countResueltosAntiguos(@Param("limite") Instant limite);

    // ============================================================
    // ESTADÍSTICAS
    // ============================================================

    @Query("SELECT COUNT(r) FROM Reporte r")
    long countTotal();

    @Query("""
            SELECT COUNT(r) FROM Reporte r
            WHERE r.estado IN ('PENDIENTE', 'REVISANDO')
            """)
    long countActivos();

    // ============================================================
    // SEGURIDAD: Validar permisos
    // ============================================================

    /**
     * NUEVO: Verifica si el usuario es el autor del reporte (para verlo/eliminarlo).
     */
    @Query("""
            SELECT COUNT(r) > 0 FROM Reporte r
            WHERE r.id = :reporteId
              AND r.usuarioReportante.id = :usuarioId
            """)
    boolean esAutorDelReporte(
            @Param("reporteId") Long reporteId,
            @Param("usuarioId") Long usuarioId
    );

    /**
     * NUEVO: Verifica si el usuario es moderador/admin (para poder cambiar estado).
     */
    @Query("""
            SELECT COUNT(u) > 0 FROM Usuario u
            WHERE u.id = :moderadorId
              AND u.rol IN ('CEO', 'ADMIN', 'MODERADOR')
              AND u.eliminado = false
              AND u.activo = true
            """)
    boolean esModeradorValido(@Param("moderadorId") Long moderadorId);
}