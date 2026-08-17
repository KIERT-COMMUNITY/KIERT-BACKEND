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

    // ========== ELIMINAR ARCHIVO ==========
    public void eliminarArchivo(String publicId) {
        try {
            log.info("Eliminando archivo de Cloudinary: {}", publicId);
            Map<String, Object> result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Resultado eliminación {}: {}", publicId, result.get("result"));
        } catch (IOException e) {
            log.error("Error al eliminar archivo de Cloudinary: {}", e.getMessage());
        }
    }

    // ========== EXTRAER PUBLIC ID DE LA URL ==========
    public String publicIdDesdeUrl(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            String sinQuery = url.split("\\?")[0];
            int idx = sinQuery.indexOf("/upload/");
            if (idx == -1) return null;
            String resto = sinQuery.substring(idx + "/upload/".length());
            String[] partes = resto.split("/", 2);
            if (partes.length == 2 && partes[0].matches("v\\d+")) {
                resto = partes[1];
            }
            int extIdx = resto.lastIndexOf('.');
            if (extIdx > 0) {
                resto = resto.substring(0, extIdx);
            }
            return resto;
        } catch (Exception e) {
            log.warn("No se pudo extraer public_id de la URL: {}", url);
            return null;
        }
    }

    // ========== SANITIZAR NOMBRE ==========
    public String sanitizar(String nombre) {
        if (nombre == null) return null;
        return nombre.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}