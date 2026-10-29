// src/main/java/com/kiert/backend/features/archivo/controller/ArchivoController.java
package com.kiert.backend.features.archivo.controller;

import com.kiert.backend.features.login.request.UrlFirmadaRequestDTO;
import com.kiert.backend.features.auth.dto.response.UrlFirmadaResponseDTO;
import com.kiert.backend.features.upload.service.UploadService;
import com.kiert.backend.features.storage.StorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/archivos")
@RequiredArgsConstructor
public class ArchivoController {

    private final UploadService uploadService;

    /**
     * Genera una URL firmada para subir un archivo a Cloudinary.
     * Requiere autenticacion: un invitado o un atacante no debe poder
     * consumir tu cuota de Cloudinary ni subir contenido a tu cuenta.
     */
    @PostMapping("/url-firmada")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UrlFirmadaResponseDTO> urlFirmada(@Valid @RequestBody UrlFirmadaRequestDTO datos) {
        log.info("Generando URL firmada para: {}", datos.nombreArchivo());
        return ResponseEntity.ok(uploadService.generarUrlFirmada(datos));
    }
}