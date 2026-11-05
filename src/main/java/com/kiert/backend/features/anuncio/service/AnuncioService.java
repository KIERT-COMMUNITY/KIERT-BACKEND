// src/main/java/com/kiert/backend/features/anuncio/service/AnuncioService.java
package com.kiert.backend.features.anuncio.service;

import com.kiert.backend.features.imagen.dto.ImagenSubidaDTO;
import com.kiert.backend.entity.Anuncio;
import com.kiert.backend.exception.BadRequestException;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.features.anuncio.repository.AnuncioRepository;
import com.kiert.backend.features.cloudinary.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnuncioService {

    private static final int TITULO_MAX_LENGTH = 200;
    private static final int TEXTO_MAX_LENGTH = 5000;

    private final AnuncioRepository anuncioRepository;
    private final CloudinaryService cloudinaryService;

    public List<Anuncio> listarActivos() {
        return anuncioRepository.findActivosVigentes(LocalDateTime.now());
    }

    public List<Anuncio> listarTodos() {
        return anuncioRepository.findAllByOrderByFechaCreacionDesc();
    }

    public Anuncio obtenerPorId(Long id) {
        return anuncioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Anuncio no encontrado"));
    }

    public Anuncio obtenerAnuncioFlotante() {
        return anuncioRepository.findFirstByEsFlotanteTrueAndActivoTrueOrderByFechaCreacionDesc()
                .orElse(null);
    }

    @Transactional
    public Anuncio crear(String titulo, String texto, String enlaceUrl, Boolean esFlotante, MultipartFile imagen) {
        log.info("Creando anuncio: titulo='{}', esFlotante={}, tieneImagen={}",
                titulo, esFlotante, imagen != null && !imagen.isEmpty());

        validarTituloYTexto(titulo, texto);

        Anuncio anuncio = Anuncio.builder()
                .titulo(titulo.trim())
                .texto(texto.trim())
                .enlaceUrl(enlaceUrl)
                .esFlotante(esFlotante != null ? esFlotante : false)
                .activo(true)
                .destacado(false)
                .fechaCreacion(LocalDateTime.now())
                .fechaPublicacion(LocalDateTime.now())
                .build();

        if (imagen != null && !imagen.isEmpty()) {
            try {
                ImagenSubidaDTO meta = cloudinaryService.subirImagenConDetalles(imagen, "anuncios");
                aplicarMetaImagen(anuncio, meta);
            } catch (Exception e) {
                log.error("Error al subir la imagen: {}", e.getMessage());
                throw new BadRequestException("No se pudo subir la imagen");
            }
        }

        try {
            return anuncioRepository.save(anuncio);
        } catch (Exception e) {
            log.error("Error al guardar el anuncio: {}", e.getMessage());
            // Limpieza defensiva: si se subio imagen pero fallo el guardado en BD,
            // intentar borrar la imagen de Cloudinary sin bloquear la excepcion original
            if (anuncio.getImagenPublicId() != null) {
                try {
                    cloudinaryService.eliminarArchivo(anuncio.getImagenPublicId(), "image");
                    log.info("Imagen huerfana eliminada de Cloudinary: {}", anuncio.getImagenPublicId());
                } catch (Exception cleanupError) {
                    log.warn("No se pudo eliminar la imagen huerfana {}: {}",
                            anuncio.getImagenPublicId(), cleanupError.getMessage());
                }
            }
            throw new BadRequestException("No se pudo guardar el anuncio");
        }
    }

    @Transactional
    public Anuncio actualizar(Long id, String titulo, String texto, String enlaceUrl, Boolean esFlotante, MultipartFile nuevaImagen) {
        log.info("Actualizando anuncio id={}, esFlotante={}, tieneNuevaImagen={}",
                id, esFlotante, nuevaImagen != null && !nuevaImagen.isEmpty());

        Anuncio anuncio = obtenerPorId(id);
        validarTituloYTexto(titulo, texto);

        anuncio.setTitulo(titulo.trim());
        anuncio.setTexto(texto.trim());
        anuncio.setEnlaceUrl(enlaceUrl);
        anuncio.setEsFlotante(esFlotante != null ? esFlotante : false);
        anuncio.setFechaActualizacion(LocalDateTime.now());

        if (nuevaImagen != null && !nuevaImagen.isEmpty()) {
            String publicIdAnterior = anuncio.getImagenPublicId();
            try {
                ImagenSubidaDTO meta = cloudinaryService.subirImagenConDetalles(nuevaImagen, "anuncios");
                aplicarMetaImagen(anuncio, meta);
            } catch (Exception e) {
                log.error("Error al subir la nueva imagen: {}", e.getMessage());
                throw new BadRequestException("No se pudo subir la nueva imagen");
            }

            // Si se subio la nueva imagen correctamente, intentar eliminar la anterior
            if (publicIdAnterior != null) {
                try {
                    cloudinaryService.eliminarArchivo(publicIdAnterior, "image");
                    log.info("Imagen anterior eliminada de Cloudinary: {}", publicIdAnterior);
                } catch (Exception cleanupError) {
                    // No relanzar: la actualizacion ya fue exitosa, la imagen vieja queda huerfana
                    log.warn("No se pudo eliminar la imagen anterior {}: {}",
                            publicIdAnterior, cleanupError.getMessage());
                }
            }
        }
        return anuncioRepository.save(anuncio);
    }

    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando anuncio id={}", id);

        Anuncio anuncio = obtenerPorId(id);

        // Intentar eliminar la imagen de Cloudinary ANTES de borrar el registro
        // Si falla, no bloqueamos el borrado del anuncio
        if (anuncio.getImagenPublicId() != null && !anuncio.getImagenPublicId().isBlank()) {
            try {
                cloudinaryService.eliminarArchivo(anuncio.getImagenPublicId(), "image");
                log.info("Imagen del anuncio eliminada de Cloudinary: {}", anuncio.getImagenPublicId());
            } catch (Exception e) {
                log.warn("No se pudo eliminar la imagen {} de Cloudinary: {}",
                        anuncio.getImagenPublicId(), e.getMessage());
                // Continuamos: el anuncio debe borrarse aunque la imagen quede huerfana
            }
        }

        anuncioRepository.delete(anuncio);
        log.info("Anuncio {} eliminado de la base de datos", id);
    }

    private void validarTituloYTexto(String titulo, String texto) {
        if (titulo == null || titulo.isBlank()) {
            throw new BadRequestException("El titulo es obligatorio");
        }
        if (texto == null || texto.isBlank()) {
            throw new BadRequestException("El texto es obligatorio");
        }
        if (titulo.length() > TITULO_MAX_LENGTH) {
            throw new BadRequestException("El titulo no debe exceder " + TITULO_MAX_LENGTH + " caracteres");
        }
        if (texto.length() > TEXTO_MAX_LENGTH) {
            throw new BadRequestException("El texto no debe exceder " + TEXTO_MAX_LENGTH + " caracteres");
        }
    }

    private void aplicarMetaImagen(Anuncio anuncio, ImagenSubidaDTO meta) {
        anuncio.setImagenUrl(meta.url());
        anuncio.setImagenPublicId(meta.publicId());
        anuncio.setImagenFormato(meta.formato());
        anuncio.setImagenPesoKb(meta.pesoKb());
        anuncio.setImagenAncho(meta.ancho());
        anuncio.setImagenAlto(meta.alto());
    }
}