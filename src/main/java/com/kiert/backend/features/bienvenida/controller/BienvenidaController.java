// src/main/java/com/kiert/backend/features/bienvenida/controller/BienvenidaController.java
package com.kiert.backend.features.bienvenida.controller;

import com.kiert.backend.features.bienvenida.dto.BienvenidaDTO;
import com.kiert.backend.features.bienvenida.service.BienvenidaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bienvenida")
@RequiredArgsConstructor
public class BienvenidaController {

    private final BienvenidaService bienvenidaService;

    @GetMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<BienvenidaDTO> obtener() {
        return ResponseEntity.ok(bienvenidaService.obtener());
    }
}