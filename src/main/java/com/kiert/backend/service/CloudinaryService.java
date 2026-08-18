package com.kiert.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.kiert.backend.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class CloudinaryService {

    private final Cloudinary cloudinary;

    // ========== CONSTANTES DE TAMAÑO ==========
    private static final long MAX_IMAGE_SIZE = 15 * 1024 * 1024; // 15MB
    private static final long MAX_VIDEO_SIZE = 50 * 1024 * 1024; // 50MB
    private static final long MAX_GIF_SIZE = 15 * 1024 * 1024; // 15MB

    // ========== SUBIR ARCHIVO (PARA CHAT) ==========
    public String subirArchivo(MultipartFile archivo) {
        try {
            log.info("📤 Subiendo archivo a Cloudinary: {}", archivo.getOriginalFilename());

            String folder = "chat";
            String resourceType = "auto";

            if (archivo.getContentType() != null && archivo.getContentType().startsWith("image/")) {
                folder = "chat/imagenes";
                resourceType = "image";
            }

            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    archivo.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", resourceType
                    )
            );

            return uploadResult.get("secure_url").toString();

        } catch (IOException e) {
            log.error("❌ Error al subir archivo a Cloudinary: {}", e.getMessage());
            throw new BadRequestException("Error al subir el archivo: " + e.getMessage());
        }
    }

    // ========== SUBIR ARCHIVO CON CARPETA PERSONALIZADA ==========
    public String subirArchivo(MultipartFile archivo, String carpeta) {
        try {
            log.info("📤 Subiendo archivo a Cloudinary en carpeta: {}", carpeta);

            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    archivo.getBytes(),
                    ObjectUtils.asMap(
                            "folder", carpeta,
                            "resource_type", "auto"
                    )
            );

            return uploadResult.get("secure_url").toString();

        } catch (IOException e) {
            log.error("❌ Error al subir archivo: {}", e.getMessage());
            throw new BadRequestException("Error al subir el archivo: " + e.getMessage());
        }
    }

    // ========== SUBIR IMAGEN (CORREGIDO) ==========
    public String subirImagen(MultipartFile imagen, String carpeta) {
        try {
            log.info("🖼️ Subiendo imagen a Cloudinary: {}", imagen.getOriginalFilename());

            if (imagen.getSize() > MAX_IMAGE_SIZE) {
                throw new BadRequestException("La imagen no puede exceder los 15MB");
            }

            String contentType = imagen.getContentType();
            if (contentType == null || !contentType.startsWith("image/") || contentType.equals("image/gif")) {
                throw new BadRequestException("El archivo debe ser una imagen (no GIF)");
            }

            // ✅ CORREGIDO: Sin transformaciones inválidas
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    imagen.getBytes(),
                    ObjectUtils.asMap(
                            "folder", carpeta != null ? carpeta : "imagenes",
                            "resource_type", "image",
                            "use_filename", true,
                            "unique_filename", true
                    )
            );

            return uploadResult.get("secure_url").toString();

        } catch (IOException e) {
            log.error("❌ Error al subir imagen: {}", e.getMessage(), e);
            throw new BadRequestException("Error al subir la imagen: " + e.getMessage());
        }
    }

    // ========== SUBIR VIDEO ==========
    public Map<String, Object> subirVideo(MultipartFile video, String carpeta) {
        try {
            log.info("🎥 Subiendo video a Cloudinary: {}", video.getOriginalFilename());

            if (video.getSize() > MAX_VIDEO_SIZE) {
                throw new BadRequestException("El video no puede exceder los 50MB");
            }

            String contentType = video.getContentType();
            if (contentType == null || !contentType.startsWith("video/")) {
                throw new BadRequestException("El archivo debe ser un video");
            }

            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    video.getBytes(),
                    ObjectUtils.asMap(
                            "folder", carpeta != null ? carpeta : "videos",
                            "resource_type", "video",
                            "chunk_size", 6000000,
                            "use_filename", true,
                            "unique_filename", true
                    )
            );

            log.info("✅ Video subido exitosamente");
            return uploadResult;

        } catch (IOException e) {
            log.error("❌ Error al subir video: {}", e.getMessage(), e);
            throw new BadRequestException("Error al subir el video: " + e.getMessage());
        }
    }

    // ========== SUBIR GIF ==========
    public String subirGif(MultipartFile gif, String carpeta) {
        try {
            log.info("🎬 Subiendo GIF a Cloudinary: {}", gif.getOriginalFilename());

            if (gif.getSize() > MAX_GIF_SIZE) {
                throw new BadRequestException("El GIF no puede exceder los 15MB");
            }

            String contentType = gif.getContentType();
            if (contentType == null || !contentType.equals("image/gif")) {
                throw new BadRequestException("El archivo debe ser un GIF");
            }

            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    gif.getBytes(),
                    ObjectUtils.asMap(
                            "folder", carpeta != null ? carpeta : "gifs",
                            "resource_type", "image",
                            "use_filename", true,
                            "unique_filename", true
                    )
            );

            return uploadResult.get("secure_url").toString();

        } catch (IOException e) {
            log.error("❌ Error al subir GIF: {}", e.getMessage(), e);
            throw new BadRequestException("Error al subir el GIF: " + e.getMessage());
        }
    }

    // ========== SUBIR MARCO ==========
    public String subirMarco(MultipartFile archivo, String nombre) {
        try {
            log.info("📤 Subiendo marco a Cloudinary: {}", nombre);

            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    archivo.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "marcos",
                            "resource_type", "image",
                            "use_filename", true,
                            "unique_filename", true
                    )
            );

            return uploadResult.get("secure_url").toString();

        } catch (IOException e) {
            log.error("❌ Error al subir marco: {}", e.getMessage());
            throw new BadRequestException("Error al subir el marco: " + e.getMessage());
        }
    }

    // ========== UTILIDADES ==========

    public boolean esVideo(MultipartFile archivo) {
        String contentType = archivo.getContentType();
        return contentType != null && contentType.startsWith("video/");
    }

    public boolean esGif(MultipartFile archivo) {
        String contentType = archivo.getContentType();
        return contentType != null && contentType.equals("image/gif");
    }

    public boolean esImagen(MultipartFile archivo) {
        String contentType = archivo.getContentType();
        return contentType != null && contentType.startsWith("image/") && !contentType.equals("image/gif");
    }

    public String getFormato(String contentType) {
        if (contentType == null) return null;
        if (contentType.startsWith("video/")) {
            return contentType.replace("video/", "").toUpperCase();
        }
        if (contentType.startsWith("image/")) {
            return contentType.replace("image/", "").toUpperCase();
        }
        return null;
    }

    // ========== GENERAR URL FIRMADA ==========
    public String generarUrlFirmadaSubida(String nombreArchivo) {
        log.info("🔑 Generando URL firmada para: {}", nombreArchivo);
        return null;
    }

    // ========== URL PÚBLICA ==========
    public String urlPublica(String nombreArchivo) {
        log.info("🌐 Generando URL pública para: {}", nombreArchivo);
        return cloudinary.url().generate(nombreArchivo);
    }

    // ========== SANITIZAR NOMBRE ==========
    public String sanitizar(String nombre) {
        if (nombre == null) return null;
        return nombre.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}