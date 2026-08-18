package com.kiert.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final CloudinaryService cloudinaryService;

    // ========== SUBIR ARCHIVO (DETECCIÓN AUTOMÁTICA) ==========
    public String subirArchivo(MultipartFile archivo) {
        log.info("📤 StorageService - Subiendo archivo a Cloudinary");
        return cloudinaryService.subirArchivo(archivo);
    }

    // ========== SUBIR ARCHIVO CON CARPETA ==========
    public String subirArchivo(MultipartFile archivo, String carpeta) {
        log.info("📤 StorageService - Subiendo archivo a Cloudinary en carpeta: {}", carpeta);
        return cloudinaryService.subirArchivo(archivo, carpeta);
    }

    // ========== SUBIR VIDEO ==========
    public String subirVideo(MultipartFile video, String carpeta) {
        log.info("🎥 StorageService - Subiendo video a Cloudinary en carpeta: {}", carpeta);
        Map<String, Object> result = cloudinaryService.subirVideo(video, carpeta);
        return result.get("secure_url").toString();
    }

    // ========== SUBIR GIF ==========
    public String subirGif(MultipartFile gif, String carpeta) {
        log.info("🎬 StorageService - Subiendo GIF a Cloudinary en carpeta: {}", carpeta);
        return cloudinaryService.subirGif(gif, carpeta);
    }

    // ========== SUBIR IMAGEN ==========
    public String subirImagen(MultipartFile imagen, String carpeta) {
        log.info("🖼️ StorageService - Subiendo imagen a Cloudinary en carpeta: {}", carpeta);
        return cloudinaryService.subirImagen(imagen, carpeta);
    }

    // ========== SUBIR MARCO ==========
    public String subirMarco(MultipartFile archivo, String nombre) {
        log.info("🖼️ StorageService - Subiendo marco a Cloudinary: {}", nombre);
        return cloudinaryService.subirMarco(archivo, nombre);
    }

    // ========== VERIFICAR TIPOS ==========
    public boolean esVideo(MultipartFile archivo) {
        return cloudinaryService.esVideo(archivo);
    }

    public boolean esGif(MultipartFile archivo) {
        return cloudinaryService.esGif(archivo);
    }

    public boolean esImagen(MultipartFile archivo) {
        return cloudinaryService.esImagen(archivo);
    }

    public String getFormato(String contentType) {
        return cloudinaryService.getFormato(contentType);
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