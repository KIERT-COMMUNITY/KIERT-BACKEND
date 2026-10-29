// src/main/java/com/kiert/backend/features/recursos/controller/RecursoBibliotecaController.java
package com.kiert.backend.features.recursos.controller;

import com.kiert.backend.features.recursos.dto.RecursoBibliotecaDTO;
import com.kiert.backend.features.recursos.dto.request.RecursoUsuarioRequest;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.features.recursos.service.RecursoBibliotecaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/biblioteca")
@RequiredArgsConstructor
public class RecursoBibliotecaController {

    private final RecursoBibliotecaService bibliotecaService;
    private final UsuarioActual usuarioActual;

    // ============================================================
    // LECTURA (publica, si hay token incluye los del usuario)
    // ============================================================

    @GetMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<RecursoBibliotecaDTO>> listarTodos() {
        Long usuarioId = usuarioActual.id();
        log.debug("GET /api/biblioteca - usuario={}", usuarioId);
        return ResponseEntity.ok(bibliotecaService.listarTodos(usuarioId));
    }

    @GetMapping("/categoria/{categoria}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<RecursoBibliotecaDTO>> listarPorCategoria(@PathVariable String categoria) {
        Long usuarioId = usuarioActual.id();
        log.debug("GET /api/biblioteca/categoria/{} - usuario={}", categoria, usuarioId);
        return ResponseEntity.ok(bibliotecaService.listarPorCategoria(usuarioId, categoria));
    }

    @GetMapping("/destacados")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<RecursoBibliotecaDTO>> listarDestacados() {
        log.debug("GET /api/biblioteca/destacados");
        return ResponseEntity.ok(bibliotecaService.listarDestacados());
    }

    @GetMapping("/categorias")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<String>> obtenerCategorias() {
        log.debug("GET /api/biblioteca/categorias");
        return ResponseEntity.ok(bibliotecaService.obtenerCategorias());
    }

    @GetMapping("/niveles")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<String>> obtenerNiveles() {
        log.debug("GET /api/biblioteca/niveles");
        return ResponseEntity.ok(bibliotecaService.obtenerNiveles());
    }

    @GetMapping("/buscar")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<RecursoBibliotecaDTO>> buscar(@RequestParam(required = false) String query) {
        Long usuarioId = usuarioActual.id();
        log.debug("GET /api/biblioteca/buscar?query={} - usuario={}", query, usuarioId);
        return ResponseEntity.ok(bibliotecaService.buscar(usuarioId, query));
    }

    @GetMapping("/buscar/{categoria}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<RecursoBibliotecaDTO>> buscarPorCategoria(
            @PathVariable String categoria,
            @RequestParam(required = false) String query) {
        log.debug("GET /api/biblioteca/buscar/{}?query={}", categoria, query);
        return ResponseEntity.ok(bibliotecaService.buscarPorCategoria(categoria, query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<RecursoBibliotecaDTO> obtenerPorId(@PathVariable Long id) {
        log.debug("GET /api/biblioteca/{}", id);
        return ResponseEntity.ok(bibliotecaService.obtenerPorId(id));
    }

    // ============================================================
    // CRUD DEL USUARIO (requiere autenticacion)
    // ============================================================

    @PostMapping("/usuario")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> crear(@Valid @RequestBody RecursoUsuarioRequest req) {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Debes iniciar sesion para agregar recursos"));
        }

        log.info("POST /api/biblioteca/usuario - usuario={}", usuarioId);
        RecursoBibliotecaDTO creado = bibliotecaService.crear(usuarioId, req);
        return ResponseEntity.ok(creado);
    }

    @PutMapping("/usuario/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody RecursoUsuarioRequest req) {

        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Debes iniciar sesion"));
        }

        log.info("PUT /api/biblioteca/usuario/{} - usuario={}", id, usuarioId);
        return ResponseEntity.ok(bibliotecaService.actualizar(usuarioId, id, req));
    }

    @DeleteMapping("/usuario/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Debes iniciar sesion"));
        }

        log.info("DELETE /api/biblioteca/usuario/{} - usuario={}", id, usuarioId);
        bibliotecaService.eliminar(usuarioId, id);
        return ResponseEntity.noContent().build();
    }
}