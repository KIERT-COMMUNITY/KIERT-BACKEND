// src/main/java/com/kiert/backend/service/RecursoBibliotecaService.java
package com.kiert.backend.service;

import com.kiert.backend.dto.RecursoBibliotecaDTO;
import com.kiert.backend.dto.request.RecursoUsuarioRequest;
import com.kiert.backend.entity.RecursoBiblioteca;
import com.kiert.backend.entity.Usuario;
import com.kiert.backend.exception.AccesoDenegadoException;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.repository.RecursoBibliotecaRepository;
import com.kiert.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecursoBibliotecaService {

    private final RecursoBibliotecaRepository recursoRepository;
    private final UsuarioRepository usuarioRepository;

    // ============================================================
    // LECTURA (globales + propios del usuario)
    // ============================================================

    @Transactional(readOnly = true)
    public List<RecursoBibliotecaDTO> listarTodos(Long usuarioId) {
        log.info("📋 Listando recursos visibles para usuario {}", usuarioId);
        List<RecursoBiblioteca> recursos = (usuarioId != null)
                ? recursoRepository.findVisiblesParaUsuario(usuarioId)
                : recursoRepository.findAllActiveOrderByFechaAgregadoDesc();
        return recursos.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RecursoBibliotecaDTO> listarPorCategoria(Long usuarioId, String categoria) {
        log.info("📋 Listando recursos categoría={} para usuario {}", categoria, usuarioId);
        List<RecursoBiblioteca> recursos = (usuarioId != null)
                ? recursoRepository.findVisiblesPorCategoria(usuarioId, categoria)
                : recursoRepository.findByCategoriaOrderByFechaAgregadoDesc(categoria);
        return recursos.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RecursoBibliotecaDTO> listarDestacados() {
        log.info("⭐ Listando recursos destacados");
        return recursoRepository.findDestacados()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> obtenerCategorias() {
        log.info("📋 Obteniendo categorías de la biblioteca");
        return recursoRepository.findDistinctCategorias();
    }

    @Transactional(readOnly = true)
    public List<String> obtenerNiveles() {
        log.info("📋 Obteniendo niveles de la biblioteca");
        return recursoRepository.findDistinctNiveles();
    }

    @Transactional(readOnly = true)
    public List<RecursoBibliotecaDTO> buscar(Long usuarioId, String query) {
        log.info("🔍 Buscando recursos query={} para usuario {}", query, usuarioId);
        if (query == null || query.trim().isEmpty()) {
            return listarTodos(usuarioId);
        }
        List<RecursoBiblioteca> recursos = (usuarioId != null)
                ? recursoRepository.buscarVisiblesParaUsuario(usuarioId, query.trim())
                : recursoRepository.buscar(query.trim());
        return recursos.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RecursoBibliotecaDTO> buscarPorCategoria(String categoria, String query) {
        log.info("🔍 Buscando recursos en categoría {}: {}", categoria, query);
        if (query == null || query.trim().isEmpty()) {
            return recursoRepository.findByCategoriaOrderByFechaAgregadoDesc(categoria)
                    .stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList());
        }
        return recursoRepository.buscarPorCategoria(categoria, query.trim())
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RecursoBibliotecaDTO obtenerPorId(Long id) {
        log.info("🔍 Obteniendo recurso: {}", id);
        RecursoBiblioteca recurso = recursoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Recurso no encontrado"));
        return toDTO(recurso);
    }

    // ============================================================
    // 🔥 CRUD DEL USUARIO
    // ============================================================

    @Transactional
    public RecursoBibliotecaDTO crear(Long usuarioId, RecursoUsuarioRequest request) {
        log.info("➕ Creando recurso para usuario {}", usuarioId);

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario no encontrado: " + usuarioId));

        RecursoBiblioteca recurso = RecursoBiblioteca.builder()
                .usuario(usuario)
                .esUsuario(true)
                .titulo(request.getTitulo().trim())
                .descripcion(trimOrEmpty(request.getDescripcion()))
                .url(request.getUrl().trim())
                .categoria(request.getCategoria().trim().toLowerCase())
                .subcategoria(trimOrNull(request.getSubcategoria()))
                .autor(trimOrNull(request.getAutor()))
                .plataforma(trimOrNull(request.getPlataforma()))
                .duracion(trimOrNull(request.getDuracion()))
                .nivel(trimOrNull(request.getNivel()))
                .destacado(Boolean.TRUE.equals(request.getDestacado()))
                .activo(true)
                .tags(tagsToString(request.getTags()))
                .build();

        RecursoBiblioteca guardado = recursoRepository.save(recurso);
        log.info("✅ Recurso {} creado por usuario {}", guardado.getId(), usuarioId);
        return toDTO(guardado);
    }

    @Transactional
    public RecursoBibliotecaDTO actualizar(Long usuarioId, Long recursoId, RecursoUsuarioRequest request) {
        log.info("✏️ Actualizando recurso {} del usuario {}", recursoId, usuarioId);

        RecursoBiblioteca recurso = recursoRepository.findById(recursoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Recurso no encontrado: " + recursoId));

        validarPropiedad(recurso, usuarioId);

        recurso.setTitulo(request.getTitulo().trim());
        recurso.setDescripcion(trimOrEmpty(request.getDescripcion()));
        recurso.setUrl(request.getUrl().trim());
        recurso.setCategoria(request.getCategoria().trim().toLowerCase());
        recurso.setSubcategoria(trimOrNull(request.getSubcategoria()));
        recurso.setAutor(trimOrNull(request.getAutor()));
        recurso.setPlataforma(trimOrNull(request.getPlataforma()));
        recurso.setDuracion(trimOrNull(request.getDuracion()));
        recurso.setNivel(trimOrNull(request.getNivel()));
        recurso.setDestacado(Boolean.TRUE.equals(request.getDestacado()));
        recurso.setTags(tagsToString(request.getTags()));

        RecursoBiblioteca actualizado = recursoRepository.save(recurso);
        log.info("✅ Recurso {} actualizado", recursoId);
        return toDTO(actualizado);
    }

    @Transactional
    public void eliminar(Long usuarioId, Long recursoId) {
        log.info("🗑️ Eliminando recurso {} del usuario {}", recursoId, usuarioId);

        RecursoBiblioteca recurso = recursoRepository.findById(recursoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Recurso no encontrado: " + recursoId));

        validarPropiedad(recurso, usuarioId);

        recurso.setActivo(false); // borrado lógico
        recursoRepository.save(recurso);
        log.info("✅ Recurso {} eliminado (lógico)", recursoId);
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private void validarPropiedad(RecursoBiblioteca recurso, Long usuarioId) {
        if (!Boolean.TRUE.equals(recurso.getEsUsuario())
                || recurso.getUsuario() == null
                || !recurso.getUsuario().getId().equals(usuarioId)) {
            throw new AccesoDenegadoException("No tienes permisos sobre este recurso");
        }
    }

    private String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /** Devuelve cadena vacía en lugar de null (para columnas NOT NULL) */
    private String trimOrEmpty(String s) {
        if (s == null) return "";
        return s.trim();
    }

    private String tagsToString(List<String> tags) {
        if (tags == null || tags.isEmpty()) return null;
        String joined = tags.stream()
                .filter(t -> t != null && !t.isBlank())
                .map(String::trim)
                .distinct()
                .collect(Collectors.joining(","));
        return joined.isEmpty() ? null : joined;
    }

    // ============================================================
    // DTO MAPPING
    // ============================================================

    private RecursoBibliotecaDTO toDTO(RecursoBiblioteca entity) {
        List<String> tags = (entity.getTags() != null && !entity.getTags().isEmpty())
                ? Arrays.stream(entity.getTags().split(","))
                  .map(String::trim)
                  .filter(s -> !s.isEmpty())
                  .collect(Collectors.toList())
                : Collections.emptyList();

        Long usuarioId = entity.getUsuario() != null ? entity.getUsuario().getId() : null;
        String usuarioNombre = entity.getUsuario() != null
                ? entity.getUsuario().getNombreUsuario()
                : null;

        return new RecursoBibliotecaDTO(
                entity.getId(),
                entity.getTitulo(),
                entity.getDescripcion(),
                entity.getUrl(),
                entity.getCategoria(),
                entity.getSubcategoria(),
                entity.getImagen(),
                entity.getAutor(),
                entity.getPlataforma(),
                entity.getDuracion(),
                entity.getNivel(),
                entity.getDestacado(),
                entity.getEsUsuario(),
                usuarioId,
                usuarioNombre,
                entity.getFechaAgregado(),
                entity.getFechaActualizacion(),
                tags
        );
    }
}