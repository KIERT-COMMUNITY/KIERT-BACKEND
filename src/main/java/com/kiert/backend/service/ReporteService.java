package com.kiert.backend.service;

import com.kiert.backend.dto.*;
import com.kiert.backend.entity.*;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    // Motivos válidos
    private static final List<String> MOTIVOS_VALIDOS = List.of(
            "SPAM", "ACOSO", "CONTENIDO_INAPROPIADO", "VIOLENCIA",
            "DERECHOS_AUTOR", "INFORMACION_FALSA", "OTRO"
    );

    private static final List<String> TIPOS_VALIDOS = List.of(
            "POST", "COMENTARIO", "RESPUESTA", "USUARIO"
    );

    @Transactional
    public ReporteDTO crearReporte(Long usuarioReportanteId, CrearReporteDTO dto, String ip) {
        log.info("📢 Creando reporte tipo={} motivo={} por usuario={}",
                dto.tipoReporte(), dto.motivo(), usuarioReportanteId);

        // Validaciones
        if (!TIPOS_VALIDOS.contains(dto.tipoReporte())) {
            throw new IllegalArgumentException("Tipo de reporte inválido: " + dto.tipoReporte());
        }
        if (!MOTIVOS_VALIDOS.contains(dto.motivo())) {
            throw new IllegalArgumentException("Motivo inválido: " + dto.motivo());
        }
        if (dto.descripcion() == null || dto.descripcion().trim().length() < 10) {
            throw new IllegalArgumentException("La descripción debe tener al menos 10 caracteres");
        }

        Usuario reportante = usuarioRepository.findById(usuarioReportanteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        // Verificar reporte duplicado
        if (reporteRepository.existeReporteDuplicado(
                usuarioReportanteId, dto.tipoReporte(),
                dto.postId(), dto.comentarioId(), dto.respuestaId(), dto.usuarioReportadoId())) {
            throw new IllegalStateException("Ya has reportado este contenido anteriormente");
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

        // Cargar entidad según tipo
        switch (dto.tipoReporte()) {
            case "POST" -> {
                if (dto.postId() == null) throw new IllegalArgumentException("postId es requerido");
                Post post = postRepository.findById(dto.postId())
                        .orElseThrow(() -> new RecursoNoEncontradoException("Post no encontrado"));
                reporte.setPost(post);
                reporte.setUsuarioReportado(post.getAutor());
            }
            case "COMENTARIO" -> {
                if (dto.comentarioId() == null) throw new IllegalArgumentException("comentarioId es requerido");
                Comentario comentario = comentarioRepository.findById(dto.comentarioId())
                        .orElseThrow(() -> new RecursoNoEncontradoException("Comentario no encontrado"));
                reporte.setComentario(comentario);
                reporte.setUsuarioReportado(comentario.getAutor());
            }
            case "RESPUESTA" -> {
                if (dto.respuestaId() == null) throw new IllegalArgumentException("respuestaId es requerido");
                RespuestaComentario respuesta = respuestaRepository.findById(dto.respuestaId())
                        .orElseThrow(() -> new RecursoNoEncontradoException("Respuesta no encontrada"));
                reporte.setRespuesta(respuesta);
                reporte.setUsuarioReportado(respuesta.getAutor());
            }
            case "USUARIO" -> {
                if (dto.usuarioReportadoId() == null) throw new IllegalArgumentException("usuarioReportadoId es requerido");
                Usuario reportado = usuarioRepository.findById(dto.usuarioReportadoId())
                        .orElseThrow(() -> new RecursoNoEncontradoException("Usuario reportado no encontrado"));
                reporte.setUsuarioReportado(reportado);
            }
        }

        // No puedes reportarte a ti mismo
        if (reporte.getUsuarioReportado() != null &&
                reporte.getUsuarioReportado().getId().equals(usuarioReportanteId)) {
            throw new IllegalStateException("No puedes reportarte a ti mismo");
        }

        reporte = reporteRepository.save(reporte);
        log.info("✅ Reporte creado con ID: {}", reporte.getId());

        return mapearADTO(reporte);
    }

    @Transactional(readOnly = true)
    public Page<ReporteDTO> listarReportes(String estado, String tipo, Pageable pageable) {
        return reporteRepository.findWithFilters(estado, tipo, pageable)
                .map(this::mapearADTO);
    }

    @Transactional(readOnly = true)
    public List<ReporteDTO> listarMisReportes(Long usuarioId) {
        return reporteRepository.findByUsuarioReportanteIdOrderByFechaCreacionDesc(usuarioId)
                .stream().map(this::mapearADTO).toList();
    }

    @Transactional
    public ReporteDTO actualizarEstado(Long reporteId, Long moderadorId, ActualizarReporteDTO dto) {
        Reporte reporte = reporteRepository.findById(reporteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reporte no encontrado"));

        Usuario moderador = usuarioRepository.findById(moderadorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Moderador no encontrado"));

        reporte.setEstado(dto.estado());
        reporte.setNotaModerador(dto.notaModerador());
        reporte.setAccionTomada(dto.accionTomada());
        reporte.setRevisadoPor(moderador);
        reporte.setFechaRevision(Instant.now());

        reporte = reporteRepository.save(reporte);
        log.info("✅ Reporte {} actualizado a estado {}", reporteId, dto.estado());

        return mapearADTO(reporte);
    }

    @Transactional(readOnly = true)
    public ReporteResumenDTO obtenerResumen() {
        return new ReporteResumenDTO(
                reporteRepository.countByEstado("PENDIENTE"),
                reporteRepository.countByEstado("REVISANDO"),
                reporteRepository.countByEstado("RESUELTO"),
                reporteRepository.countByEstado("RECHAZADO")
        );
    }

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