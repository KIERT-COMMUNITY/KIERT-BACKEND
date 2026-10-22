// src/main/java/com/kiert/backend/controller/AdminReporteController.java
package com.kiert.backend.features.admin.controller;

import com.kiert.backend.features.reporte.dto.ActualizarReporteDTO;
import com.kiert.backend.features.reporte.dto.ReporteDTO;
import com.kiert.backend.features.reporte.dto.ReporteResumenDTO;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.features.reporte.service.ReporteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/reportes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AdminReporteController {

    private final ReporteService reporteService;
    private final UsuarioActual usuarioActual;

    /**
     * Listar reportes con filtros
     * GET /api/admin/reportes?estado=PENDIENTE&tipo=POST
     */
    @GetMapping
    public ResponseEntity<Page<ReporteDTO>> listarReportes(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String tipo,
            @PageableDefault(size = 20, sort = "fechaCreacion", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(reporteService.listarReportes(estado, tipo, pageable));
    }

    /**
     * Resumen para el dashboard
     * GET /api/admin/reportes/resumen
     */
    @GetMapping("/resumen")
    public ResponseEntity<ReporteResumenDTO> resumen() {
        return ResponseEntity.ok(reporteService.obtenerResumen());
    }

    /**
     * Actualizar estado de un reporte
     * PUT /api/admin/reportes/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarReporte(
            @PathVariable Long id,
            @RequestBody ActualizarReporteDTO dto) {
        try {
            Long moderadorId = usuarioActual.id();
            if (moderadorId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Usuario no autenticado"));
            }
            ReporteDTO actualizado = reporteService.actualizarEstado(id, moderadorId, dto);
            return ResponseEntity.ok(actualizado);
        } catch (Exception e) {
            log.error("Error al actualizar reporte: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}