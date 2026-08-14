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
@SuppressWarnings("unchecked") // Suprime warnings de tipos no verificados
public class CloudinaryService {

    private final Cloudinary cloudinary;

    // ========== SUBIR ARCHIVO ==========
    public String subirArchivo(MultipartFile archivo) {
        try {
            log.info("Subiendo archivo a Cloudinary...");
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    archivo.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "perfiles",
                            "resource_type", "image"
                    )
            );
            String url = uploadResult.get("secure_url").toString();
            log.info("Archivo subido exitosamente: {}", url);
            return url;
        } catch (IOException e) {
            log.error("Error al subir archivo a Cloudinary: {}", e.getMessage());
            throw new BadRequestException("Error al subir la imagen a Cloudinary: " + e.getMessage());
        }
    }

    // ========== GENERAR URL FIRMADA ==========
    public String generarUrlFirmadaSubida(String nombreArchivo) {
        log.info("Generando URL firmada para: {}", nombreArchivo);
        return null;
    }

    // ========== URL PÚBLICA ==========
    public String urlPublica(String nombreArchivo) {
        log.info("Generando URL pública para: {}", nombreArchivo);
        return cloudinary.url().generate(nombreArchivo);
    }

    // ========== SANITIZAR NOMBRE ==========
    public String sanitizar(String nombre) {
        if (nombre == null) return null;
        return nombre.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}