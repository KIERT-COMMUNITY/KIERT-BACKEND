// src/main/java/com/kiert/backend/controller/RecursoBibliotecaController.java
package com.kiert.backend.controller;

import com.kiert.backend.dto.RecursoBibliotecaDTO;
import com.kiert.backend.service.RecursoBibliotecaService;
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

    @GetMapping
    public ResponseEntity<List<RecursoBibliotecaDTO>> listarTodos() {
        log.info("📋 GET /api/biblioteca - Listando todos los recursos");
        return ResponseEntity.ok(bibliotecaService.listarTodos());
    }

    @GetMapping("/categoria/{categoria}")
    public ResponseEntity<List<RecursoBibliotecaDTO>> listarPorCategoria(@PathVariable String categoria) {
        log.info("📋 GET /api/biblioteca/categoria/{} - Listando por categoría", categoria);
        return ResponseEntity.ok(bibliotecaService.listarPorCategoria(categoria));
    }

    @GetMapping("/destacados")
    public ResponseEntity<List<RecursoBibliotecaDTO>> listarDestacados() {
        log.info("⭐ GET /api/biblioteca/destacados - Listando destacados");
        return ResponseEntity.ok(bibliotecaService.listarDestacados());
    }

    @GetMapping("/categorias")
    public ResponseEntity<List<String>> obtenerCategorias() {
        log.info("📋 GET /api/biblioteca/categorias - Obteniendo categorías");
        return ResponseEntity.ok(bibliotecaService.obtenerCategorias());
    }

    @GetMapping("/niveles")
    public ResponseEntity<List<String>> obtenerNiveles() {
        log.info("📋 GET /api/biblioteca/niveles - Obteniendo niveles");
        return ResponseEntity.ok(bibliotecaService.obtenerNiveles());
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<RecursoBibliotecaDTO>> buscar(@RequestParam(required = false) String query) {
        log.info("🔍 GET /api/biblioteca/buscar?query={} - Buscando recursos", query);
        return ResponseEntity.ok(bibliotecaService.buscar(query));
    }

    @GetMapping("/buscar/{categoria}")
    public ResponseEntity<List<RecursoBibliotecaDTO>> buscarPorCategoria(
            @PathVariable String categoria,
            @RequestParam(required = false) String query) {
        log.info("🔍 GET /api/biblioteca/buscar/{}?query={} - Buscando en categoría", categoria, query);
        return ResponseEntity.ok(bibliotecaService.buscarPorCategoria(categoria, query));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecursoBibliotecaDTO> obtenerPorId(@PathVariable Long id) {
        log.info("🔍 GET /api/biblioteca/{} - Obteniendo recurso", id);
        return ResponseEntity.ok(bibliotecaService.obtenerPorId(id));
    }
}