package com.kiert.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final CloudinaryService cloudinaryService;

    // ========== SUBIR ARCHIVO (PARA CHAT) ==========
    public String subirArchivo(MultipartFile archivo) {
        log.info("📤 StorageService - Subiendo archivo a Cloudinary");
        return cloudinaryService.subirArchivo(archivo);
    }

    // ========== SUBIR ARCHIVO CON CARPETA ==========
    public String subirArchivo(MultipartFile archivo, String carpeta) {
        log.info("📤 StorageService - Subiendo archivo a Cloudinary en carpeta: {}", carpeta);
        return cloudinaryService.subirArchivo(archivo, carpeta);
    }

    // ========== GENERAR URL FIRMADA ==========
    public String generarUrlFirmadaSubida(String nombreArchivo) {
        log.info("🔑 StorageService - Generando URL firmada para: {}", nombreArchivo);
        return cloudinaryService.generarUrlFirmadaSubida(nombreArchivo);
    }

    // ========== URL PÚBLICA ==========
    public String urlPublica(String nombreArchivo) {
        log.info("🌐 StorageService - URL pública para: {}", nombreArchivo);
        return cloudinaryService.urlPublica(nombreArchivo);
    }

    // ========== SANITIZAR NOMBRE ==========
    public String sanitizar(String nombre) {
        return cloudinaryService.sanitizar(nombre);
    }
}