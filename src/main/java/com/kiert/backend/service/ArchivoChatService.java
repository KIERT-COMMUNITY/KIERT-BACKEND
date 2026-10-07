package com.kiert.backend.service;

import com.kiert.backend.dto.ArchivoSubidoDTO;
import com.kiert.backend.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
            storageService.eliminarArchivo(archivo.publicId(), archivo.resourceType());
        }
    }

    public String validarArchivo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BadRequestException("No se permiten archivos vacios");
        }

        String nombre = obtenerNombre(archivo);
        String extension = obtenerExtension(nombre);
        String tipoMime = obtenerTipoMime(archivo);

        if (extension.isBlank()) {
            throw new BadRequestException("El archivo " + nombre + " no tiene extension");
        }

        if (EXTENSIONES_BLOQUEADAS.contains(extension)) {
            throw new BadRequestException("El tipo de archivo ." + extension + " esta bloqueado");
        }

        String tipoArchivo = clasificar(extension, tipoMime);
        long limite = obtenerLimite(tipoArchivo);

        if (archivo.getSize() > limite) {
            throw new BadRequestException(
                    "El archivo " + nombre + " excede el limite de " + formatoMb(limite)
            );
        }

        return tipoArchivo;
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
                "El formato ." + extension + " no esta permitido en el chat"
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
