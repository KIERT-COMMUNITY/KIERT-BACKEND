// src/main/java/com/kiert/backend/service/ReporteService.java
package com.kiert.backend.features.reporte.service;

import com.kiert.backend.entity.*;
import com.kiert.backend.exception.BadRequestException;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.features.comentarios.repository.ComentarioRepository;
import com.kiert.backend.features.comentarios.repository.RespuestaComentarioRepository;
import com.kiert.backend.features.post.repository.PostRepository;
import com.kiert.backend.features.reporte.dto.ActualizarReporteDTO;
import com.kiert.backend.features.reporte.dto.CrearReporteDTO;
import com.kiert.backend.features.reporte.dto.ReporteDTO;
import com.kiert.backend.features.reporte.dto.ReporteResumenDTO;
import com.kiert.backend.features.reporte.repository.ReporteRepository;
import com.kiert.backend.features.usuario.repository.UsuarioRepository;
import com.kiert.backend.features.notificaciones.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReporteService {

    private final ReporteRepository reporteRepository;
    private final UsuarioRepository usuarioRepository;
    private final PostRepository postRepository;
    private final ComentarioRepository comentarioRepository;
    private final RespuestaComentarioRepository respuestaRepository;
    private final NotificacionService notificacionService;

    private static final String CACHE_RESUMEN_REPORTES = "resumenReportes";
    private static final String CACHE_MIS_REPORTES = "misReportes";

    private static final List<String> MOTIVOS_VALIDOS = List.of(
            "SPAM", "ACOSO", "CONTENIDO_INAPROPIADO", "VIOLENCIA",
            "DERECHOS_AUTOR", "INFORMACION_FALSA", "OTRO"
    );

    private static final List<String> TIPOS_VALIDOS = List.of(
            "POST", "COMENTARIO", "RESPUESTA", "USUARIO"
    );

    private static final List<String> ESTADOS_VALIDOS = List.of(
            "PENDIENTE", "REVISANDO", "RESUELTO", "RECHAZADO"
    );

    private static final List<String> ACCIONES_VALIDAS = List.of(
            "NINGUNA", "ADVERTENCIA", "ELIMINADO", "BANEADO"
    );

    private static final int MAX_DESCRIPCION = 1000;
    private static final int MAX_NOTA = 1000;

    // ============================================================
    // CREAR REPORTE
    // ============================================================
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_RESUMEN_REPORTES, allEntries = true),
            @CacheEvict(value = CACHE_MIS_REPORTES, key = "'usuario:' + #usuarioReportanteId")
    })
    public ReporteDTO crearReporte(Long usuarioReportanteId, CrearReporteDTO dto, String ip) {
        log.info("Creando reporte tipo={} motivo={} por usuario={}",
                dto.tipoReporte(), dto.motivo(), usuarioReportanteId);

        // Validaciones
        if (usuarioReportanteId == null || dto == null) {
            throw new BadRequestException("Parámetros inválidos");
        }
        if (dto.tipoReporte() == null || !TIPOS_VALIDOS.contains(dto.tipoReporte())) {
            throw new BadRequestException("Tipo de reporte inválido");
        }
        if (dto.motivo() == null || !MOTIVOS_VALIDOS.contains(dto.motivo())) {
            throw new BadRequestException("Motivo inválido");
        }
        if (dto.descripcion() == null || dto.descripcion().trim().length() < 10) {
            throw new BadRequestException("La descripción debe tener al menos 10 caracteres");
        }
        if (dto.descripcion().length() > MAX_DESCRIPCION) {
            throw new BadRequestException("La descripción es demasiado larga");
        }

        Usuario reportante = usuarioRepository.findById(usuarioReportanteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        // Validar duplicados según el tipo (usando los métodos existentes del repository)
        boolean yaReportado = switch (dto.tipoReporte()) {
            case "POST" -> dto.postId() != null &&
                    reporteRepository.existeReporteDePost(usuarioReportanteId, dto.postId());
            case "COMENTARIO" -> dto.comentarioId() != null &&
                    reporteRepository.existeReporteDeComentario(usuarioReportanteId, dto.comentarioId());
            case "RESPUESTA" -> dto.respuestaId() != null &&
                    reporteRepository.existeReporteDeRespuesta(usuarioReportanteId, dto.respuestaId());
            case "USUARIO" -> dto.usuarioReportadoId() != null &&
                    reporteRepository.existeReporteDeUsuario(usuarioReportanteId, dto.usuarioReportadoId());
            default -> false;
        };

        if (yaReportado) {
            throw new BadRequestException("Ya has reportado este contenido anteriormente");
        }

        Reporte reporte = Reporte.builder()
                .usuarioReportante(reportante)
                .tipoReporte(dto.tipoReporte())
                .motivo(dto.motivo())
                .descripcion(dto.descripcion().trim())
                .ipUsuario(ip)
                .estado("PENDIENTE")
                .fechaCreacion(Instant.now())
                .build();

        switch (dto.tipoReporte()) {
            case "POST" -> {
                if (dto.postId() == null) throw new BadRequestException("postId es requerido");
                Post post = postRepository.findActiveById(dto.postId())
                        .orElseThrow(() -> new RecursoNoEncontradoException("Post no encontrado"));
                reporte.setPost(post);
                reporte.setUsuarioReportado(post.getAutor());
            }
            case "COMENTARIO" -> {
                if (dto.comentarioId() == null) throw new BadRequestException("comentarioId es requerido");
                Comentario comentario = comentarioRepository.findActivoConAutor(dto.comentarioId())
                        .orElseThrow(() -> new RecursoNoEncontradoException("Comentario no encontrado"));
                reporte.setComentario(comentario);
                reporte.setUsuarioReportado(comentario.getAutor());
            }
            case "RESPUESTA" -> {
                if (dto.respuestaId() == null) throw new BadRequestException("respuestaId es requerido");
                RespuestaComentario respuesta = respuestaRepository.findActivaById(dto.respuestaId())
                        .orElseThrow(() -> new RecursoNoEncontradoException("Respuesta no encontrada"));
                reporte.setRespuesta(respuesta);
                reporte.setUsuarioReportado(respuesta.getAutor());
            }
            case "USUARIO" -> {
                if (dto.usuarioReportadoId() == null) throw new BadRequestException("usuarioReportadoId es requerido");
                Usuario reportado = usuarioRepository.findById(dto.usuarioReportadoId())
                        .orElseThrow(() -> new RecursoNoEncontradoException("Usuario reportado no encontrado"));
                reporte.setUsuarioReportado(reportado);
            }
        }

        if (reporte.getUsuarioReportado() != null &&
                reporte.getUsuarioReportado().getId().equals(usuarioReportanteId)) {
            throw new BadRequestException("No puedes reportarte a ti mismo");
        }

        reporte = reporteRepository.save(reporte);

        // Notificar a moderadores
        try {
            notificacionService.notificarNuevoReporteAModeradores(
                    reportante,
                    reporte.getUsuarioReportado(),
                    reporte.getTipoReporte(),
                    reporte.getMotivo(),
                    reporte.getId()
            );
        } catch (Exception e) {
            log.error("[NOTIF] Error al notificar a moderadores: {}", e.getMessage());
        }

        return mapearADTO(reporte);
    }

    // ============================================================
    // LISTAR REPORTES
    // ============================================================
    @Transactional(readOnly = true)
    public Page<ReporteDTO> listarReportes(String estado, String tipo, Pageable pageable) {
        return reporteRepository.findWithFilters(estado, tipo, pageable)
                .map(this::mapearADTO);
    }

    // ============================================================
    // LISTAR MIS REPORTES
    // ============================================================
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_MIS_REPORTES, key = "'usuario:' + #usuarioId")
    public List<ReporteDTO> listarMisReportes(Long usuarioId) {
        log.info("[DB] Listando reportes del usuario: {}", usuarioId);
        return reporteRepository.findByUsuarioReportanteIdOrderByFechaCreacionDesc(usuarioId)
                .stream().map(this::mapearADTO).toList();
    }

    // ============================================================
    // ACTUALIZAR ESTADO
    // ============================================================
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_RESUMEN_REPORTES, allEntries = true),
            @CacheEvict(value = CACHE_MIS_REPORTES, allEntries = true)
    })
    public ReporteDTO actualizarEstado(Long reporteId, Long moderadorId, ActualizarReporteDTO dto) {
        // Validaciones
        if (reporteId == null || moderadorId == null || dto == null) {
            throw new BadRequestException("Parámetros inválidos");
        }
        if (dto.estado() == null || !ESTADOS_VALIDOS.contains(dto.estado())) {
            throw new BadRequestException("Estado inválido");
        }
        if (dto.accionTomada() != null && !ACCIONES_VALIDAS.contains(dto.accionTomada())) {
            throw new BadRequestException("Acción inválida");
        }
        if (dto.notaModerador() != null && dto.notaModerador().length() > MAX_NOTA) {
            throw new BadRequestException("La nota es demasiado larga");
        }

        Reporte reporte = reporteRepository.findById(reporteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reporte no encontrado"));

        // Validar que el moderador sea válido
        Usuario moderador = usuarioRepository.findById(moderadorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Moderador no encontrado"));

        if (!usuarioRepository.esAdministrador(moderadorId)) {
            throw new SecurityException("No tienes permisos de moderador");
        }

        String estadoAnterior = reporte.getEstado();

        reporte.setEstado(dto.estado());
        reporte.setNotaModerador(dto.notaModerador());
        reporte.setAccionTomada(dto.accionTomada());
        reporte.setRevisadoPor(moderador);
        reporte.setFechaRevision(Instant.now());

        reporte = reporteRepository.save(reporte);

        // Notificaciones
        boolean estadoFinal = "RESUELTO".equals(dto.estado()) || "RECHAZADO".equals(dto.estado());
        boolean cambioEstado = !dto.estado().equals(estadoAnterior);

        if (estadoFinal && cambioEstado) {
            // Notificar al reportante
            try {
                notificacionService.notificarResultadoAlReportante(
                        reporte.getUsuarioReportante(),
                        moderador,
                        dto.estado(),
                        reporte.getTipoReporte(),
                        reporte.getMotivo(),
                        dto.notaModerador(),
                        dto.accionTomada()
                );
            } catch (Exception e) {
                log.error("[NOTIF] Error al notificar reportante: {}", e.getMessage());
            }

            // Notificar al usuario reportado
            try {
                if (reporte.getUsuarioReportado() != null
                        && dto.accionTomada() != null
                        && !dto.accionTomada().equals("NINGUNA")
                        && "RESUELTO".equals(dto.estado())) {

                    notificacionService.notificarUsuarioReportado(
                            reporte.getUsuarioReportado(),
                            moderador,
                            dto.accionTomada(),
                            reporte.getMotivo(),
                            dto.notaModerador()
                    );
                }
            } catch (Exception e) {
                log.error("[NOTIF] Error al notificar usuario reportado: {}", e.getMessage());
            }
        }

        return mapearADTO(reporte);
    }

    // ============================================================
    // OBTENER RESUMEN
    // ============================================================
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_RESUMEN_REPORTES, key = "'all'")
    public ReporteResumenDTO obtenerResumen() {
        return new ReporteResumenDTO(
                reporteRepository.countByEstado("PENDIENTE"),
                reporteRepository.countByEstado("REVISANDO"),
                reporteRepository.countByEstado("RESUELTO"),
                reporteRepository.countByEstado("RECHAZADO")
        );
    }

    // ============================================================
    // DTO MAPPING
    // ============================================================
    private ReporteDTO mapearADTO(Reporte r) {
        return new ReporteDTO(
                r.getId(),
                r.getTipoReporte(),
                r.getMotivo(),
                r.getDescripcion(),
                r.getEstado(),
                r.getFechaCreacion(),
                r.getUsuarioReportante() != null ? r.getUsuarioReportante().getId() : null,
                r.getUsuarioReportante() != null ? r.getUsuarioReportante().getNombreUsuario() : null,
                r.getUsuarioReportante() != null ? r.getUsuarioReportante().getFotoPerfilUrl() : null,
                r.getUsuarioReportado() != null ? r.getUsuarioReportado().getId() : null,
                r.getUsuarioReportado() != null ? r.getUsuarioReportado().getNombreUsuario() : null,
                r.getUsuarioReportado() != null ? r.getUsuarioReportado().getFotoPerfilUrl() : null,
                r.getPost() != null ? r.getPost().getId() : null,
                r.getPost() != null ? r.getPost().getTitulo() : null,
                r.getComentario() != null ? r.getComentario().getId() : null,
                r.getComentario() != null ? r.getComentario().getContenido() : null,
                r.getRespuesta() != null ? r.getRespuesta().getId() : null,
                r.getRespuesta() != null ? r.getRespuesta().getContenido() : null,
                r.getRevisadoPor() != null ? r.getRevisadoPor().getId() : null,
                r.getRevisadoPor() != null ? r.getRevisadoPor().getNombreUsuario() : null,
                r.getFechaRevision(),
                r.getNotaModerador(),
                r.getAccionTomada()
        );
    }
}