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
            if (anuncio.getImagenPublicId() != null) {
                cloudinaryService.eliminarArchivo(anuncio.getImagenPublicId(), "image");
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
            if (publicIdAnterior != null) {
                cloudinaryService.eliminarArchivo(publicIdAnterior, "image");
            }
        }
        return anuncioRepository.save(anuncio);
    }

    @Transactional
    public void eliminar(Long id) {
        Anuncio anuncio = obtenerPorId(id);
        if (anuncio.getImagenPublicId() != null) {
            cloudinaryService.eliminarArchivo(anuncio.getImagenPublicId(), "image");
        }
        anuncioRepository.delete(anuncio);
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