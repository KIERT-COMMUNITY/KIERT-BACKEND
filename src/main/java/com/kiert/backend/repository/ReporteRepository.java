package com.kiert.backend.repository;



import com.kiert.backend.entity.Reporte;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReporteRepository extends JpaRepository<Reporte, Long> {

    // Verificar si ya existe un reporte del mismo usuario para el mismo contenido
    @Query("SELECT COUNT(r) > 0 FROM Reporte r WHERE " +
            "r.usuarioReportante.id = :usuarioId AND " +
            "r.tipoReporte = :tipo AND " +
            "(:postId IS NULL OR r.post.id = :postId) AND " +
            "(:comentarioId IS NULL OR r.comentario.id = :comentarioId) AND " +
            "(:respuestaId IS NULL OR r.respuesta.id = :respuestaId) AND " +
            "(:usuarioReportadoId IS NULL OR r.usuarioReportado.id = :usuarioReportadoId)")
    boolean existeReporteDuplicado(
            @Param("usuarioId") Long usuarioId,
            @Param("tipo") String tipo,
            @Param("postId") Long postId,
            @Param("comentarioId") Long comentarioId,
            @Param("respuestaId") Long respuestaId,
            @Param("usuarioReportadoId") Long usuarioReportadoId
    );

    // Listar todos los reportes con filtros
    @Query("SELECT r FROM Reporte r " +
            "LEFT JOIN FETCH r.usuarioReportante " +
            "LEFT JOIN FETCH r.usuarioReportado " +
            "LEFT JOIN FETCH r.post " +
            "LEFT JOIN FETCH r.comentario " +
            "LEFT JOIN FETCH r.respuesta " +
            "WHERE (:estado IS NULL OR r.estado = :estado) " +
            "AND (:tipo IS NULL OR r.tipoReporte = :tipo) " +
            "ORDER BY r.fechaCreacion DESC")
    Page<Reporte> findWithFilters(
            @Param("estado") String estado,
            @Param("tipo") String tipo,
            Pageable pageable
    );

    // Reportes de un usuario específico
    List<Reporte> findByUsuarioReportanteIdOrderByFechaCreacionDesc(Long usuarioId);

    // Contar por estado
    long countByEstado(String estado);
}