// src/main/java/com/kiert/backend/service/AnuncioService.java
package com.kiert.backend.service;

import com.kiert.backend.dto.ImagenSubidaDTO;
import com.kiert.backend.entity.Anuncio;
import com.kiert.backend.exception.BadRequestException;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.repository.AnuncioRepository;
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

    @Transactional
    public Anuncio crear(String titulo, String texto, String enlaceUrl, MultipartFile imagen) {
        log.info("Creando anuncio: titulo='{}', tieneImagen={}", titulo, imagen != null && !imagen.isEmpty());

        Anuncio anuncio = Anuncio.builder()
                .titulo(titulo)
                .texto(texto)
                .enlaceUrl(enlaceUrl)
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
                log.error("Error al subir la imagen del anuncio a Cloudinary: {}", e.getMessage(), e);
                throw new BadRequestException("No se pudo subir la imagen: " + e.getMessage());
            }
        }

        try {
            Anuncio guardado = anuncioRepository.save(anuncio);
            log.info("Anuncio creado con id={}", guardado.getId());
            return guardado;
        } catch (Exception e) {
            log.error("Error al guardar el anuncio en BD: {}", e.getMessage(), e);
            // Si la imagen se subió pero el guardado falló, la borramos para no dejar huérfanos
            if (anuncio.getImagenPublicId() != null) {
                try {
                    cloudinaryService.eliminarArchivo(anuncio.getImagenPublicId(), "image");
                } catch (Exception ex) {
                    log.warn("No se pudo limpiar la imagen huérfana: {}", ex.getMessage());
                }
            }
            throw new BadRequestException("No se pudo guardar el anuncio: " + e.getMessage());
        }
    }

    @Transactional
    public Anuncio actualizar(Long id, String titulo, String texto, String enlaceUrl, MultipartFile nuevaImagen) {
        log.info("Actualizando anuncio id={}, tieneNuevaImagen={}", id, nuevaImagen != null && !nuevaImagen.isEmpty());

        Anuncio anuncio = obtenerPorId(id);

        anuncio.setTitulo(titulo);
        anuncio.setTexto(texto);
        anuncio.setEnlaceUrl(enlaceUrl);
        anuncio.setFechaActualizacion(LocalDateTime.now());

        if (nuevaImagen != null && !nuevaImagen.isEmpty()) {
            String publicIdAnterior = anuncio.getImagenPublicId();

            try {
                ImagenSubidaDTO meta = cloudinaryService.subirImagenConDetalles(nuevaImagen, "anuncios");
                aplicarMetaImagen(anuncio, meta);
            } catch (Exception e) {
                log.error("Error al subir la nueva imagen: {}", e.getMessage(), e);
                throw new BadRequestException("No se pudo subir la nueva imagen: " + e.getMessage());
            }

            // Borrar la anterior solo si la nueva se subió bien
            if (publicIdAnterior != null) {
                try {
                    cloudinaryService.eliminarArchivo(publicIdAnterior, "image");
                } catch (Exception e) {
                    log.warn("No se pudo borrar la imagen anterior ({}): {}", publicIdAnterior, e.getMessage());
                }
            }
        }

        try {
            return anuncioRepository.save(anuncio);
        } catch (Exception e) {
            log.error("Error al guardar el anuncio actualizado: {}", e.getMessage(), e);
            throw new BadRequestException("No se pudo actualizar el anuncio: " + e.getMessage());
        }
    }

    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando anuncio id={}", id);

        Anuncio anuncio = obtenerPorId(id);

        if (anuncio.getImagenPublicId() != null) {
            try {
                cloudinaryService.eliminarArchivo(anuncio.getImagenPublicId(), "image");
            } catch (Exception e) {
                log.warn("No se pudo borrar la imagen de Cloudinary: {}", e.getMessage());
            }
        }

        anuncioRepository.delete(anuncio);
        log.info("Anuncio {} eliminado", id);
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