// src/main/java/com/kiert/backend/features/recursos/service/RecursoBibliotecaService.java
package com.kiert.backend.features.recursos.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiert.backend.entity.HistorialRecursoBiblioteca;
import com.kiert.backend.entity.RecursoBiblioteca;
import com.kiert.backend.entity.Usuario;
import com.kiert.backend.exception.AccesoDenegadoException;
import com.kiert.backend.exception.BadRequestException;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.features.recursos.dto.HistorialRecursoDTO;
import com.kiert.backend.features.recursos.dto.RecursoBibliotecaDTO;
import com.kiert.backend.features.recursos.dto.request.RecursoUsuarioRequest;
import com.kiert.backend.features.recursos.repository.HistorialRecursoBibliotecaRepository;
import com.kiert.backend.features.recursos.repository.RecursoBibliotecaRepository;
import com.kiert.backend.features.usuario.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
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
    private final HistorialRecursoBibliotecaRepository historialRepository;
    private final ObjectMapper objectMapper;

    private static final String CACHE_RECURSOS = "recursosBiblioteca";
    private static final String CACHE_RECURSO = "recursoBiblioteca";
    private static final String CACHE_DESTACADOS = "recursosDestacados";
    private static final String CACHE_CATEGORIAS = "categoriasBiblioteca";
    private static final String CACHE_NIVELES = "nivelesBiblioteca";

    private static final int MAX_TITULO = 200;
    private static final int MAX_DESCRIPCION = 1000;
    private static final int MAX_URL = 500;
    private static final int MAX_TAGS = 10;
    private static final int MAX_TAG_LENGTH = 30;
    private static final int MAX_RECURSOS_POR_USUARIO = 100;

    // ============================================================
    // LECTURA
    // ============================================================

    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_RECURSOS, key = "'usuario:' + (#usuarioId != null ? #usuarioId : 'anon')")
    public List<RecursoBibliotecaDTO> listarTodos(Long usuarioId) {
        log.debug("[DB] Listando recursos visibles para usuario {}", usuarioId);
        List<RecursoBiblioteca> recursos = (usuarioId != null)
                ? recursoRepository.findVisiblesParaUsuario(usuarioId)
                : recursoRepository.findAllActiveOrderByFechaAgregadoDesc();
        return recursos.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_RECURSOS, key = "'usuario:' + (#usuarioId != null ? #usuarioId : 'anon') + ':cat:' + #categoria")
    public List<RecursoBibliotecaDTO> listarPorCategoria(Long usuarioId, String categoria) {
        log.debug("[DB] Listando recursos categoria={} para usuario {}", categoria, usuarioId);
        List<RecursoBiblioteca> recursos = (usuarioId != null)
                ? recursoRepository.findVisiblesPorCategoria(usuarioId, categoria)
                : recursoRepository.findByCategoriaOrderByFechaAgregadoDesc(categoria);
        return recursos.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_DESTACADOS, key = "'all'")
    public List<RecursoBibliotecaDTO> listarDestacados() {
        log.debug("[DB] Listando recursos destacados");
        return recursoRepository.findDestacados()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_CATEGORIAS, key = "'all'")
    public List<String> obtenerCategorias() {
        return recursoRepository.findDistinctCategorias();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_NIVELES, key = "'all'")
    public List<String> obtenerNiveles() {
        return recursoRepository.findDistinctNiveles();
    }

    @Transactional(readOnly = true)
    public List<RecursoBibliotecaDTO> buscar(Long usuarioId, String query) {
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
    @Cacheable(value = CACHE_RECURSO, key = "#id")
    public RecursoBibliotecaDTO obtenerPorId(Long id) {
        if (id == null) {
            throw new BadRequestException("ID requerido");
        }
        RecursoBiblioteca recurso = recursoRepository.findActivoById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Recurso no encontrado"));
        return toDTO(recurso);
    }

    // ============================================================
    // CRUD DEL USUARIO (con historial)
    // ============================================================

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_RECURSOS, allEntries = true),
            @CacheEvict(value = CACHE_DESTACADOS, allEntries = true),
            @CacheEvict(value = CACHE_CATEGORIAS, allEntries = true),
            @CacheEvict(value = CACHE_NIVELES, allEntries = true)
    })
    public RecursoBibliotecaDTO crear(Long usuarioId, RecursoUsuarioRequest request, HttpServletRequest httpRequest) {
        log.info("Creando recurso para usuario {}", usuarioId);

        if (usuarioId == null) throw new BadRequestException("Usuario requerido");
        if (request == null) throw new BadRequestException("Datos requeridos");

        validarRequest(request);
        validarUrl(request.getUrl());

        long totalRecursos = recursoRepository.countByUsuarioId(usuarioId);
        if (totalRecursos >= MAX_RECURSOS_POR_USUARIO) {
            throw new BadRequestException("Has alcanzado el limite de recursos. Elimina algunos antes de crear mas.");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

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
                .destacado(false)
                .activo(true)
                .tags(tagsToString(request.getTags()))
                .build();

        recurso = recursoRepository.save(recurso);

        registrarHistorial(recurso, usuario, "CREAR", null, toDTO(recurso), httpRequest);

        return toDTO(recurso);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_RECURSOS, allEntries = true),
            @CacheEvict(value = CACHE_RECURSO, key = "#recursoId"),
            @CacheEvict(value = CACHE_DESTACADOS, allEntries = true),
            @CacheEvict(value = CACHE_CATEGORIAS, allEntries = true),
            @CacheEvict(value = CACHE_NIVELES, allEntries = true)
    })
    public RecursoBibliotecaDTO actualizar(Long usuarioId, Long recursoId, RecursoUsuarioRequest request, HttpServletRequest httpRequest) {
        log.info("Actualizando recurso {} del usuario {}", recursoId, usuarioId);

        if (usuarioId == null || recursoId == null) throw new BadRequestException("Parametros invalidos");
        if (request == null) throw new BadRequestException("Datos requeridos");

        validarRequest(request);
        validarUrl(request.getUrl());

        RecursoBiblioteca recurso = recursoRepository.findActivoById(recursoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Recurso no encontrado"));

        validarPropiedad(recurso, usuarioId);

        RecursoBibliotecaDTO anterior = toDTO(recurso);

        recurso.setTitulo(request.getTitulo().trim());
        recurso.setDescripcion(trimOrEmpty(request.getDescripcion()));
        recurso.setUrl(request.getUrl().trim());
        recurso.setCategoria(request.getCategoria().trim().toLowerCase());
        recurso.setSubcategoria(trimOrNull(request.getSubcategoria()));
        recurso.setAutor(trimOrNull(request.getAutor()));
        recurso.setPlataforma(trimOrNull(request.getPlataforma()));
        recurso.setDuracion(trimOrNull(request.getDuracion()));
        recurso.setNivel(trimOrNull(request.getNivel()));
        recurso.setTags(tagsToString(request.getTags()));

        recurso = recursoRepository.save(recurso);

        registrarHistorial(recurso, recurso.getUsuario(), "EDITAR", anterior, toDTO(recurso), httpRequest);

        return toDTO(recurso);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_RECURSOS, allEntries = true),
            @CacheEvict(value = CACHE_RECURSO, key = "#recursoId"),
            @CacheEvict(value = CACHE_DESTACADOS, allEntries = true),
            @CacheEvict(value = CACHE_CATEGORIAS, allEntries = true),
            @CacheEvict(value = CACHE_NIVELES, allEntries = true)
    })
    public void eliminar(Long usuarioId, Long recursoId, HttpServletRequest httpRequest) {
        log.info("Eliminando recurso {} del usuario {}", recursoId, usuarioId);

        if (usuarioId == null || recursoId == null) throw new BadRequestException("Parametros invalidos");

        RecursoBiblioteca recurso = recursoRepository.findActivoById(recursoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Recurso no encontrado"));

        validarPropiedad(recurso, usuarioId);

        RecursoBibliotecaDTO anterior = toDTO(recurso);

        recurso.setActivo(false);
        recursoRepository.save(recurso);

        registrarHistorial(recurso, recurso.getUsuario(), "ELIMINAR", anterior, null, httpRequest);

        log.info("Recurso {} eliminado (logico)", recursoId);
    }

    // ============================================================
    // HISTORIAL
    // ============================================================

    @Transactional(readOnly = true)
    public List<HistorialRecursoDTO> obtenerHistorial(Long recursoId) {
        return historialRepository.findByRecursoIdOrderByFechaAccionDesc(recursoId)
                .stream()
                .map(this::toHistorialDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<HistorialRecursoDTO> obtenerHistorialUsuario(Long usuarioId) {
        return historialRepository.findByUsuarioIdOrderByFechaAccionDesc(usuarioId)
                .stream()
                .map(this::toHistorialDTO)
                .collect(Collectors.toList());
    }

    // ============================================================
    // AUXILIARES
    // ============================================================

    private void registrarHistorial(RecursoBiblioteca recurso, Usuario usuario, String accion,
                                    RecursoBibliotecaDTO anterior, RecursoBibliotecaDTO nuevo,
                                    HttpServletRequest request) {
        try {
            HistorialRecursoBiblioteca historial = HistorialRecursoBiblioteca.builder()
                    .recurso(recurso)
                    .usuario(usuario)
                    .accion(accion)
                    .datosAnteriores(anterior != null ? objectMapper.writeValueAsString(anterior) : null)
                    .datosNuevos(nuevo != null ? objectMapper.writeValueAsString(nuevo) : null)
                    .ipUsuario(request != null ? obtenerIp(request) : null)
                    .build();
            historialRepository.save(historial);
        } catch (JsonProcessingException e) {
            log.warn("No se pudo serializar el historial para recurso {}: {}", recurso.getId(), e.getMessage());
        }
    }

    private String obtenerIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = request.getRemoteAddr();
        return ip != null ? ip.split(",")[0].trim() : null;
    }

    // ============================================================
    // VALIDACIONES
    // ============================================================
    private void validarRequest(RecursoUsuarioRequest request) {
        if (request.getTitulo() == null || request.getTitulo().isBlank()) {
            throw new BadRequestException("El titulo es obligatorio");
        }
        if (request.getTitulo().length() > MAX_TITULO) {
            throw new BadRequestException("El titulo es demasiado largo");
        }
        if (request.getDescripcion() != null && request.getDescripcion().length() > MAX_DESCRIPCION) {
            throw new BadRequestException("La descripcion es demasiado larga");
        }
        if (request.getUrl() == null || request.getUrl().isBlank()) {
            throw new BadRequestException("La URL es obligatoria");
        }
        if (request.getUrl().length() > MAX_URL) {
            throw new BadRequestException("La URL es demasiado larga");
        }
        if (request.getCategoria() == null || request.getCategoria().isBlank()) {
            throw new BadRequestException("La categoria es obligatoria");
        }
        if (request.getTags() != null && request.getTags().size() > MAX_TAGS) {
            throw new BadRequestException("Maximo " + MAX_TAGS + " tags");
        }
        if (request.getTags() != null) {
            for (String tag : request.getTags()) {
                if (tag != null && tag.length() > MAX_TAG_LENGTH) {
                    throw new BadRequestException("Cada tag debe tener maximo " + MAX_TAG_LENGTH + " caracteres");
                }
            }
        }
    }

    /**
     * Validacion de URL simplificada:
     * - Solo exige https://
     * - Rechaza IPs directas, localhost y rangos privados (seguridad basica)
     * - Permite CUALQUIER dominio externo (netflix.com, twitch.tv, etc.)
     */
    private void validarUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new BadRequestException("La URL es obligatoria");
        }

        final String urlLimpia = url.trim();

        if (!urlLimpia.startsWith("https://")) {
            throw new BadRequestException("La URL debe comenzar con https://");
        }

        final URI uri;
        try {
            uri = URI.create(urlLimpia);
        } catch (Exception e) {
            throw new BadRequestException("La URL no tiene un formato valido");
        }

        final String hostRaw = uri.getHost();
        if (hostRaw == null || hostRaw.isBlank()) {
            throw new BadRequestException("URL invalida: no tiene dominio");
        }

        if (hostRaw.startsWith("[")) {
            throw new BadRequestException("No se permiten direcciones IPv6 directas");
        }

        final String host = hostRaw.toLowerCase();

        // Bloquear IPs directas
        if (host.matches("^\\d{1,3}(\\.\\d{1,3}){3}$")) {
            throw new BadRequestException("No se permiten IPs directas");
        }

        // Bloquear localhost y rangos privados
        if (host.equals("localhost")
                || host.startsWith("127.")
                || host.startsWith("192.168.")
                || host.startsWith("10.")
                || host.equals("0.0.0.0")
                || host.startsWith("169.254.")) {
            throw new BadRequestException("No se permiten URLs internas");
        }

        if (host.startsWith("172.")) {
            String[] partes = host.split("\\.");
            if (partes.length >= 2) {
                try {
                    int segundo = Integer.parseInt(partes[1]);
                    if (segundo >= 16 && segundo <= 31) {
                        throw new BadRequestException("No se permiten URLs internas");
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        // NOTA: Ya NO se valida contra lista blanca. Cualquier dominio externo se acepta.
    }

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

    private HistorialRecursoDTO toHistorialDTO(HistorialRecursoBiblioteca h) {
        return new HistorialRecursoDTO(
                h.getId(),
                h.getRecurso() != null ? h.getRecurso().getId() : null,
                h.getAccion(),
                h.getDatosAnteriores(),
                h.getDatosNuevos(),
                h.getIpUsuario(),
                h.getUsuario() != null ? h.getUsuario().getNombreUsuario() : null,
                h.getFechaAccion()
        );
    }

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