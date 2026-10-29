// src/main/java/com/kiert/backend/features/reporte/controller/ReporteController.java
package com.kiert.backend.features.reporte.controller;

import com.kiert.backend.features.reporte.dto.ActualizarReporteDTO;
import com.kiert.backend.features.reporte.dto.CrearReporteDTO;
import com.kiert.backend.features.reporte.dto.ReporteDTO;
import com.kiert.backend.features.reporte.dto.ReporteResumenDTO;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.features.reporte.service.ReporteService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;
    private final UsuarioActual usuarioActual;

    // ============================================================
    // CREAR REPORTE
    // ============================================================
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> crearReporte(
            @RequestBody(required = false) CrearReporteDTO dto,
            HttpServletRequest request) {

        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no autenticado"));
        }
        if (dto == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Datos del reporte requeridos"));
        }

        try {
            String ip = request.getRemoteAddr();
            ReporteDTO reporte = reporteService.crearReporte(usuarioId, dto, ip);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "mensaje", "Reporte enviado correctamente. Nuestro equipo lo revisara.",
                    "reporte", reporte
            ));

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Error de validacion al crear reporte por usuario {}: {}", usuarioId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.warn("Error al crear reporte por usuario {}: {}", usuarioId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "No se pudo procesar el reporte"));
        }
    }

    // ============================================================
    // LISTAR REPORTES (solo moderadores)
    // ============================================================
    @GetMapping
    @PreAuthorize("hasAnyRole('CEO','ADMIN','MODERADOR')")
    public ResponseEntity<Page<ReporteDTO>> listarReportes(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String tipo,
            @PageableDefault(size = 20, sort = "fechaCreacion", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(reporteService.listarReportes(estado, tipo, pageable));
    }

    // ============================================================
    // MIS REPORTES
    // ============================================================
    @GetMapping("/mis-reportes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> misReportes() {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no autenticado"));
        }
        List<ReporteDTO> reportes = reporteService.listarMisReportes(usuarioId);
        return ResponseEntity.ok(reportes);
    }

    // ============================================================
    // ACTUALIZAR ESTADO (solo moderadores)
    // ============================================================
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CEO','ADMIN','MODERADOR')")
    public ResponseEntity<?> actualizarReporte(
            @PathVariable Long id,
            @RequestBody(required = false) ActualizarReporteDTO dto) {

        Long moderadorId = usuarioActual.id();
        if (moderadorId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no autenticado"));
        }
        if (dto == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Datos de actualizacion requeridos"));
        }

        try {
            ReporteDTO actualizado = reporteService.actualizarEstado(id, moderadorId, dto);
            return ResponseEntity.ok(actualizado);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "No tienes permisos de moderador"));
        } catch (Exception e) {
            log.warn("Error al actualizar reporte {} por moderador {}: {}", id, moderadorId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "No se pudo actualizar el reporte"));
        }
    }

    // ============================================================
    // RESUMEN (solo moderadores)
    // ============================================================
    @GetMapping("/resumen")
    @PreAuthorize("hasAnyRole('CEO','ADMIN','MODERADOR')")
    public ResponseEntity<ReporteResumenDTO> resumen() {
        return ResponseEntity.ok(reporteService.obtenerResumen());
    }
}