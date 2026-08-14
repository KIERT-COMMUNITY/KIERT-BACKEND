package com.kiert.backend.controller;

import com.kiert.backend.dto.UrlFirmadaRequestDTO;
import com.kiert.backend.dto.UrlFirmadaResponseDTO;
import com.kiert.backend.service.UploadService;
import com.kiert.backend.service.StorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/archivos")
@RequiredArgsConstructor
public class ArchivoController {

    private final UploadService uploadService;
    private final StorageService storageService;

    @PostMapping("/url-firmada")
    public ResponseEntity<UrlFirmadaResponseDTO> urlFirmada(@Valid @RequestBody UrlFirmadaRequestDTO datos) {
        log.info("🔑 Generando URL firmada para: {}", datos.nombreArchivo());
        return ResponseEntity.ok(uploadService.generarUrlFirmada(datos));
    }

    @GetMapping("/test")
    public ResponseEntity<String> testConexion() {
        try {
            String testUrl = storageService.urlPublica("test.txt");
            log.info("✅ URL de prueba: {}", testUrl);
            return ResponseEntity.ok("✅ Conexión a Cloudinary exitosa. URL: " + testUrl);
        } catch (Exception e) {
            log.error("❌ Error: {}", e.getMessage());
            return ResponseEntity.status(500).body("❌ Error: " + e.getMessage());
        }
    }
}