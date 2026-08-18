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

    // ========== SUBIR ARCHIVO (PARA CHAT) ==========
    public String subirArchivo(MultipartFile archivo) {
        try {
            log.info("📤 Subiendo archivo a Cloudinary: {}", archivo.getOriginalFilename());

            // Determinar la carpeta según el tipo de archivo
            String folder = "chat";
            String resourceType = "auto";

            // Si es imagen, usar carpeta específica
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

            String url = uploadResult.get("secure_url").toString();
            log.info("✅ Archivo subido exitosamente: {}", url);
            return url;

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

            String url = uploadResult.get("secure_url").toString();
            log.info("✅ Archivo subido exitosamente: {}", url);
            return url;

        } catch (IOException e) {
            log.error("❌ Error al subir archivo: {}", e.getMessage());
            throw new BadRequestException("Error al subir el archivo: " + e.getMessage());
        }
    }

    // ========== GENERAR URL FIRMADA ==========
    public String generarUrlFirmadaSubida(String nombreArchivo) {
        log.info("🔑 Generando URL firmada para: {}", nombreArchivo);
        // Implementación para URLs firmadas si es necesario
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
    public String subirMarco(MultipartFile archivo, String nombre) {
        try {
            log.info("📤 Subiendo marco a Cloudinary: {}", nombre);

            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    archivo.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "marcos",
                            "resource_type", "image",
                            "transformation", ObjectUtils.asMap(
                                    "width", 200,
                                    "height", 200,
                                    "crop", "fill"
                            )
                    )
            );

            return uploadResult.get("secure_url").toString();
        } catch (IOException e) {
            log.error("❌ Error al subir marco: {}", e.getMessage());
            throw new BadRequestException("Error al subir el marco: " + e.getMessage());
        }
    }
}