// src/main/java/com/kiert/backend/features/upload/service/UploadService.java
package com.kiert.backend.features.upload.service;

import com.kiert.backend.features.login.request.UrlFirmadaRequestDTO;
import com.kiert.backend.features.auth.dto.response.UrlFirmadaResponseDTO;
import com.kiert.backend.exception.BadRequestException;
import com.kiert.backend.features.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadService {

    private final StorageService storageService;

    private static final int MAX_NOMBRE_ARCHIVO = 255;

    private static final Set<String> EXTENSIONES_PERMITIDAS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "svg",
            "mp4", "webm", "mov", "m4v",
            "mp3", "wav", "ogg", "m4a",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "txt", "csv", "zip"
    );

    public UrlFirmadaResponseDTO generarUrlFirmada(UrlFirmadaRequestDTO datos) {
        if (datos == null) {
            throw new BadRequestException("Datos requeridos");
        }
        if (datos.nombreArchivo() == null || datos.nombreArchivo().isBlank()) {
            throw new BadRequestException("El nombre del archivo es obligatorio");
        }

        String nombreArchivo = datos.nombreArchivo().trim();

        if (nombreArchivo.length() > MAX_NOMBRE_ARCHIVO) {
            throw new BadRequestException("El nombre del archivo es demasiado largo");
        }

        int dotIdx = nombreArchivo.lastIndexOf('.');
        if (dotIdx < 0 || dotIdx == nombreArchivo.length() - 1) {
            throw new BadRequestException("El archivo debe tener una extension");
        }

        String extension = nombreArchivo.substring(dotIdx + 1).toLowerCase(Locale.ROOT);
        if (!EXTENSIONES_PERMITIDAS.contains(extension)) {
            throw new BadRequestException("Extension de archivo no permitida");
        }

        if (nombreArchivo.contains("..")
                || nombreArchivo.contains("/")
                || nombreArchivo.contains("\\")) {
            throw new BadRequestException("El nombre del archivo contiene caracteres no permitidos");
        }

        String nombreSanitizado = storageService.sanitizar(nombreArchivo);
        if (nombreSanitizado == null || nombreSanitizado.isBlank()) {
            throw new BadRequestException("El nombre del archivo no es valido");
        }

        log.info("Generando URL firmada para: {}", nombreSanitizado);

        String urlSubida = storageService.generarUrlFirmadaSubida(nombreSanitizado);
        if (urlSubida == null || urlSubida.isBlank()) {
            log.warn("No se pudo generar la URL firmada para: {}", nombreSanitizado);
            throw new BadRequestException("No se pudo generar la URL de subida");
        }

        String urlPublica = storageService.urlPublica(nombreSanitizado);

        return new UrlFirmadaResponseDTO(urlSubida, urlPublica);
    }
}