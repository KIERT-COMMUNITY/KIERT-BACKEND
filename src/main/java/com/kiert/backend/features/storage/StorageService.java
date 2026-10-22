// src/main/java/com/kiert/backend/service/StorageService.java
package com.kiert.backend.features.storage;

import com.kiert.backend.features.archivo.dto.ArchivoSubidoDTO;
import com.kiert.backend.features.cloudinary.service.CloudinaryService;
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
        return cloudinaryService.subirArchivo(archivo);
    }

    // ========== SUBIR ARCHIVO CON CARPETA ==========
    public String subirArchivo(MultipartFile archivo, String carpeta) {
        return cloudinaryService.subirArchivo(archivo, carpeta);
    }

    // ========== SUBIR ARCHIVO DE CHAT ==========
    public ArchivoSubidoDTO subirArchivoChat(
            MultipartFile archivo,
            String carpeta,
            String tipoArchivo
    ) {
        return cloudinaryService.subirArchivoChat(archivo, carpeta, tipoArchivo);
    }

    // ========== ELIMINAR ARCHIVO ==========
    public void eliminarArchivo(String publicId, String resourceType) {
        cloudinaryService.eliminarArchivo(publicId, resourceType);
    }

    // ========== SUBIR VIDEO ==========
    public String subirVideo(MultipartFile video, String carpeta) {
        Map<String, Object> result = cloudinaryService.subirVideo(video, carpeta);
        if (result == null || !result.containsKey("secure_url")) {
            throw new RuntimeException("No se pudo subir el video");
        }
        return result.get("secure_url").toString();
    }

    // ========== SUBIR GIF ==========
    public String subirGif(MultipartFile gif, String carpeta) {
        return cloudinaryService.subirGif(gif, carpeta);
    }

    // ========== SUBIR IMAGEN ==========
    public String subirImagen(MultipartFile imagen, String carpeta) {
        return cloudinaryService.subirImagen(imagen, carpeta);
    }

    // ========== SUBIR MARCO ==========
    public String subirMarco(MultipartFile archivo, String nombre) {
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
        return cloudinaryService.generarUrlFirmadaSubida(nombreArchivo);
    }

    // ========== URL PÚBLICA ==========
    public String urlPublica(String nombreArchivo) {
        return cloudinaryService.urlPublica(nombreArchivo);
    }

    // ========== SANITIZAR NOMBRE ==========
    public String sanitizar(String nombre) {
        return cloudinaryService.sanitizar(nombre);
    }
}