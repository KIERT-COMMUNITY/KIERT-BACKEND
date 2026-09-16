// src/main/java/com/kiert/backend/controller/RecursoBibliotecaController.java
package com.kiert.backend.controller;

import com.kiert.backend.dto.RecursoBibliotecaDTO;
import com.kiert.backend.dto.request.RecursoUsuarioRequest;
import com.kiert.backend.exception.AccesoDenegadoException;
import com.kiert.backend.security.JwtService;
import com.kiert.backend.service.RecursoBibliotecaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/biblioteca")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class RecursoBibliotecaController {

    private final RecursoBibliotecaService bibliotecaService;
    private final JwtService jwtService;

    // ============================================================
    // 🔥 HELPER: extrae el usuarioId del token JWT
    // ============================================================
    private Long obtenerUsuarioId(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            log.debug("⚠️ No hay header Authorization");
            return null;
        }
        try {
            String token = header.substring(7);
            Long id = jwtService.extraerUsuarioId(token);
            log.debug("🔑 usuarioId extraído del token: {}", id);
            return id;
        } catch (Exception e) {
            log.warn("⚠️ Error extrayendo usuarioId: {}", e.getMessage());
            return null;
        }
    }

    // ============================================================
    // LECTURA (público, pero si hay token incluye los del usuario)
    // ============================================================

    @GetMapping
    public ResponseEntity<List<RecursoBibliotecaDTO>> listarTodos(HttpServletRequest request) {
        Long usuarioId = obtenerUsuarioId(request);
        log.info("📋 GET /api/biblioteca - usuario={}", usuarioId);
        return ResponseEntity.ok(bibliotecaService.listarTodos(usuarioId));
    }

    @GetMapping("/categoria/{categoria}")
    public ResponseEntity<List<RecursoBibliotecaDTO>> listarPorCategoria(
            HttpServletRequest request,
            @PathVariable String categoria) {
        Long usuarioId = obtenerUsuarioId(request);
        log.info("📋 GET /api/biblioteca/categoria/{} - usuario={}", categoria, usuarioId);
        return ResponseEntity.ok(bibliotecaService.listarPorCategoria(usuarioId, categoria));
    }

    @GetMapping("/destacados")
    public ResponseEntity<List<RecursoBibliotecaDTO>> listarDestacados() {
        log.info("⭐ GET /api/biblioteca/destacados");
        return ResponseEntity.ok(bibliotecaService.listarDestacados());
    }

    @GetMapping("/categorias")
    public ResponseEntity<List<String>> obtenerCategorias() {
        log.info("📋 GET /api/biblioteca/categorias");
        return ResponseEntity.ok(bibliotecaService.obtenerCategorias());
    }

    @GetMapping("/niveles")
    public ResponseEntity<List<String>> obtenerNiveles() {
        log.info("📋 GET /api/biblioteca/niveles");
        return ResponseEntity.ok(bibliotecaService.obtenerNiveles());
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<RecursoBibliotecaDTO>> buscar(
            HttpServletRequest request,
            @RequestParam(required = false) String query) {
        Long usuarioId = obtenerUsuarioId(request);
        log.info("🔍 GET /api/biblioteca/buscar?query={} - usuario={}", query, usuarioId);
        return ResponseEntity.ok(bibliotecaService.buscar(usuarioId, query));
    }

    @GetMapping("/buscar/{categoria}")
    public ResponseEntity<List<RecursoBibliotecaDTO>> buscarPorCategoria(
            @PathVariable String categoria,
            @RequestParam(required = false) String query) {
        log.info("🔍 GET /api/biblioteca/buscar/{}?query={}", categoria, query);
        return ResponseEntity.ok(bibliotecaService.buscarPorCategoria(categoria, query));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecursoBibliotecaDTO> obtenerPorId(@PathVariable Long id) {
        log.info("🔍 GET /api/biblioteca/{}", id);
        return ResponseEntity.ok(bibliotecaService.obtenerPorId(id));
    }

    // ============================================================
    // 🔥 CRUD DEL USUARIO (requiere autenticación)
    // ============================================================

    @PostMapping("/usuario")
    public ResponseEntity<RecursoBibliotecaDTO> crear(
            HttpServletRequest request,
            @Valid @RequestBody RecursoUsuarioRequest req) {

        Long usuarioId = obtenerUsuarioId(request);
        log.info("➕ POST /api/biblioteca/usuario - usuario={}", usuarioId);
        log.info("   payload: titulo='{}', categoria='{}', url='{}'",
                req.getTitulo(), req.getCategoria(), req.getUrl());

        if (usuarioId == null) {
            throw new AccesoDenegadoException("Debes iniciar sesión para agregar recursos");
        }

        RecursoBibliotecaDTO creado = bibliotecaService.crear(usuarioId, req);
        log.info("✅ Recurso creado con id={}", creado.id());
        return ResponseEntity.ok(creado);
    }

    @PutMapping("/usuario/{id}")
    public ResponseEntity<RecursoBibliotecaDTO> actualizar(
            HttpServletRequest request,
            @PathVariable Long id,
            @Valid @RequestBody RecursoUsuarioRequest req) {

        Long usuarioId = obtenerUsuarioId(request);
        log.info("✏️ PUT /api/biblioteca/usuario/{} - usuario={}", id, usuarioId);

        if (usuarioId == null) {
            throw new AccesoDenegadoException("Debes iniciar sesión");
        }

        return ResponseEntity.ok(bibliotecaService.actualizar(usuarioId, id, req));
    }

    @DeleteMapping("/usuario/{id}")
    public ResponseEntity<Void> eliminar(
            HttpServletRequest request,
            @PathVariable Long id) {

        Long usuarioId = obtenerUsuarioId(request);
        log.info("🗑️ DELETE /api/biblioteca/usuario/{} - usuario={}", id, usuarioId);

        if (usuarioId == null) {
            throw new AccesoDenegadoException("Debes iniciar sesión");
        }

        bibliotecaService.eliminar(usuarioId, id);
        return ResponseEntity.noContent().build();
    }
}