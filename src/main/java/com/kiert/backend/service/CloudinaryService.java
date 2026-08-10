package com.kiert.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    // ========== SUBIR ARCHIVO ==========
    public String subirArchivo(MultipartFile archivo) {
        try {
            String nombreOriginal = archivo.getOriginalFilename();
            log.info("📤 Subiendo archivo a Cloudinary: {}", nombreOriginal);

            String publicId = UUID.randomUUID().toString();

            Map<String, Object> subida = cloudinary.uploader().upload(
                    archivo.getBytes(),
                    ObjectUtils.asMap(
                            "public_id", publicId,
                            "folder", "kiert-files",
                            "resource_type", "auto",
                            "overwrite", true
                    )
            );

            String url = (String) subida.get("secure_url");
            log.info("✅ Archivo subido exitosamente: {}", url);
            return url;

        } catch (Exception e) {
            log.error("❌ Error al subir a Cloudinary: {}", e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error al subir archivo a Cloudinary: " + e.getMessage(), e);
        }
    }

    // ========== GENERAR URL FIRMADA (para compatibilidad) ==========
    public String generarUrlFirmadaSubida(String nombreArchivo) {
        // Cloudinary no necesita URL firmada para subir con API Key
        // Este método se mantiene por compatibilidad con Supabase
        log.info("🔑 Generando URL firmada para: {}", nombreArchivo);
        return "https://api.cloudinary.com/v1_1/auto/upload";
    }

    // ========== URL PÚBLICA (para compatibilidad) ==========
    public String urlPublica(String nombreArchivo) {
        log.info("🌐 URL pública para: {}", nombreArchivo);
        return "https://res.cloudinary.com/image/upload/kiert-files/" + nombreArchivo;
    }

    // ========== SANITIZAR NOMBRE ==========
    public String sanitizar(String nombre) {
        if (nombre == null) return "archivo";
        return nombre.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}