// src/main/java/com/kiert/backend/features/cloudinary/service/CloudinaryService.java
package com.kiert.backend.features.cloudinary.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.kiert.backend.features.archivo.dto.ArchivoSubidoDTO;
import com.kiert.backend.features.imagen.dto.ImagenSubidaDTO;
import com.kiert.backend.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class CloudinaryService {

    private final Cloudinary cloudinary;

    private static final long MAX_IMAGE_SIZE = 15 * 1024 * 1024;
    private static final long MAX_VIDEO_SIZE = 50 * 1024 * 1024;
    private static final long MAX_GIF_SIZE   = 15 * 1024 * 1024;
    private static final long MAX_DOC_SIZE   = 20 * 1024 * 1024;

    private static final Set<String> RESOURCE_TYPES_VALIDOS = Set.of(
            "image", "video", "raw"
    );

    private static final Set<String> CARPETAS_PERMITIDAS = Set.of(
            "chat", "chat/imagenes", "chat/archivos", "chat/stickers",
            "anuncios",
            "posts", "posts/imagenes", "posts/videos", "posts/gifs", "posts/archivos",
            "documentos", "biblioteca",
            "imagenes", "videos", "gifs",
            "marcos", "portadas", "avatares", "perfil",
            "reportes", "temp"
    );

    // ========== SUBIR ARCHIVO (CHAT) ==========
    public String subirArchivo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BadRequestException("El archivo es obligatorio");
        }
        if (archivo.getSize() > MAX_DOC_SIZE) {
            throw new BadRequestException("El archivo no puede exceder los 20MB");
        }
        return subirArchivo(archivo, "chat");
    }

    // ========== SUBIR ARCHIVO CON CARPETA ==========
    public String subirArchivo(MultipartFile archivo, String carpeta) {
        try {
            log.info("Subiendo archivo a Cloudinary en carpeta: {}", carpeta);

            if (archivo == null || archivo.isEmpty()) {
                throw new BadRequestException("El archivo es obligatorio");
            }
            if (archivo.getSize() > MAX_DOC_SIZE) {
                throw new BadRequestException("El archivo no puede exceder los 20MB");
            }

            validarCarpeta(carpeta);

            String contentType = archivo.getContentType();
            String resourceType = "auto";

            if (contentType != null && contentType.startsWith("image/")) {
                resourceType = "image";
            }

            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    archivo.getBytes(),
                    ObjectUtils.asMap(
                            "folder", carpeta != null ? carpeta : "chat",
                            "resource_type", resourceType,
                            "use_filename", true,
                            "unique_filename", true,
                            "overwrite", false
                    )
            );

            return uploadResult.get("secure_url").toString();

        } catch (IOException e) {
            log.error("Error al subir archivo: {}", e.getMessage());
            throw new BadRequestException("Error al subir el archivo");
        }
    }

    // ========== SUBIR DOCUMENTO ==========
    public String subirDocumento(MultipartFile archivo, String carpeta) {
        try {
            log.info("Subiendo documento a Cloudinary: {} (tipo: {})",
                    archivo.getOriginalFilename(), archivo.getContentType());

            if (archivo.getSize() > MAX_DOC_SIZE) {
                throw new BadRequestException("El documento no puede exceder los 20MB");
            }

            validarCarpeta(carpeta);

            String contentType = archivo.getContentType();
            String resourceType = resolverResourceTypeDocumento(contentType);

            log.info("resource_type elegido: {} para {}", resourceType, archivo.getOriginalFilename());

            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    archivo.getBytes(),
                    ObjectUtils.asMap(
                            "folder", carpeta != null ? carpeta : "documentos",
                            "resource_type", resourceType,
                            "use_filename", true,
                            "unique_filename", true,
                            "overwrite", false
                    )
            );

            String url = uploadResult.get("secure_url").toString();
            log.info("Documento subido exitosamente: {}", url);

            return url;

        } catch (IOException e) {
            log.error("Error al subir documento: {}", e.getMessage());
            throw new BadRequestException("Error al subir el documento");
        }
    }

    private String resolverResourceTypeDocumento(String contentType) {
        if (contentType == null) return "raw";
        if (contentType.equals("application/pdf")) return "image";
        if (contentType.startsWith("image/")) return "image";
        if (contentType.startsWith("video/")) return "video";
        return "raw";
    }

    // ========== SUBIR ARCHIVO DE CHAT ==========
    public ArchivoSubidoDTO subirArchivoChat(MultipartFile archivo, String carpeta, String tipoArchivo) {
        try {
            log.info("Subiendo archivo de chat tipo {} a Cloudinary en carpeta: {}", tipoArchivo, carpeta);

            if (archivo == null || archivo.isEmpty()) {
                throw new BadRequestException("El archivo es obligatorio");
            }
            if (archivo.getSize() > MAX_DOC_SIZE) {
                throw new BadRequestException("El archivo no puede exceder los 20MB");
            }

            validarCarpeta(carpeta);

            String resourceType = resolverResourceType(tipoArchivo);

            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    archivo.getBytes(),
                    ObjectUtils.asMap(
                            "folder", carpeta != null ? carpeta : "chat",
                            "resource_type", resourceType,
                            "use_filename", true,
                            "unique_filename", true
                    )
            );

            return construirDTO(archivo, uploadResult, tipoArchivo, resourceType);

        } catch (IOException e) {
            log.error("Error al subir archivo de chat: {}", e.getMessage());
            throw new BadRequestException("Error al subir el archivo");
        }
    }

    // ========== ELIMINAR POR PUBLIC ID ==========
    public void eliminarArchivo(String publicId, String resourceType) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }

        validarPublicId(publicId);

        String tipo = (resourceType == null || resourceType.isBlank())
                ? "image"
                : resourceType.trim().toLowerCase();

        if (!RESOURCE_TYPES_VALIDOS.contains(tipo)) {
            log.warn("resourceType invalido: '{}'. Usando 'image' por defecto", tipo);
            tipo = "image";
        }

        try {
            log.info("Eliminando archivo de Cloudinary: {} (tipo {})", publicId, tipo);

            cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap("resource_type", tipo, "invalidate", true)
            );

        } catch (IOException e) {
            log.error("Error al eliminar archivo {}: {}", publicId, e.getMessage());
            throw new BadRequestException("Error al eliminar el archivo");
        }
    }

    // ========== ELIMINAR POR URL ==========
    public void eliminarArchivoPorUrl(String url) {
        if (url == null || url.isBlank()) {
            log.warn("URL vacia, no se puede eliminar");
            return;
        }

        try {
            String publicId = extraerPublicId(url);
            if (publicId == null || publicId.isBlank()) {
                log.warn("No se pudo extraer public_id de: {}", url);
                return;
            }

            validarPublicId(publicId);

            String resourceType = "image";
            if (url.contains("/raw/")) resourceType = "raw";
            else if (url.contains("/video/")) resourceType = "video";

            log.info("Eliminando archivo de Cloudinary: {} (tipo {})", publicId, resourceType);

            cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap("resource_type", resourceType, "invalidate", true)
            );

            log.info("Archivo eliminado correctamente: {}", publicId);

        } catch (BadRequestException e) {
            log.warn("PublicId rechazado al eliminar: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Error al eliminar archivo de Cloudinary: {}", e.getMessage());
        }
    }

    private String extraerPublicId(String url) {
        try {
            if (!url.contains("res.cloudinary.com")) {
                log.warn("URL no es de Cloudinary: {}", url);
                return null;
            }

            int uploadIdx = url.lastIndexOf("/upload/");
            if (uploadIdx == -1) return null;

            String resto = url.substring(uploadIdx + "/upload/".length());

            if (resto.startsWith("v")) {
                int slashIdx = resto.indexOf("/");
                if (slashIdx != -1) {
                    resto = resto.substring(slashIdx + 1);
                }
            }

            int dotIdx = resto.lastIndexOf(".");
            if (dotIdx != -1) {
                resto = resto.substring(0, dotIdx);
            }

            return resto;

        } catch (Exception e) {
            log.error("Error extrayendo public_id de URL {}: {}", url, e.getMessage());
            return null;
        }
    }

    // ========== SUBIR IMAGEN ==========
    public String subirImagen(MultipartFile imagen, String carpeta) {
        try {
            log.info("Subiendo imagen a Cloudinary: {}", imagen.getOriginalFilename());

            if (imagen.getSize() > MAX_IMAGE_SIZE) {
                throw new BadRequestException("La imagen no puede exceder los 15MB");
            }

            String contentType = imagen.getContentType();
            if (contentType == null || !contentType.startsWith("image/") || contentType.equals("image/gif")) {
                throw new BadRequestException("El archivo debe ser una imagen (no GIF)");
            }

            validarCarpeta(carpeta);

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
            log.error("Error al subir imagen: {}", e.getMessage());
            throw new BadRequestException("Error al subir la imagen");
        }
    }

    // ========== SUBIR IMAGEN CON DETALLES ==========
    public ImagenSubidaDTO subirImagenConDetalles(MultipartFile imagen, String carpeta) {
        try {
            log.info("Subiendo imagen con detalles a Cloudinary: {}", imagen.getOriginalFilename());

            if (imagen.getSize() > MAX_IMAGE_SIZE) {
                throw new BadRequestException("La imagen no puede exceder los 15MB");
            }

            String contentType = imagen.getContentType();
            if (contentType == null || !contentType.startsWith("image/") || contentType.equals("image/gif")) {
                throw new BadRequestException("El archivo debe ser una imagen (no GIF)");
            }

            validarCarpeta(carpeta);

            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    imagen.getBytes(),
                    ObjectUtils.asMap(
                            "folder", carpeta != null ? carpeta : "anuncios",
                            "resource_type", "image",
                            "use_filename", true,
                            "unique_filename", true
                    )
            );

            String url      = string(uploadResult.get("secure_url"));
            String publicId = string(uploadResult.get("public_id"));
            String formato  = string(uploadResult.get("format"));
            Integer ancho   = intValue(uploadResult.get("width"));
            Integer alto    = intValue(uploadResult.get("height"));
            Long bytes      = longValue(uploadResult.get("bytes"));
            Integer pesoKb  = bytes != null ? (int) (bytes / 1024) : null;

            log.info("Imagen subida con detalles: url={}, publicId={}, formato={}, {}x{}, {}KB",
                    url, publicId, formato, ancho, alto, pesoKb);

            return new ImagenSubidaDTO(url, publicId, formato, pesoKb, ancho, alto);

        } catch (IOException e) {
            log.error("Error al subir imagen con detalles: {}", e.getMessage());
            throw new BadRequestException("Error al subir la imagen");
        }
    }

    // ========== SUBIR VIDEO ==========
    public Map<String, Object> subirVideo(MultipartFile video, String carpeta) {
        try {
            log.info("Subiendo video a Cloudinary: {}", video.getOriginalFilename());

            if (video.getSize() > MAX_VIDEO_SIZE) {
                throw new BadRequestException("El video no puede exceder los 50MB");
            }

            String contentType = video.getContentType();
            if (contentType == null || !contentType.startsWith("video/")) {
                throw new BadRequestException("El archivo debe ser un video");
            }

            validarCarpeta(carpeta);

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

            log.info("Video subido exitosamente");
            return uploadResult;

        } catch (IOException e) {
            log.error("Error al subir video: {}", e.getMessage());
            throw new BadRequestException("Error al subir el video");
        }
    }

    // ========== SUBIR GIF ==========
    public String subirGif(MultipartFile gif, String carpeta) {
        try {
            log.info("Subiendo GIF a Cloudinary: {}", gif.getOriginalFilename());

            if (gif.getSize() > MAX_GIF_SIZE) {
                throw new BadRequestException("El GIF no puede exceder los 15MB");
            }

            String contentType = gif.getContentType();
            if (contentType == null || !contentType.equals("image/gif")) {
                throw new BadRequestException("El archivo debe ser un GIF");
            }

            validarCarpeta(carpeta);

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
            log.error("Error al subir GIF: {}", e.getMessage());
            throw new BadRequestException("Error al subir el GIF");
        }
    }

    // ========== SUBIR MARCO ==========
    public String subirMarco(MultipartFile archivo, String nombre) {
        try {
            log.info("Subiendo marco a Cloudinary: {}", nombre);

            if (archivo == null || archivo.isEmpty()) {
                throw new BadRequestException("El archivo del marco es obligatorio");
            }
            if (archivo.getSize() > MAX_IMAGE_SIZE) {
                throw new BadRequestException("El marco no puede exceder los 15MB");
            }

            String contentType = archivo.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new BadRequestException("El marco debe ser una imagen");
            }

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
            log.error("Error al subir marco: {}", e.getMessage());
            throw new BadRequestException("Error al subir el marco");
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

    // ========== GENERAR URL FIRMADA (stub original) ==========
    public String generarUrlFirmadaSubida(String nombreArchivo) {
        log.info("Generando URL firmada para: {}", nombreArchivo);
        return null;
    }

    // ========== URL PUBLICA ==========
    public String urlPublica(String nombreArchivo) {
        if (nombreArchivo == null || nombreArchivo.isBlank()) {
            throw new BadRequestException("Nombre de archivo requerido");
        }
        if (nombreArchivo.contains("..") || nombreArchivo.startsWith("/")) {
            throw new BadRequestException("Nombre de archivo no permitido");
        }
        log.info("Generando URL publica para: {}", nombreArchivo);
        return cloudinary.url().generate(nombreArchivo);
    }

    public String sanitizar(String nombre) {
        if (nombre == null) return null;
        return nombre.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    // ========== GENERAR URL DE DESCARGA FORZADA ==========
    public String generarUrlDescarga(String urlOriginal) {
        if (urlOriginal == null || urlOriginal.isBlank()) return urlOriginal;

        try {
            int uploadIdx = urlOriginal.indexOf("/upload/");
            if (uploadIdx == -1) return urlOriginal;

            String inicio = urlOriginal.substring(0, uploadIdx + "/upload/".length());
            String resto = urlOriginal.substring(uploadIdx + "/upload/".length());

            if (resto.startsWith("fl_attachment")) return urlOriginal;

            return inicio + "fl_attachment/" + resto;

        } catch (Exception e) {
            log.error("Error generando URL de descarga: {}", e.getMessage());
            return urlOriginal;
        }
    }

    // ========== HELPERS PRIVADOS ==========

    private void validarCarpeta(String carpeta) {
        if (carpeta == null || carpeta.isBlank()) {
            return;
        }

        String carpetaNormalizada = carpeta.trim().toLowerCase();

        if (carpetaNormalizada.contains("..")
                || carpetaNormalizada.contains("/etc")
                || carpetaNormalizada.contains("\\")
                || carpetaNormalizada.startsWith("/")) {
            log.warn("Carpeta rechazada por seguridad: {}", carpeta);
            throw new BadRequestException("Carpeta no permitida");
        }

        if (!CARPETAS_PERMITIDAS.contains(carpetaNormalizada)) {
            log.warn("Carpeta no esta en la whitelist: {}", carpeta);
            throw new BadRequestException("Carpeta no permitida");
        }
    }

    private void validarPublicId(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            throw new BadRequestException("publicId requerido");
        }

        String id = publicId.trim().toLowerCase();

        if (id.contains("..") || id.startsWith("/") || id.contains("\\")) {
            log.warn("publicId rechazado por path traversal: {}", publicId);
            throw new BadRequestException("publicId no permitido");
        }

        boolean prefijoValido = CARPETAS_PERMITIDAS.stream()
                .anyMatch(carpeta -> id.startsWith(carpeta + "/") || id.equals(carpeta));

        if (!prefijoValido) {
            log.warn("publicId fuera de carpetas permitidas: {}", publicId);
            throw new BadRequestException("publicId no permitido");
        }
    }

    private String resolverResourceType(String tipoArchivo) {
        if (tipoArchivo == null) return "auto";
        return switch (tipoArchivo) {
            case "IMAGEN" -> "image";
            case "VIDEO" -> "video";
            case "AUDIO", "NOTA_VOZ" -> "video";
            default -> "raw";
        };
    }

    private ArchivoSubidoDTO construirDTO(
            MultipartFile archivo,
            Map<String, Object> uploadResult,
            String tipoArchivo,
            String resourceType
    ) {
        String secureUrl   = string(uploadResult.get("secure_url"));
        String publicId    = string(uploadResult.get("public_id"));
        String formato     = string(uploadResult.get("format"));
        String resource    = string(uploadResult.get("resource_type"));
        Long   bytes       = longValue(uploadResult.get("bytes"));
        Double duracion    = doubleValue(uploadResult.get("duration"));
        Integer ancho      = intValue(uploadResult.get("width"));
        Integer alto       = intValue(uploadResult.get("height"));

        return new ArchivoSubidoDTO(
                archivo.getOriginalFilename(),
                secureUrl,
                publicId,
                archivo.getContentType(),
                tipoArchivo,
                formato,
                resource != null ? resource : resourceType,
                bytes != null ? bytes : archivo.getSize(),
                duracion,
                ancho,
                alto
        );
    }

    private String string(Object valor) { return valor == null ? null : valor.toString(); }
    private Long longValue(Object valor) { return valor instanceof Number n ? n.longValue() : null; }
    private Double doubleValue(Object valor) { return valor instanceof Number n ? n.doubleValue() : null; }
    private Integer intValue(Object valor) { return valor instanceof Number n ? n.intValue() : null; }
}