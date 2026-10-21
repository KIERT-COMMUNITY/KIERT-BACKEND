// src/main/java/com/kiert/backend/service/StickerService.java
package com.kiert.backend.service;

import com.kiert.backend.dto.StickerDTO;
import com.kiert.backend.entity.Sticker;
import com.kiert.backend.entity.Usuario;
import com.kiert.backend.exception.BadRequestException;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.repository.StickerRepository;
import com.kiert.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StickerService {

    private final StickerRepository stickerRepository;
    private final UsuarioRepository usuarioRepository;
    private final CloudinaryService cloudinaryService;

    private static final long MAX_SIZE = 1024 * 1024;     // 1 MB
    private static final int MAX_NOMBRE = 100;
    private static final int MAX_STICKERS_POR_USUARIO = 100;

    private static final List<String> FORMATOS_VALIDOS = List.of(
            "image/webp", "image/png", "image/gif", "image/jpeg", "image/jpg"
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

        // Validaciones
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
        if (!FORMATOS_VALIDOS.contains(contentType.toLowerCase())) {
            throw new BadRequestException(
                    "Formato no válido. Usa WEBP, PNG, GIF o JPG."
            );
        }

        // Validar límite de stickers por usuario
        long totalStickers = stickerRepository.countActivosByUsuarioId(usuarioId);
        if (totalStickers >= MAX_STICKERS_POR_USUARIO) {
            throw new BadRequestException(
                    "Has alcanzado el límite de stickers. Elimina algunos primero."
            );
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        // Subir a Cloudinary
        String url;
        try {
            url = cloudinaryService.subirArchivo(archivo, "stickers");
        } catch (Exception e) {
            log.error("Error al subir sticker a Cloudinary: {}", e.getMessage());
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

        sticker = stickerRepository.save(sticker);
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
            throw new BadRequestException("Parámetros inválidos");
        }

        Sticker sticker = stickerRepository.findActivoById(stickerId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sticker no encontrado"));

        // Validar propiedad
        if (!sticker.getUsuario().getId().equals(usuarioId)) {
            // Mensaje genérico para no revelar existencia
            throw new SecurityException("No tienes permiso para eliminar este sticker");
        }

        log.info("Eliminando sticker {} (usuario {})", stickerId, usuarioId);

        // Eliminar de Cloudinary
        if (sticker.getUrlImagen() != null) {
            try {
                cloudinaryService.eliminarArchivoPorUrl(sticker.getUrlImagen());
            } catch (Exception e) {
                log.warn("No se pudo eliminar sticker de Cloudinary: {}", e.getMessage());
                // Continuar con el soft delete en BD
            }
        }

        // Soft delete en BD
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