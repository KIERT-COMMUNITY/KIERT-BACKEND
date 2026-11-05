// src/main/java/com/kiert/backend/features/recursos/controller/RecursoBibliotecaController.java
package com.kiert.backend.features.recursos.controller;

import com.kiert.backend.features.recursos.dto.HistorialRecursoDTO;
import com.kiert.backend.features.recursos.dto.RecursoBibliotecaDTO;
import com.kiert.backend.features.recursos.dto.request.RecursoUsuarioRequest;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.features.recursos.service.RecursoBibliotecaService;
import jakarta.servlet.http.HttpServletRequest;
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
    // LECTURA
    // ============================================================

    @GetMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<RecursoBibliotecaDTO>> listarTodos() {
        Long usuarioId = usuarioActual.id();
        return ResponseEntity.ok(bibliotecaService.listarTodos(usuarioId));
    }

    @GetMapping("/categoria/{categoria}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<RecursoBibliotecaDTO>> listarPorCategoria(@PathVariable String categoria) {
        Long usuarioId = usuarioActual.id();
        return ResponseEntity.ok(bibliotecaService.listarPorCategoria(usuarioId, categoria));
    }

    @GetMapping("/destacados")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<RecursoBibliotecaDTO>> listarDestacados() {
        return ResponseEntity.ok(bibliotecaService.listarDestacados());
    }

    @GetMapping("/categorias")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<String>> obtenerCategorias() {
        return ResponseEntity.ok(bibliotecaService.obtenerCategorias());
    }

    @GetMapping("/niveles")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<String>> obtenerNiveles() {
        return ResponseEntity.ok(bibliotecaService.obtenerNiveles());
    }

    @GetMapping("/buscar")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<RecursoBibliotecaDTO>> buscar(@RequestParam(required = false) String query) {
        Long usuarioId = usuarioActual.id();
        return ResponseEntity.ok(bibliotecaService.buscar(usuarioId, query));
    }

    @GetMapping("/buscar/{categoria}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<RecursoBibliotecaDTO>> buscarPorCategoria(
            @PathVariable String categoria,
            @RequestParam(required = false) String query) {
        return ResponseEntity.ok(bibliotecaService.buscarPorCategoria(categoria, query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<RecursoBibliotecaDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(bibliotecaService.obtenerPorId(id));
    }

    // ============================================================
    // CRUD DEL USUARIO
    // ============================================================

    @PostMapping("/usuario")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> crear(@Valid @RequestBody RecursoUsuarioRequest req, HttpServletRequest request) {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Debes iniciar sesion para agregar recursos"));
        }
        return ResponseEntity.ok(bibliotecaService.crear(usuarioId, req, request));
    }

    @PutMapping("/usuario/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody RecursoUsuarioRequest req,
            HttpServletRequest request) {

        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Debes iniciar sesion"));
        }
        return ResponseEntity.ok(bibliotecaService.actualizar(usuarioId, id, req, request));
    }

    @DeleteMapping("/usuario/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> eliminar(@PathVariable Long id, HttpServletRequest request) {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Debes iniciar sesion"));
        }
        bibliotecaService.eliminar(usuarioId, id, request);
        return ResponseEntity.noContent().build();
    }

    // ============================================================
    // HISTORIAL
    // ============================================================

    @GetMapping("/{id}/historial")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<HistorialRecursoDTO>> obtenerHistorial(@PathVariable Long id) {
        return ResponseEntity.ok(bibliotecaService.obtenerHistorial(id));
    }

    @GetMapping("/historial/mio")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HistorialRecursoDTO>> obtenerHistorialUsuario() {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(bibliotecaService.obtenerHistorialUsuario(usuarioId));
    }
}