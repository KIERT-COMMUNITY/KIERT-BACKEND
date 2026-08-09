package com.kiert.backend.controller;

import com.kiert.backend.dto.UrlFirmadaRequestDTO;
import com.kiert.backend.dto.UrlFirmadaResponseDTO;
import com.kiert.backend.service.UploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Espejo exacto de upload.service.ts -> pedirUrlFirmada()
// (POST /api/archivos/url-firmada)
@RestController
@RequestMapping("/api/archivos")
@RequiredArgsConstructor
public class ArchivoController {

    private final UploadService uploadService;

    @PostMapping("/url-firmada")
    public ResponseEntity<UrlFirmadaResponseDTO> urlFirmada(@Valid @RequestBody UrlFirmadaRequestDTO datos) {
        return ResponseEntity.ok(uploadService.generarUrlFirmada(datos));
    }
}
