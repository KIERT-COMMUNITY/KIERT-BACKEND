// src/main/java/com/kiert/backend/features/storage/StorageService.java
package com.kiert.backend.features.storage;

import com.kiert.backend.features.archivo.dto.ArchivoSubidoDTO;
import com.kiert.backend.features.cloudinary.service.CloudinaryService;
import com.kiert.backend.exception.BadRequestException;
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

    // ========== SUBIR ARCHIVO (DETECCION AUTOMATICA) ==========
    public String subirArchivo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BadRequestException("El archivo es obligatorio");
        }
        return cloudinaryService.subirArchivo(archivo);
    }

    // ========== SUBIR ARCHIVO CON CARPETA ==========
    public String subirArchivo(MultipartFile archivo, String carpeta) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BadRequestException("El archivo es obligatorio");
        }
        return cloudinaryService.subirArchivo(archivo, carpeta);
    }

    // ========== SUBIR ARCHIVO DE CHAT ==========
    public ArchivoSubidoDTO subirArchivoChat(
            MultipartFile archivo,
            String carpeta,
            String tipoArchivo
    ) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BadRequestException("El archivo es obligatorio");
        }
        return cloudinaryService.subirArchivoChat(archivo, carpeta, tipoArchivo);
    }

    // ========== ELIMINAR ARCHIVO ==========
    public void eliminarArchivo(String publicId, String resourceType) {
        cloudinaryService.eliminarArchivo(publicId, resourceType);
    }

    // ========== SUBIR VIDEO ==========
    public String subirVideo(MultipartFile video, String carpeta) {
        if (video == null || video.isEmpty()) {
            throw new BadRequestException("El video es obligatorio");
        }
        Map<String, Object> result = cloudinaryService.subirVideo(video, carpeta);
        if (result == null || !result.containsKey("secure_url")) {
            log.warn("Cloudinary no devolvio secure_url al subir video");
            throw new BadRequestException("No se pudo subir el video");
        }
        return result.get("secure_url").toString();
    }

    // ========== SUBIR GIF ==========
    public String subirGif(MultipartFile gif, String carpeta) {
        if (gif == null || gif.isEmpty()) {
            throw new BadRequestException("El GIF es obligatorio");
        }
        return cloudinaryService.subirGif(gif, carpeta);
    }

    // ========== SUBIR IMAGEN ==========
    public String subirImagen(MultipartFile imagen, String carpeta) {
        if (imagen == null || imagen.isEmpty()) {
            throw new BadRequestException("La imagen es obligatoria");
        }
        return cloudinaryService.subirImagen(imagen, carpeta);
    }

    // ========== SUBIR MARCO ==========
    public String subirMarco(MultipartFile archivo, String nombre) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BadRequestException("El archivo del marco es obligatorio");
        }
        return cloudinaryService.subirMarco(archivo, nombre);
    }

    // ========== VERIFICAR TIPOS ==========
    public boolean esVideo(MultipartFile archivo) {
        return archivo != null && cloudinaryService.esVideo(archivo);
    }

    public boolean esGif(MultipartFile archivo) {
        return archivo != null && cloudinaryService.esGif(archivo);
    }

    public boolean esImagen(MultipartFile archivo) {
        return archivo != null && cloudinaryService.esImagen(archivo);
    }

    public String getFormato(String contentType) {
        return cloudinaryService.getFormato(contentType);
    }

    // ========== GENERAR URL FIRMADA ==========
    public String generarUrlFirmadaSubida(String nombreArchivo) {
        return cloudinaryService.generarUrlFirmadaSubida(nombreArchivo);
    }

    // ========== URL PUBLICA ==========
    public String urlPublica(String nombreArchivo) {
        return cloudinaryService.urlPublica(nombreArchivo);
    }

    // ========== SANITIZAR NOMBRE ==========
    public String sanitizar(String nombre) {
        return cloudinaryService.sanitizar(nombre);
    }
}