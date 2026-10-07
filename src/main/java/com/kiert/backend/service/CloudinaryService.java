package com.kiert.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.kiert.backend.dto.ArchivoSubidoDTO;
import com.kiert.backend.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class CloudinaryService {

    private static final long MAX_IMAGE_SIZE = 15L * 1024L * 1024L;
    private static final long MAX_VIDEO_SIZE = 50L * 1024L * 1024L;
    private static final long MAX_GIF_SIZE = 15L * 1024L * 1024L;

    private final Cloudinary cloudinary;

    public String subirArchivo(MultipartFile archivo) {
        return subirArchivo(archivo, "chat");
    }

    public String subirArchivo(MultipartFile archivo, String carpeta) {
        validarArchivoNoVacio(archivo);

        try {
            Map<String, Object> resultado = cloudinary.uploader().upload(
                    archivo.getBytes(),
                    ObjectUtils.asMap(
                            "folder", normalizarCarpeta(carpeta, "archivos"),
                            "resource_type", determinarResourceType(archivo),
                            "use_filename", true,
                            "unique_filename", true
                    )
            );

            return obtenerTexto(resultado, "secure_url");
        } catch (IOException e) {
            log.error("Error al subir archivo a Cloudinary", e);
            throw new BadRequestException("No se pudo subir el archivo");
        }
    }

    public ArchivoSubidoDTO subirArchivoChat(
            MultipartFile archivo,
            String carpeta,
            String tipoArchivo
    ) {
        validarArchivoNoVacio(archivo);

        String resourceType = determinarResourceType(archivo);
        String nombreOriginal = obtenerNombreOriginal(archivo);
        String tipoMime = obtenerTipoMime(archivo);

        try {
            Map<String, Object> resultado = cloudinary.uploader().upload(
                    archivo.getBytes(),
                    ObjectUtils.asMap(
                            "folder", normalizarCarpeta(carpeta, "chat/archivos"),
                            "resource_type", resourceType,
                            "use_filename", true,
                            "unique_filename", true,
                            "filename_override", sanitizar(nombreOriginal)
                    )
            );

            return new ArchivoSubidoDTO(
                    nombreOriginal,
                    obtenerTexto(resultado, "secure_url"),
                    obtenerTexto(resultado, "public_id"),
                    tipoMime,
                    tipoArchivo,
                    obtenerTextoOpcional(resultado, "format", obtenerExtension(nombreOriginal)),
                    obtenerTextoOpcional(resultado, "resource_type", resourceType),
                    obtenerLong(resultado, "bytes", archivo.getSize()),
                    obtenerDouble(resultado, "duration"),
                    obtenerInteger(resultado, "width"),
                    obtenerInteger(resultado, "height")
            );
        } catch (IOException e) {
            log.error("Error al subir adjunto de chat a Cloudinary", e);
            throw new BadRequestException("No se pudo subir el archivo " + nombreOriginal);
        }
    }

    public void eliminarArchivo(String publicId, String resourceType) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }

        String tipoRecurso = recursoPermitido(resourceType)
                ? resourceType.toLowerCase(Locale.ROOT)
                : "image";

        try {
            Map<String, Object> resultado = cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap(
                            "resource_type", tipoRecurso,
                            "invalidate", true
                    )
            );

            String estado = obtenerTextoOpcional(resultado, "result", "unknown");
            if (!"ok".equalsIgnoreCase(estado) && !"not found".equalsIgnoreCase(estado)) {
                log.warn("Cloudinary devolvio estado {} al eliminar {}", estado, publicId);
            }
        } catch (IOException e) {
            log.error("Error al eliminar recurso de Cloudinary: {}", publicId, e);
            throw new BadRequestException("No se pudo eliminar el archivo almacenado");
        }
    }

    public String subirImagen(MultipartFile imagen, String carpeta) {
        validarArchivoNoVacio(imagen);

        if (imagen.getSize() > MAX_IMAGE_SIZE) {
            throw new BadRequestException("La imagen no puede exceder los 15 MB");
        }

        String contentType = imagen.getContentType();
        if (contentType == null
                || !contentType.startsWith("image/")
                || "image/gif".equalsIgnoreCase(contentType)) {
            throw new BadRequestException("El archivo debe ser una imagen distinta de GIF");
        }

        try {
            Map<String, Object> resultado = cloudinary.uploader().upload(
                    imagen.getBytes(),
                    ObjectUtils.asMap(
                            "folder", normalizarCarpeta(carpeta, "imagenes"),
                            "resource_type", "image",
                            "use_filename", true,
                            "unique_filename", true
                    )
            );
            return obtenerTexto(resultado, "secure_url");
        } catch (IOException e) {
            log.error("Error al subir imagen a Cloudinary", e);
            throw new BadRequestException("No se pudo subir la imagen");
        }
    }

    public Map<String, Object> subirVideo(MultipartFile video, String carpeta) {
        validarArchivoNoVacio(video);

        if (video.getSize() > MAX_VIDEO_SIZE) {
            throw new BadRequestException("El video no puede exceder los 50 MB");
        }

        String contentType = video.getContentType();
        if (contentType == null || !contentType.startsWith("video/")) {
            throw new BadRequestException("El archivo debe ser un video");
        }

        try {
            return cloudinary.uploader().upload(
                    video.getBytes(),
                    ObjectUtils.asMap(
                            "folder", normalizarCarpeta(carpeta, "videos"),
                            "resource_type", "video",
                            "chunk_size", 6000000,
                            "use_filename", true,
                            "unique_filename", true
                    )
            );
        } catch (IOException e) {
            log.error("Error al subir video a Cloudinary", e);
            throw new BadRequestException("No se pudo subir el video");
        }
    }

    public String subirGif(MultipartFile gif, String carpeta) {
        validarArchivoNoVacio(gif);

        if (gif.getSize() > MAX_GIF_SIZE) {
            throw new BadRequestException("El GIF no puede exceder los 15 MB");
        }

        if (!"image/gif".equalsIgnoreCase(gif.getContentType())) {
            throw new BadRequestException("El archivo debe ser un GIF");
        }

        try {
            Map<String, Object> resultado = cloudinary.uploader().upload(
                    gif.getBytes(),
                    ObjectUtils.asMap(
                            "folder", normalizarCarpeta(carpeta, "gifs"),
                            "resource_type", "image",
                            "use_filename", true,
                            "unique_filename", true
                    )
            );
            return obtenerTexto(resultado, "secure_url");
        } catch (IOException e) {
            log.error("Error al subir GIF a Cloudinary", e);
            throw new BadRequestException("No se pudo subir el GIF");
        }
    }

    public String subirMarco(MultipartFile archivo, String nombre) {
        validarArchivoNoVacio(archivo);

        try {
            Map<String, Object> resultado = cloudinary.uploader().upload(
                    archivo.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "marcos",
                            "resource_type", "image",
                            "public_id", sanitizar(nombre),
                            "unique_filename", true
                    )
            );
            return obtenerTexto(resultado, "secure_url");
        } catch (IOException e) {
            log.error("Error al subir marco a Cloudinary", e);
            throw new BadRequestException("No se pudo subir el marco");
        }
    }

    public boolean esVideo(MultipartFile archivo) {
        String contentType = archivo.getContentType();
        return contentType != null && contentType.startsWith("video/");
    }

    public boolean esAudio(MultipartFile archivo) {
        String contentType = archivo.getContentType();
        return contentType != null && contentType.startsWith("audio/");
    }

    public boolean esGif(MultipartFile archivo) {
        return "image/gif".equalsIgnoreCase(archivo.getContentType());
    }

    public boolean esImagen(MultipartFile archivo) {
        String contentType = archivo.getContentType();
        return contentType != null
                && contentType.startsWith("image/")
                && !"image/gif".equalsIgnoreCase(contentType);
    }

    public String getFormato(String contentType) {
        if (contentType == null || !contentType.contains("/")) {
            return null;
        }
        return contentType.substring(contentType.indexOf('/') + 1).toUpperCase(Locale.ROOT);
    }

    public String generarUrlFirmadaSubida(String nombreArchivo) {
        log.warn("La generacion de URL firmada de subida no esta implementada para {}", nombreArchivo);
        return null;
    }

    public String urlPublica(String nombreArchivo) {
        return cloudinary.url().generate(nombreArchivo);
    }

    public String sanitizar(String nombre) {
        if (nombre == null) {
            return null;
        }
        return nombre.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private void validarArchivoNoVacio(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BadRequestException("El archivo esta vacio");
        }
    }

    private String determinarResourceType(MultipartFile archivo) {
        String contentType = obtenerTipoMime(archivo);
        if (contentType.startsWith("image/")) {
            return "image";
        }
        if (contentType.startsWith("video/") || contentType.startsWith("audio/")) {
            return "video";
        }
        return "raw";
    }

    private String obtenerNombreOriginal(MultipartFile archivo) {
        String nombre = archivo.getOriginalFilename();
        return nombre == null || nombre.isBlank() ? "archivo" : nombre;
    }

    private String obtenerTipoMime(MultipartFile archivo) {
        String contentType = archivo.getContentType();
        return contentType == null || contentType.isBlank()
                ? "application/octet-stream"
                : contentType.toLowerCase(Locale.ROOT);
    }

    private String obtenerExtension(String nombre) {
        int posicion = nombre.lastIndexOf('.');
        if (posicion < 0 || posicion == nombre.length() - 1) {
            return null;
        }
        return nombre.substring(posicion + 1).toLowerCase(Locale.ROOT);
    }

    private String normalizarCarpeta(String carpeta, String predeterminada) {
        return carpeta == null || carpeta.isBlank() ? predeterminada : carpeta;
    }

    private boolean recursoPermitido(String resourceType) {
        if (resourceType == null) {
            return false;
        }
        String valor = resourceType.toLowerCase(Locale.ROOT);
        return "image".equals(valor) || "video".equals(valor) || "raw".equals(valor);
    }

    private String obtenerTexto(Map<String, Object> resultado, String clave) {
        Object valor = resultado.get(clave);
        if (valor == null) {
            throw new BadRequestException("Cloudinary no devolvio " + clave);
        }
        return valor.toString();
    }

    private String obtenerTextoOpcional(
            Map<String, Object> resultado,
            String clave,
            String predeterminado
    ) {
        Object valor = resultado.get(clave);
        return valor == null ? predeterminado : valor.toString();
    }

    private Long obtenerLong(Map<String, Object> resultado, String clave, long predeterminado) {
        Object valor = resultado.get(clave);
        return valor instanceof Number numero ? numero.longValue() : predeterminado;
    }

    private Double obtenerDouble(Map<String, Object> resultado, String clave) {
        Object valor = resultado.get(clave);
        return valor instanceof Number numero ? numero.doubleValue() : null;
    }

    private Integer obtenerInteger(Map<String, Object> resultado, String clave) {
        Object valor = resultado.get(clave);
        return valor instanceof Number numero ? numero.intValue() : null;
    }
}
