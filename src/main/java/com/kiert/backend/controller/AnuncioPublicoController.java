package com.kiert.backend.controller;

import com.kiert.backend.entity.Anuncio;
import com.kiert.backend.service.AnuncioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/anuncios")
@RequiredArgsConstructor
public class AnuncioPublicoController {

    private final AnuncioService anuncioService;

    @GetMapping("/publicos")
    public ResponseEntity<List<Anuncio>> listarPublicos() {
        return ResponseEntity.ok(anuncioService.listarActivos());
    }
}