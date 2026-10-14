// src/main/java/com/kiert/backend/service/StickerService.java
package com.kiert.backend.service;

import com.kiert.backend.dto.StickerDTO;
import com.kiert.backend.entity.Sticker;
import com.kiert.backend.entity.Usuario;
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
    private static final List<String> FORMATOS_VALIDOS = List.of(
            "image/webp", "image/png", "image/gif", "image/jpeg", "image/jpg"
    );

    @Transactional(readOnly = true)
    public List<StickerDTO> listarMisStickers(Long usuarioId) {
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

    @Transactional
    public StickerDTO crearSticker(Long usuarioId, String nombre,
                                   Boolean esPublico, MultipartFile archivo) {
        log.info("Creando sticker para usuario {}: {} (público: {})",
                usuarioId, nombre, esPublico);

        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del sticker es obligatorio");
        }

        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo del sticker es obligatorio");
        }

        if (archivo.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException(
                    "El sticker no debe superar " + (MAX_SIZE / 1024) + " KB"
            );
        }

        String contentType = archivo.getContentType();
        if (contentType == null) {
            throw new IllegalArgumentException("No se pudo determinar el tipo de archivo");
        }
        if (!FORMATOS_VALIDOS.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Formato no válido. Usa WEBP, PNG, GIF o JPG."
            );
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        String url;
        try {
            url = cloudinaryService.subirArchivo(archivo, "stickers");
        } catch (Exception e) {
            log.error("Error al subir sticker a Cloudinary: {}", e.getMessage(), e);
            throw new RuntimeException("Error al subir el sticker: " + e.getMessage());
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
        log.info("✅ Sticker creado id={} esPublico={} url={}",
                sticker.getId(), sticker.getEsPublico(), sticker.getUrlImagen());

        return mapearADTO(sticker);
    }

    // ============================================================
    // ✅ ELIMINAR STICKER (borra de Cloudinary + soft delete en BD)
    // ============================================================
    @Transactional
    public void eliminarSticker(Long stickerId, Long usuarioId) {
        Sticker sticker = stickerRepository.findById(stickerId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sticker no encontrado"));

        // Verificar que el sticker pertenece al usuario
        if (!sticker.getUsuario().getId().equals(usuarioId)) {
            throw new SecurityException("No puedes eliminar este sticker");
        }

        // Si ya está inactivo, no hacer nada
        if (!sticker.getActivo()) {
            log.warn("Sticker {} ya estaba eliminado", stickerId);
            return;
        }

        log.info("Eliminando sticker {} (usuario {})", stickerId, usuarioId);

        // ✅ Eliminar de Cloudinary
        if (sticker.getUrlImagen() != null) {
            cloudinaryService.eliminarArchivoPorUrl(sticker.getUrlImagen());
        }

        // ✅ Soft delete en BD (mantener histórico)
        sticker.setActivo(false);
        stickerRepository.save(sticker);

        log.info("✅ Sticker {} eliminado correctamente", stickerId);
    }

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