// src/main/java/com/kiert/backend/features/sticker/service/StickerService.java
package com.kiert.backend.features.sticker.service;

import com.kiert.backend.features.sticker.dto.StickerDTO;
import com.kiert.backend.entity.Sticker;
import com.kiert.backend.entity.Usuario;
import com.kiert.backend.exception.BadRequestException;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.features.sticker.repository.StickerRepository;
import com.kiert.backend.features.usuario.repository.UsuarioRepository;
import com.kiert.backend.features.cloudinary.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StickerService {

    private final StickerRepository stickerRepository;
    private final UsuarioRepository usuarioRepository;
    private final CloudinaryService cloudinaryService;

    private static final long MAX_SIZE = 1024 * 1024;
    private static final int MAX_NOMBRE = 100;
    private static final int MAX_STICKERS_POR_USUARIO = 100;

    private static final Set<String> FORMATOS_VALIDOS = Set.of(
            "image/webp", "image/png", "image/gif", "image/jpeg", "image/jpg"
    );

    private static final Set<String> EXTENSIONES_VALIDAS = Set.of(
            "webp", "png", "gif", "jpg", "jpeg"
    );

    // ============================================================
    // LISTAR
    // ============================================================
    @Transactional(readOnly = true)
    public List<StickerDTO> listarMisStickers(Long usuarioId) {
        if (usuarioId == null) {
            throw new BadRequestException("Usuario requerido");
        }
        return stickerRepository.findByUsuarioIdOrderByFechaDesc(usuarioId)
                .stream()
                .map(this::mapearADTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StickerDTO> listarStickersPublicos() {
        return stickerRepository.findPublicos()
                .stream()
                .map(this::mapearADTO)
                .collect(Collectors.toList());
    }

    // ============================================================
    // CREAR STICKER
    // ============================================================
    @Transactional
    public StickerDTO crearSticker(Long usuarioId, String nombre,
                                   Boolean esPublico, MultipartFile archivo) {
        log.info("Creando sticker para usuario {}", usuarioId);

        if (usuarioId == null) {
            throw new BadRequestException("Usuario requerido");
        }
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new BadRequestException("El nombre del sticker es obligatorio");
        }
        if (nombre.trim().length() > MAX_NOMBRE) {
            throw new BadRequestException("El nombre es demasiado largo");
        }
        if (archivo == null || archivo.isEmpty()) {
            throw new BadRequestException("El archivo del sticker es obligatorio");
        }
        if (archivo.getSize() > MAX_SIZE) {
            throw new BadRequestException(
                    "El sticker no debe superar " + (MAX_SIZE / 1024) + " KB"
            );
        }

        String contentType = archivo.getContentType();
        if (contentType == null) {
            throw new BadRequestException("No se pudo determinar el tipo de archivo");
        }
        if (!FORMATOS_VALIDOS.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException(
                    "Formato no valido. Usa WEBP, PNG, GIF o JPG."
            );
        }

        // Validacion adicional por extension del archivo
        String nombreOriginal = archivo.getOriginalFilename();
        if (nombreOriginal != null) {
            int dotIdx = nombreOriginal.lastIndexOf('.');
            if (dotIdx != -1) {
                String ext = nombreOriginal.substring(dotIdx + 1).toLowerCase(Locale.ROOT);
                if (!EXTENSIONES_VALIDAS.contains(ext)) {
                    throw new BadRequestException("Extension de archivo no permitida");
                }
            }
        }

        long totalStickers = stickerRepository.countActivosByUsuarioId(usuarioId);
        if (totalStickers >= MAX_STICKERS_POR_USUARIO) {
            throw new BadRequestException(
                    "Has alcanzado el limite de stickers. Elimina algunos primero."
            );
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        String url;
        try {
            url = cloudinaryService.subirArchivo(archivo, "stickers");
        } catch (Exception e) {
            log.warn("Error al subir sticker a Cloudinary: {}", e.getMessage());
            throw new BadRequestException("No se pudo subir el sticker");
        }

        Sticker sticker = Sticker.builder()
                .usuario(usuario)
                .nombre(nombre.trim())
                .urlImagen(url)
                .urlMiniatura(url)
                .tipoArchivo(contentType)
                .pesoKb((int) (archivo.getSize() / 1024))
                .esPublico(esPublico != null && esPublico)
                .activo(true)
                .fechaCreacion(Instant.now())
                .build();

        try {
            sticker = stickerRepository.save(sticker);
        } catch (Exception e) {
            // Si falla el guardado, eliminar el archivo de Cloudinary para no dejar huerfanos
            try {
                cloudinaryService.eliminarArchivoPorUrl(url);
            } catch (Exception cleanupEx) {
                log.warn("No se pudo eliminar sticker huerfano de Cloudinary: {}", cleanupEx.getMessage());
            }
            log.warn("Error al guardar sticker en BD: {}", e.getMessage());
            throw new BadRequestException("No se pudo guardar el sticker");
        }

        log.info("Sticker creado id={} esPublico={}",
                sticker.getId(), sticker.getEsPublico());

        return mapearADTO(sticker);
    }

    // ============================================================
    // ELIMINAR STICKER
    // ============================================================
    @Transactional
    public void eliminarSticker(Long stickerId, Long usuarioId) {
        if (stickerId == null || usuarioId == null) {
            throw new BadRequestException("Parametros invalidos");
        }

        Sticker sticker = stickerRepository.findActivoById(stickerId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sticker no encontrado"));

        if (!sticker.getUsuario().getId().equals(usuarioId)) {
            throw new SecurityException("No tienes permiso para eliminar este sticker");
        }

        log.info("Eliminando sticker {} (usuario {})", stickerId, usuarioId);

        if (sticker.getUrlImagen() != null) {
            try {
                cloudinaryService.eliminarArchivoPorUrl(sticker.getUrlImagen());
            } catch (Exception e) {
                log.warn("No se pudo eliminar sticker de Cloudinary: {}", e.getMessage());
            }
        }

        sticker.setActivo(false);
        stickerRepository.save(sticker);

        log.info("Sticker {} eliminado correctamente", stickerId);
    }

    // ============================================================
    // HELPERS
    // ============================================================
    private StickerDTO mapearADTO(Sticker s) {
        return new StickerDTO(
                s.getId(),
                s.getUsuario().getId(),
                s.getNombre(),
                s.getUrlImagen(),
                s.getUrlMiniatura(),
                s.getTipoArchivo(),
                s.getPesoKb(),
                s.getAncho(),
                s.getAlto(),
                s.getEsPublico(),
                s.getFechaCreacion()
        );
    }
}