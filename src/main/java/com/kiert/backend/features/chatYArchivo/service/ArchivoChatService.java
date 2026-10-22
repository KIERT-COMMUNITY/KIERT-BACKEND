// src/main/java/com/kiert/backend/service/ArchivoChatService.java
package com.kiert.backend.features.chatYArchivo.service;

import com.kiert.backend.features.archivo.dto.ArchivoSubidoDTO;
import com.kiert.backend.exception.BadRequestException;
import com.kiert.backend.features.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArchivoChatService {

    public static final int MAX_ARCHIVOS_POR_MENSAJE = 5;
    public static final long MAX_TOTAL_POR_MENSAJE = 30L * 1024L * 1024L;

    private static final long MAX_IMAGEN = 5L * 1024L * 1024L;
    private static final long MAX_DOCUMENTO = 10L * 1024L * 1024L;
    private static final long MAX_VIDEO = 20L * 1024L * 1024L;
    private static final long MAX_AUDIO = 10L * 1024L * 1024L;
    private static final long MAX_ZIP = 10L * 1024L * 1024L;

    private static final Set<String> EXTENSIONES_BLOQUEADAS = Set.of(
            "exe", "bat", "cmd", "ps1", "msi", "apk",
            "jar", "scr", "dll", "sh", "com", "vbs"
    );

    private static final Set<String> EXTENSIONES_IMAGEN = Set.of(
            "jpg", "jpeg", "png", "webp", "gif"
    );

    private static final Set<String> EXTENSIONES_VIDEO = Set.of(
            "mp4", "webm", "mov", "m4v"
    );

    private static final Set<String> EXTENSIONES_AUDIO = Set.of(
            "mp3", "wav", "ogg", "m4a", "aac", "webm", "opus", "flac"
    );

    private static final Set<String> EXTENSIONES_DOCUMENTO = Set.of(
            "pdf", "doc", "docx", "xls", "xlsx",
            "ppt", "pptx", "txt", "csv"
    );

    private static final Set<String> EXTENSIONES_COMPRIMIDO = Set.of("zip");

    // Firmas MIME esperadas (magic bytes) — primera línea de defensa
    private static final Set<String> MIMES_PERMITIDOS = Set.of(
            // Imágenes
            "image/jpeg", "image/png", "image/webp", "image/gif",
            // Videos
            "video/mp4", "video/webm", "video/quicktime", "video/x-m4v",
            // Audio
            "audio/mpeg", "audio/wav", "audio/ogg", "audio/mp4",
            "audio/aac", "audio/webm", "audio/opus", "audio/flac",
            // Documentos
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain", "text/csv",
            // Comprimidos
            "application/zip", "application/x-zip-compressed"
    );

    private final StorageService storageService;

    public List<ArchivoSubidoDTO> validarYSubir(
            List<MultipartFile> archivos,
            String carpeta
    ) {
        List<MultipartFile> archivosValidos = normalizarLista(archivos);
        validarColeccion(archivosValidos);

        List<ArchivoSubidoDTO> subidos = new ArrayList<>();

        try {
            for (MultipartFile archivo : archivosValidos) {
                String tipoArchivo = validarArchivo(archivo);
                subidos.add(storageService.subirArchivoChat(
                        archivo,
                        carpeta,
                        tipoArchivo
                ));
            }
            return subidos;
        } catch (RuntimeException error) {
            limpiarSubidos(subidos);
            throw error;
        }
    }

    public void eliminarArchivos(List<ArchivoSubidoDTO> archivos) {
        if (archivos == null || archivos.isEmpty()) {
            return;
        }

        for (ArchivoSubidoDTO archivo : archivos) {
            if (archivo == null) {
                continue;
            }
            try {
                storageService.eliminarArchivo(archivo.publicId(), archivo.resourceType());
            } catch (RuntimeException error) {
                log.error("No se pudo eliminar el archivo {}", archivo.publicId(), error);
            }
        }
    }

    public String validarArchivo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BadRequestException("No se permiten archivos vacíos");
        }

        String nombre = obtenerNombre(archivo);
        String extension = obtenerExtension(nombre);
        String tipoMime = obtenerTipoMime(archivo);

        if (extension.isBlank()) {
            throw new BadRequestException("El archivo " + nombre + " no tiene extensión");
        }

        if (EXTENSIONES_BLOQUEADAS.contains(extension)) {
            throw new BadRequestException("El tipo de archivo ." + extension + " está bloqueado");
        }

        // Validar que el MIME declarado esté permitido
        if (!MIMES_PERMITIDOS.contains(tipoMime) && !tipoMime.startsWith("image/")
                && !tipoMime.startsWith("video/") && !tipoMime.startsWith("audio/")) {
            throw new BadRequestException(
                    "El tipo MIME '" + tipoMime + "' no está permitido"
            );
        }

        // Verificar magic bytes (primeros bytes reales del archivo)
        if (!verificarMagicBytes(archivo, tipoMime)) {
            throw new BadRequestException(
                    "El contenido del archivo no coincide con su extensión"
            );
        }

        String tipoArchivo = clasificar(extension, tipoMime);
        long limite = obtenerLimite(tipoArchivo);

        if (archivo.getSize() > limite) {
            throw new BadRequestException(
                    "El archivo " + nombre + " excede el límite de " + formatoMb(limite)
            );
        }

        return tipoArchivo;
    }

    /**
     * Verifica los "magic bytes" (firma del archivo) para evitar que un atacante
     * suba un .exe renombrado a .jpg.
     *
     * No es infalible (requiere leer los primeros 8-12 bytes), pero bloquea
     * la mayoría de ataques de "polyglot files".
     */
    private boolean verificarMagicBytes(MultipartFile archivo, String tipoMime) {
        try (InputStream is = archivo.getInputStream()) {
            byte[] header = new byte[12];
            int leidos = is.read(header);
            if (leidos < 4) return false;

            // JPEG: FF D8 FF
            if (tipoMime.equals("image/jpeg")) {
                return (header[0] & 0xFF) == 0xFF
                        && (header[1] & 0xFF) == 0xD8
                        && (header[2] & 0xFF) == 0xFF;
            }
            // PNG: 89 50 4E 47
            if (tipoMime.equals("image/png")) {
                return (header[0] & 0xFF) == 0x89
                        && header[1] == 0x50
                        && header[2] == 0x4E
                        && header[3] == 0x47;
            }
            // GIF: 47 49 46 38
            if (tipoMime.equals("image/gif")) {
                return header[0] == 0x47 && header[1] == 0x49
                        && header[2] == 0x46 && header[3] == 0x38;
            }
            // WEBP: RIFF....WEBP
            if (tipoMime.equals("image/webp")) {
                return header[0] == 0x52 && header[1] == 0x49
                        && header[2] == 0x46 && header[3] == 0x46
                        && header[8] == 0x57 && header[9] == 0x45
                        && header[10] == 0x42 && header[11] == 0x50;
            }
            // PDF: 25 50 44 46
            if (tipoMime.equals("application/pdf")) {
                return header[0] == 0x25 && header[1] == 0x50
                        && header[2] == 0x44 && header[3] == 0x46;
            }
            // MP4/MOV: ....ftyp
            if (tipoMime.startsWith("video/")) {
                return header[4] == 0x66 && header[5] == 0x74
                        && header[6] == 0x79 && header[7] == 0x70;
            }
            // ZIP (y derivados como docx/xlsx/pptx): 50 4B 03 04
            if (tipoMime.contains("zip")
                    || tipoMime.contains("word")
                    || tipoMime.contains("excel")
                    || tipoMime.contains("spreadsheet")
                    || tipoMime.contains("powerpoint")
                    || tipoMime.contains("presentation")) {
                return header[0] == 0x50 && header[1] == 0x4B
                        && header[2] == 0x03 && header[3] == 0x04;
            }
            // MP3: ID3 o FF FB
            if (tipoMime.equals("audio/mpeg")) {
                return (header[0] == 0x49 && header[1] == 0x44 && header[2] == 0x33)
                        || ((header[0] & 0xFF) == 0xFF && (header[1] & 0xE0) == 0xE0);
            }
            // WAV: RIFF....WAVE
            if (tipoMime.equals("audio/wav")) {
                return header[0] == 0x52 && header[1] == 0x49
                        && header[2] == 0x46 && header[3] == 0x46
                        && header[8] == 0x57 && header[9] == 0x41
                        && header[10] == 0x56 && header[11] == 0x45;
            }

            // Si no pudimos verificar, aceptamos (fail-open)
            return true;

        } catch (IOException e) {
            log.error("Error leyendo magic bytes: {}", e.getMessage());
            return false;
        }
    }

    private void validarColeccion(List<MultipartFile> archivos) {
        if (archivos.isEmpty()) {
            throw new BadRequestException("Debes seleccionar al menos un archivo");
        }

        if (archivos.size() > MAX_ARCHIVOS_POR_MENSAJE) {
            throw new BadRequestException("Solo se permiten 5 archivos por mensaje");
        }

        long total = archivos.stream().mapToLong(MultipartFile::getSize).sum();
        if (total > MAX_TOTAL_POR_MENSAJE) {
            throw new BadRequestException("Los archivos no pueden superar 30 MB por mensaje");
        }
    }

    private List<MultipartFile> normalizarLista(List<MultipartFile> archivos) {
        if (archivos == null) {
            return List.of();
        }
        return archivos.stream()
                .filter(archivo -> archivo != null && !archivo.isEmpty())
                .toList();
    }

    private String clasificar(String extension, String tipoMime) {
        if (EXTENSIONES_IMAGEN.contains(extension) && tipoMime.startsWith("image/")) {
            return "IMAGEN";
        }

        if (EXTENSIONES_VIDEO.contains(extension) && tipoMime.startsWith("video/")) {
            return "VIDEO";
        }

        if (EXTENSIONES_AUDIO.contains(extension) && tipoMime.startsWith("audio/")) {
            return "AUDIO";
        }

        if (EXTENSIONES_DOCUMENTO.contains(extension)) {
            return "DOCUMENTO";
        }

        if (EXTENSIONES_COMPRIMIDO.contains(extension)) {
            return "ZIP";
        }

        throw new BadRequestException(
                "El formato ." + extension + " no está permitido en el chat"
        );
    }

    private long obtenerLimite(String tipoArchivo) {
        return switch (tipoArchivo) {
            case "IMAGEN" -> MAX_IMAGEN;
            case "VIDEO" -> MAX_VIDEO;
            case "AUDIO", "NOTA_VOZ" -> MAX_AUDIO;
            case "ZIP" -> MAX_ZIP;
            default -> MAX_DOCUMENTO;
        };
    }

    private String obtenerNombre(MultipartFile archivo) {
        String nombre = archivo.getOriginalFilename();
        return nombre == null || nombre.isBlank() ? "archivo" : nombre;
    }

    private String obtenerExtension(String nombre) {
        int posicion = nombre.lastIndexOf('.');
        if (posicion < 0 || posicion == nombre.length() - 1) {
            return "";
        }
        return nombre.substring(posicion + 1).toLowerCase(Locale.ROOT);
    }

    private String obtenerTipoMime(MultipartFile archivo) {
        String tipoMime = archivo.getContentType();
        return tipoMime == null || tipoMime.isBlank()
                ? "application/octet-stream"
                : tipoMime.toLowerCase(Locale.ROOT);
    }

    private String formatoMb(long bytes) {
        return (bytes / 1024L / 1024L) + " MB";
    }

    private void limpiarSubidos(List<ArchivoSubidoDTO> subidos) {
        for (ArchivoSubidoDTO archivo : subidos) {
            try {
                storageService.eliminarArchivo(archivo.publicId(), archivo.resourceType());
            } catch (RuntimeException error) {
                log.error("No se pudo revertir el archivo {}", archivo.publicId(), error);
            }
        }
    }
}