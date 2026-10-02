// src/main/java/com/kiert/backend/service/DocumentoService.java
package com.kiert.backend.service;

import com.kiert.backend.dto.AutorResumenDTO;
import com.kiert.backend.dto.CrearDocumentoDTO;
import com.kiert.backend.dto.DocumentoDTO;
import com.kiert.backend.entity.Documento;
import com.kiert.backend.entity.Usuario;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.repository.DocumentoRepository;
import com.kiert.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentoService {

    private final DocumentoRepository documentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CloudinaryService cloudinaryService;

    private final List<String> CATEGORIAS_PREDEFINIDAS = List.of(
            "Matemáticas", "Comunicación", "Integrales", "Física", "Química",
            "Programación", "Inglés", "Historia", "Geografía", "Biología",
            "Economía", "Derecho", "Medicina", "Arquitectura", "Diseño"
    );

    // Nombres de caché centralizados
    private static final String CACHE_DOCUMENTOS = "documentos";
    private static final String CACHE_DOCUMENTO = "documento";
    private static final String CACHE_CATEGORIAS = "categoriasDocumentos";
    private static final String CACHE_DOCS_USUARIO = "documentosUsuario";

    // ============================================================
    // CATEGORÍAS (lectura muy frecuente)
    // ============================================================
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_CATEGORIAS, key = "'all'")
    public List<String> obtenerCategorias() {
        log.info("[DB] Obteniendo categorías de documentos");

        List<String> categoriasBD = documentoRepository.findDistinctCategorias();
        List<String> todas = new ArrayList<>(CATEGORIAS_PREDEFINIDAS);
        for (String cat : categoriasBD) {
            if (!todas.contains(cat)) {
                todas.add(cat);
            }
        }
        Collections.sort(todas);
        return todas;
    }

    // ============================================================
    // LISTAR DOCUMENTOS
    // ============================================================
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_DOCUMENTOS, key = "'all'")
    public List<DocumentoDTO> listarTodos() {
        log.info("[DB] Listando todos los documentos");
        return documentoRepository.findAllActiveOrderByFechaCreacionDesc()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(
            value = CACHE_DOCUMENTOS,
            key = "'categoria:' + #categoria"
    )
    public List<DocumentoDTO> listarPorCategoria(String categoria) {
        log.info("[DB] Listando documentos por categoría: {}", categoria);
        return documentoRepository.findByCategoriaOrderByFechaCreacionDesc(categoria)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(
            value = CACHE_DOCS_USUARIO,
            key = "'usuario:' + #usuarioId"
    )
    public List<DocumentoDTO> listarPorUsuario(Long usuarioId) {
        log.info("[DB] Listando documentos del usuario: {}", usuarioId);
        return documentoRepository.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ============================================================
    // OBTENER POR ID — solo lectura (cacheable)
    // ============================================================
    /**
     * CAMBIO IMPORTANTE: Este método YA NO incrementa visitas.
     * Antes lo hacía, lo cual rompía el caché (efecto secundario en método "readOnly").
     *
     * El controller debe llamar a registrarVisita(id) por separado.
     */
    @Transactional(readOnly = true)
    @Cacheable(
            value = CACHE_DOCUMENTO,
            key = "#id"
    )
    public DocumentoDTO obtenerPorId(Long id) {
        log.info("[DB] Obteniendo documento: {}", id);
        Documento documento = documentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Documento no encontrado"));
        return toDTO(documento);
    }

    /**
     * Registra una visita al documento (escritura, sin caché).
     * Llama a este método desde el controller cuando quieras contar una visita.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_DOCUMENTO, key = "#id"),
            @CacheEvict(value = CACHE_DOCUMENTOS, allEntries = true),
            @CacheEvict(value = CACHE_DOCS_USUARIO, allEntries = true)
    })
    public void registrarVisita(Long id) {
        log.debug("Registrando visita al documento: {}", id);
        documentoRepository.findById(id).ifPresent(documento -> {
            documento.setVisitas(documento.getVisitas() + 1);
            documentoRepository.save(documento);
        });
    }

    // ============================================================
    // BUSCAR
    // ============================================================
    @Transactional(readOnly = true)
    public List<DocumentoDTO> buscar(String query) {
        log.info("Buscando documentos: {}", query);
        if (query == null || query.trim().isEmpty()) {
            return listarTodos();  // usa el caché
        }
        // Las búsquedas por texto libre NO se cachean (muchas variantes)
        return documentoRepository.searchByTitulo(query.trim())
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DocumentoDTO> buscarPorCategoria(String categoria, String query) {
        log.info("Buscando documentos en categoría {}: {}", categoria, query);
        if (query == null || query.trim().isEmpty()) {
            return listarPorCategoria(categoria);  // usa el caché
        }
        // Las búsquedas por texto libre NO se cachean
        return documentoRepository.searchByCategoriaAndTitulo(categoria, query.trim())
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ============================================================
    // CREAR DOCUMENTO
    // ============================================================
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_DOCUMENTOS, allEntries = true),
            @CacheEvict(value = CACHE_DOCS_USUARIO, key = "'usuario:' + #usuarioId"),
            @CacheEvict(value = CACHE_CATEGORIAS, allEntries = true)
    })
    public DocumentoDTO crear(Long usuarioId, CrearDocumentoDTO datos, MultipartFile archivo) {
        log.info("Creando documento para usuario: {}", usuarioId);

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        String categoria = datos.categoria();
        if (categoria == null || categoria.trim().isEmpty()) {
            categoria = "Otros";
        }
        if (datos.categoriaPersonalizada() != null && !datos.categoriaPersonalizada().trim().isEmpty()) {
            categoria = datos.categoriaPersonalizada().trim();
        }

        if (archivo == null || archivo.isEmpty()) {
            throw new RuntimeException("Debes seleccionar un archivo");
        }

        String urlArchivo;
        String tipoArchivo;
        String nombreArchivo = archivo.getOriginalFilename();

        try {
            String carpeta = "documentos";
            String extension = obtenerExtension(nombreArchivo);
            tipoArchivo = extension.toLowerCase();

            if (esImagen(archivo)) {
                urlArchivo = cloudinaryService.subirImagen(archivo, carpeta + "/imagenes");
            } else if (esPdf(archivo)) {
                urlArchivo = cloudinaryService.subirArchivo(archivo, carpeta + "/pdfs");
            } else {
                urlArchivo = cloudinaryService.subirArchivo(archivo, carpeta + "/archivos");
            }

            log.info("Archivo subido exitosamente: {}", urlArchivo);
        } catch (Exception e) {
            log.error("Error al subir archivo: {}", e.getMessage());
            throw new RuntimeException("Error al subir el archivo: " + e.getMessage());
        }

        Documento documento = Documento.builder()
                .titulo(datos.titulo())
                .descripcion(datos.descripcion())
                .categoria(categoria)
                .categoriaPersonalizada(datos.categoriaPersonalizada())
                .urlArchivo(urlArchivo)
                .nombreArchivo(nombreArchivo)
                .tipoArchivo(tipoArchivo)
                .tamanoKb(archivo.getSize() / 1024)
                .usuario(usuario)
                .descargas(0L)
                .visitas(0L)
                .activo(true)
                .fechaCreacion(Instant.now())
                .build();

        documento = documentoRepository.save(documento);
        log.info("Documento creado con ID: {}", documento.getId());

        return toDTO(documento);
    }

    // ============================================================
    // ACTUALIZAR DOCUMENTO
    // ============================================================
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_DOCUMENTO, key = "#id"),
            @CacheEvict(value = CACHE_DOCUMENTOS, allEntries = true),
            @CacheEvict(value = CACHE_DOCS_USUARIO, key = "'usuario:' + #usuarioId"),
            @CacheEvict(value = CACHE_CATEGORIAS, allEntries = true)
    })
    public DocumentoDTO actualizar(Long id, Long usuarioId, CrearDocumentoDTO datos) {
        log.info("Actualizando documento: {}", id);

        Documento documento = documentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Documento no encontrado"));

        if (!documento.getUsuario().getId().equals(usuarioId)) {
            throw new SecurityException("No tienes permiso para modificar este documento");
        }

        String categoria = datos.categoria();
        if (datos.categoriaPersonalizada() != null && !datos.categoriaPersonalizada().trim().isEmpty()) {
            categoria = datos.categoriaPersonalizada().trim();
        }

        documento.setTitulo(datos.titulo());
        documento.setDescripcion(datos.descripcion());
        documento.setCategoria(categoria);
        documento.setCategoriaPersonalizada(datos.categoriaPersonalizada());
        documento.setFechaActualizacion(Instant.now());

        documento = documentoRepository.save(documento);
        log.info("Documento actualizado: {}", id);

        return toDTO(documento);
    }

    // ============================================================
    // ELIMINAR DOCUMENTO
    // ============================================================
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_DOCUMENTO, key = "#id"),
            @CacheEvict(value = CACHE_DOCUMENTOS, allEntries = true),
            @CacheEvict(value = CACHE_DOCS_USUARIO, key = "'usuario:' + #usuarioId"),
            @CacheEvict(value = CACHE_CATEGORIAS, allEntries = true)
    })
    public void eliminar(Long id, Long usuarioId) {
        log.info("Eliminando documento: {}", id);

        Documento documento = documentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Documento no encontrado"));

        if (!documento.getUsuario().getId().equals(usuarioId)) {
            throw new SecurityException("No tienes permiso para eliminar este documento");
        }

        documento.setActivo(false);
        documentoRepository.save(documento);
        log.info("Documento eliminado: {}", id);
    }

    // ============================================================
    // INCREMENTAR DESCARGAS
    // ============================================================
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_DOCUMENTO, key = "#id"),
            @CacheEvict(value = CACHE_DOCUMENTOS, allEntries = true)
    })
    public void incrementarDescargas(Long id) {
        log.debug("Incrementando descargas del documento: {}", id);

        Documento documento = documentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Documento no encontrado"));
        documento.setDescargas(documento.getDescargas() + 1);
        documentoRepository.save(documento);
    }

    // ============================================================
    // UTILIDADES
    // ============================================================
    private String obtenerExtension(String nombreArchivo) {
        if (nombreArchivo == null) return "archivo";
        int lastDot = nombreArchivo.lastIndexOf('.');
        if (lastDot == -1) return "archivo";
        return nombreArchivo.substring(lastDot + 1);
    }

    private boolean esImagen(MultipartFile archivo) {
        String contentType = archivo.getContentType();
        return contentType != null && contentType.startsWith("image/");
    }

    private boolean esPdf(MultipartFile archivo) {
        String contentType = archivo.getContentType();
        return contentType != null && contentType.equals("application/pdf");
    }

    private AutorResumenDTO toAutorDTO(Usuario usuario) {
        return new AutorResumenDTO(
                usuario.getId(),
                usuario.getNombreUsuario(),
                usuario.getFotoPerfilUrl(),
                usuario.getMarcoId()
        );
    }

    private DocumentoDTO toDTO(Documento documento) {
        return new DocumentoDTO(
                documento.getId(),
                documento.getTitulo(),
                documento.getDescripcion(),
                documento.getCategoria(),
                documento.getCategoriaPersonalizada(),
                documento.getUrlArchivo(),
                documento.getNombreArchivo(),
                documento.getTipoArchivo(),
                documento.getTamanoKb(),
                toAutorDTO(documento.getUsuario()),
                documento.getDescargas(),
                documento.getVisitas(),
                documento.getFechaCreacion()
        );
    }
}