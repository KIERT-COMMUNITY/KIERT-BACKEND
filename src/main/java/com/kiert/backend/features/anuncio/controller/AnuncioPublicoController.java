// src/main/java/com/kiert/backend/controller/AnuncioPublicoController.java
package com.kiert.backend.features.anuncio.controller;

import com.kiert.backend.entity.Anuncio;
import com.kiert.backend.features.anuncio.service.AnuncioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/anuncios")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AnuncioPublicoController {

    private final AnuncioService anuncioService;

    @GetMapping("/publicos")
    public ResponseEntity<List<Anuncio>> listarPublicos() {
        return ResponseEntity.ok(anuncioService.listarActivos());
    }

    // ✅ Endpoint para obtener el anuncio del banner flotante
    @GetMapping("/flotante")
    public ResponseEntity<Anuncio> obtenerFlotante() {
        Anuncio anuncio = anuncioService.obtenerAnuncioFlotante();
        if (anuncio == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(anuncio);
    }
}