// src/main/java/com/kiert/backend/features/anuncio/controller/AnuncioController.java
package com.kiert.backend.features.anuncio.controller;

import com.kiert.backend.entity.Anuncio;
import com.kiert.backend.features.anuncio.service.AnuncioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/anuncios")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CEO','ADMIN','MODERADOR')")
public class AnuncioController {

    private final AnuncioService anuncioService;

    @GetMapping
    @PreAuthorize("hasAnyRole('CEO','ADMIN','MODERADOR')")
    public ResponseEntity<List<Anuncio>> listarTodos() {
        return ResponseEntity.ok(anuncioService.listarTodos());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CEO','ADMIN','MODERADOR')")
    public ResponseEntity<Anuncio> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(anuncioService.obtenerPorId(id));
    }

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('CEO','ADMIN','MODERADOR')")
    public ResponseEntity<Anuncio> crear(
            @RequestParam("titulo") String titulo,
            @RequestParam("texto") String texto,
            @RequestParam(value = "enlaceUrl", required = false) String enlaceUrl,
            @RequestParam(value = "esFlotante", required = false, defaultValue = "false") Boolean esFlotante,
            @RequestPart(value = "imagen", required = false) MultipartFile imagen) {
        return ResponseEntity.ok(anuncioService.crear(titulo, texto, enlaceUrl, esFlotante, imagen));
    }

    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('CEO','ADMIN','MODERADOR')")
    public ResponseEntity<Anuncio> actualizar(
            @PathVariable Long id,
            @RequestParam("titulo") String titulo,
            @RequestParam("texto") String texto,
            @RequestParam(value = "enlaceUrl", required = false) String enlaceUrl,
            @RequestParam(value = "esFlotante", required = false, defaultValue = "false") Boolean esFlotante,
            @RequestPart(value = "imagen", required = false) MultipartFile nuevaImagen) {
        return ResponseEntity.ok(anuncioService.actualizar(id, titulo, texto, enlaceUrl, esFlotante, nuevaImagen));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CEO','ADMIN','MODERADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        anuncioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}