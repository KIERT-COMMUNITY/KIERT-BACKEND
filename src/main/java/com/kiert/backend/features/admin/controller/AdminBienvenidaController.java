package com.kiert.backend.features.admin.controller;
import com.kiert.backend.features.bienvenida.dto.ActualizarBienvenidaDTO;
import com.kiert.backend.features.bienvenida.dto.BienvenidaDTO;
import com.kiert.backend.features.bienvenida.dto.HistorialBienvenidaDTO;
import com.kiert.backend.features.bienvenida.service.BienvenidaService;
import com.kiert.backend.security.UsuarioActual;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/bienvenida")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AdminBienvenidaController {

    private final BienvenidaService bienvenidaService;
    private final UsuarioActual usuarioActual;

    @GetMapping
    @PreAuthorize("hasAnyRole('CEO','ADMIN','MODERADOR')")
    public ResponseEntity<BienvenidaDTO> obtener() {
        return ResponseEntity.ok(bienvenidaService.obtener());
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('CEO','ADMIN','MODERADOR')")
    public ResponseEntity<BienvenidaDTO> actualizar(
            @Valid @RequestBody ActualizarBienvenidaDTO dto,
            HttpServletRequest request) {
        Long adminId = usuarioActual.id();
        String ip = obtenerIp(request);
        return ResponseEntity.ok(bienvenidaService.actualizar(adminId, dto.titulo(), dto.contenido(), ip));
    }

    @GetMapping("/historial")
    @PreAuthorize("hasAnyRole('CEO','ADMIN','MODERADOR')")
    public ResponseEntity<List<HistorialBienvenidaDTO>> historial(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(bienvenidaService.listarHistorial(page, size));
    }

    private String obtenerIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
