package com.kiert.backend.controller;

import com.kiert.backend.dto.*;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.service.ReporteService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class ReporteController {

    private final ReporteService reporteService;
    private final UsuarioActual usuarioActual;

    /**
     * Crear un nuevo reporte
     * POST /api/reportes
     */
    @PostMapping
    public ResponseEntity<?> crearReporte(
            @RequestBody CrearReporteDTO dto,
            HttpServletRequest request) {
        try {
            Long usuarioId = usuarioActual.id();
            if (usuarioId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Usuario no autenticado"));
            }

            String ip = obtenerIpCliente(request);
            ReporteDTO reporte = reporteService.crearReporte(usuarioId, dto, ip);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "mensaje", "Reporte enviado correctamente. Nuestro equipo lo revisará.",
                    "reporte", reporte
            ));

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("⚠️ Error de validación al crear reporte: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Error al crear reporte: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al procesar el reporte"));
        }
    }

    /**
     * Listar todos los reportes (solo moderadores)
     * GET /api/reportes?estado=PENDIENTE&tipo=POST
     */
    @GetMapping
    public ResponseEntity<Page<ReporteDTO>> listarReportes(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String tipo,
            @PageableDefault(size = 20, sort = "fechaCreacion", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(reporteService.listarReportes(estado, tipo, pageable));
    }

    /**
     * Ver mis reportes enviados
     * GET /api/reportes/mis-reportes
     */
    @GetMapping("/mis-reportes")
    public ResponseEntity<?> misReportes() {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<ReporteDTO> reportes = reporteService.listarMisReportes(usuarioId);
        return ResponseEntity.ok(reportes);
    }

    /**
     * Actualizar estado de un reporte (moderador)
     * PUT /api/reportes/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarReporte(
            @PathVariable Long id,
            @RequestBody ActualizarReporteDTO dto) {
        try {
            Long moderadorId = usuarioActual.id();
            if (moderadorId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            ReporteDTO actualizado = reporteService.actualizarEstado(id, moderadorId, dto);
            return ResponseEntity.ok(actualizado);
        } catch (Exception e) {
            log.error("❌ Error al actualizar reporte: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Resumen de reportes para dashboard
     * GET /api/reportes/resumen
     */
    @GetMapping("/resumen")
    public ResponseEntity<ReporteResumenDTO> resumen() {
        return ResponseEntity.ok(reporteService.obtenerResumen());
    }

    private String obtenerIpCliente(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}