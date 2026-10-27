package com.kiert.backend.features.bienvenida.controller;

import com.kiert.backend.features.bienvenida.dto.BienvenidaDTO;
import com.kiert.backend.features.bienvenida.service.BienvenidaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bienvenida")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class BienvenidaController {

    private final BienvenidaService bienvenidaService;

    @GetMapping
    public ResponseEntity<BienvenidaDTO> obtener() {
        return ResponseEntity.ok(bienvenidaService.obtener());
    }
}