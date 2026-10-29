// src/main/java/com/kiert/backend/features/admin/controller/AdminReporteController.java
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/reportes")
@RequiredArgsConstructor
public class AdminReporteController {

    private final ReporteService reporteService;
    private final UsuarioActual usuarioActual;

    @GetMapping
    @PreAuthorize("hasAnyRole('CEO','ADMIN','MODERADOR')")
    public ResponseEntity<Page<ReporteDTO>> listarReportes(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String tipo,
            @PageableDefault(size = 20, sort = "fechaCreacion", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(reporteService.listarReportes(estado, tipo, pageable));
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasAnyRole('CEO','ADMIN','MODERADOR')")
    public ResponseEntity<ReporteResumenDTO> resumen() {
        return ResponseEntity.ok(reporteService.obtenerResumen());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CEO','ADMIN','MODERADOR')")
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
            log.error("Error al actualizar reporte {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "No se pudo actualizar el reporte"));
        }
    }
}