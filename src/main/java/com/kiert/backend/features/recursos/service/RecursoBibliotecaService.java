// src/main/java/com/kiert/backend/service/RecursoBibliotecaService.java
package com.kiert.backend.features.recursos.service;

import com.kiert.backend.features.recursos.dto.RecursoBibliotecaDTO;
import com.kiert.backend.features.recursos.dto.request.RecursoUsuarioRequest;
import com.kiert.backend.entity.RecursoBiblioteca;
import com.kiert.backend.entity.Usuario;
import com.kiert.backend.exception.AccesoDenegadoException;
import com.kiert.backend.exception.BadRequestException;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.features.recursos.repository.RecursoBibliotecaRepository;
import com.kiert.backend.features.usuario.repository.UsuarioRepository;
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

    private static final String CACHE_RECURSOS = "recursosBiblioteca";
    private static final String CACHE_RECURSO = "recursoBiblioteca";
    private static final String CACHE_DESTACADOS = "recursosDestacados";
    private static final String CACHE_CATEGORIAS = "categoriasBiblioteca";
    private static final String CACHE_NIVELES = "nivelesBiblioteca";

    // Límites de validación
    private static final int MAX_TITULO = 200;
    private static final int MAX_DESCRIPCION = 1000;
    private static final int MAX_URL = 500;
    private static final int MAX_TAGS = 10;
    private static final int MAX_TAG_LENGTH = 30;

    // Whitelist de dominios permitidos
    private static final List<String> DOMINIOS_PERMITIDOS = List.of(
            "coursera.org", "edx.org", "udemy.com", "udacity.com", "platzi.com",
            "domestika.org", "crehana.com", "khanacademy.org", "classcentral.com",
            "freecodecamp.org", "codecademy.com", "aprendesql.dev", "midu.link",
            "mygreatlearning.com", "exceedlms.com", "nobook.com",
            "developer.mozilla.org", "docs.microsoft.com", "docs.python.org",
            "docs.oracle.com", "docs.spring.io", "angular.io", "react.dev",
            "vuejs.org", "nodejs.org", "kotlinlang.org", "go.dev", "rust-lang.org",
            "minecraft.wiki", "wikipedia.org",
            "github.com", "gitlab.com", "bitbucket.org", "stackoverflow.com",
            "figma.com", "notion.so", "trello.com", "miro.com",
            "photopea.com", "ninite.com", "ventoy.net", "rufus.ie",
            "optijuegos.net", "4download.net", "technical.city", "ifixit.com",
            "duolingo.com", "babbel.com", "busuu.com", "lingoda.com",
            "youtube.com", "vimeo.com", "ted.com",
            "medium.com", "dev.to", "vidaextra.com", "scholar.google.com",
            "hackthebox.com", "tryhackme.com", "netacad.com",
            "openai.com", "anthropic.com", "claude.ai", "chatgpt.com",
            "gemini.google.com", "perplexity.ai", "deepseek.com", "kimi.ai",
            "grok.com", "toolify.ai", "opencode.ai", "pippit.ai",
            "kaggle.com", "hackerrank.com", "leetcode.com", "codewars.com",
            "encoded.pe", "rebajitas.com", "itsfree.dev", "minecraftsearch.com",
            "frivlegend.com", "kawaiianimes.app", "moewalls.com",
            "wallpapercave.com", "wallpaper.mob.org", "alphacoders.com",
            "geode-sdk.org", "viper.pages.dev", "rojatvdirecta.online",
            "futbollibrefullhd.org", "capacitacionlaboral.trabajo.gob.pe"
    );

    // ============================================================
    // LECTURA
    // ============================================================

    @Transactional(readOnly = true)
    @Cacheable(
            value = CACHE_RECURSOS,
            key = "'usuario:' + (#usuarioId != null ? #usuarioId : 'anon')"
    )
    public List<RecursoBibliotecaDTO> listarTodos(Long usuarioId) {
        log.info("[DB] Listando recursos visibles para usuario {}", usuarioId);
        List<RecursoBiblioteca> recursos = (usuarioId != null)
                ? recursoRepository.findVisiblesParaUsuario(usuarioId)
                : recursoRepository.findAllActiveOrderByFechaAgregadoDesc();
        return recursos.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(
            value = CACHE_RECURSOS,
            key = "'usuario:' + (#usuarioId != null ? #usuarioId : 'anon') + ':cat:' + #categoria"
    )
    public List<RecursoBibliotecaDTO> listarPorCategoria(Long usuarioId, String categoria) {
        log.info("[DB] Listando recursos categoría={} para usuario {}", categoria, usuarioId);
        List<RecursoBiblioteca> recursos = (usuarioId != null)
                ? recursoRepository.findVisiblesPorCategoria(usuarioId, categoria)
                : recursoRepository.findByCategoriaOrderByFechaAgregadoDesc(categoria);
        return recursos.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_DESTACADOS, key = "'all'")
    public List<RecursoBibliotecaDTO> listarDestacados() {
        log.info("[DB] Listando recursos destacados");
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
    // CRUD DEL USUARIO
    // ============================================================

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_RECURSOS, allEntries = true),
            @CacheEvict(value = CACHE_DESTACADOS, allEntries = true),
            @CacheEvict(value = CACHE_CATEGORIAS, allEntries = true),
            @CacheEvict(value = CACHE_NIVELES, allEntries = true)
    })
    public RecursoBibliotecaDTO crear(Long usuarioId, RecursoUsuarioRequest request) {
        log.info("Creando recurso para usuario {}", usuarioId);

        if (usuarioId == null) {
            throw new BadRequestException("Usuario requerido");
        }
        if (request == null) {
            throw new BadRequestException("Datos requeridos");
        }

        validarRequest(request);
        validarUrl(request.getUrl());

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
                .destacado(false) // Solo admin puede destacar
                .activo(true)
                .tags(tagsToString(request.getTags()))
                .build();

        RecursoBiblioteca guardado = recursoRepository.save(recurso);
        return toDTO(guardado);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_RECURSOS, allEntries = true),
            @CacheEvict(value = CACHE_RECURSO, key = "#recursoId"),
            @CacheEvict(value = CACHE_DESTACADOS, allEntries = true),
            @CacheEvict(value = CACHE_CATEGORIAS, allEntries = true),
            @CacheEvict(value = CACHE_NIVELES, allEntries = true)
    })
    public RecursoBibliotecaDTO actualizar(Long usuarioId, Long recursoId, RecursoUsuarioRequest request) {
        log.info("Actualizando recurso {} del usuario {}", recursoId, usuarioId);

        if (usuarioId == null || recursoId == null) {
            throw new BadRequestException("Parámetros inválidos");
        }
        if (request == null) {
            throw new BadRequestException("Datos requeridos");
        }

        validarRequest(request);
        validarUrl(request.getUrl());

        RecursoBiblioteca recurso = recursoRepository.findActivoById(recursoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Recurso no encontrado"));

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
        // NO permitir que el usuario se auto-destaque
        // recurso.setDestacado(...) — solo admin
        recurso.setTags(tagsToString(request.getTags()));

        RecursoBiblioteca actualizado = recursoRepository.save(recurso);
        return toDTO(actualizado);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_RECURSOS, allEntries = true),
            @CacheEvict(value = CACHE_RECURSO, key = "#recursoId"),
            @CacheEvict(value = CACHE_DESTACADOS, allEntries = true),
            @CacheEvict(value = CACHE_CATEGORIAS, allEntries = true),
            @CacheEvict(value = CACHE_NIVELES, allEntries = true)
    })
    public void eliminar(Long usuarioId, Long recursoId) {
        if (usuarioId == null || recursoId == null) {
            throw new BadRequestException("Parámetros inválidos");
        }

        RecursoBiblioteca recurso = recursoRepository.findActivoById(recursoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Recurso no encontrado"));

        validarPropiedad(recurso, usuarioId);

        recurso.setActivo(false);
        recursoRepository.save(recurso);
        log.info("Recurso {} eliminado (lógico)", recursoId);
    }

    // ============================================================
    // VALIDACIONES
    // ============================================================
    private void validarRequest(RecursoUsuarioRequest request) {
        if (request.getTitulo() == null || request.getTitulo().isBlank()) {
            throw new BadRequestException("El título es obligatorio");
        }
        if (request.getTitulo().length() > MAX_TITULO) {
            throw new BadRequestException("El título es demasiado largo");
        }
        if (request.getDescripcion() != null && request.getDescripcion().length() > MAX_DESCRIPCION) {
            throw new BadRequestException("La descripción es demasiado larga");
        }
        if (request.getUrl() == null || request.getUrl().isBlank()) {
            throw new BadRequestException("La URL es obligatoria");
        }
        if (request.getUrl().length() > MAX_URL) {
            throw new BadRequestException("La URL es demasiado larga");
        }
        if (request.getCategoria() == null || request.getCategoria().isBlank()) {
            throw new BadRequestException("La categoría es obligatoria");
        }
        if (request.getTags() != null && request.getTags().size() > MAX_TAGS) {
            throw new BadRequestException("Máximo " + MAX_TAGS + " tags");
        }
        if (request.getTags() != null) {
            for (String tag : request.getTags()) {
                if (tag != null && tag.length() > MAX_TAG_LENGTH) {
                    throw new BadRequestException("Cada tag debe tener máximo " + MAX_TAG_LENGTH + " caracteres");
                }
            }
        }
    }

    private void validarUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new BadRequestException("La URL es obligatoria");
        }

        final String urlLimpia = url.trim();

        // Protocolo: SOLO https://
        if (!urlLimpia.startsWith("https://")) {
            throw new BadRequestException("La URL debe comenzar con https://");
        }

        final URI uri;
        try {
            uri = URI.create(urlLimpia);
        } catch (Exception e) {
            throw new BadRequestException("La URL no tiene un formato válido");
        }

        final String hostRaw = uri.getHost();
        if (hostRaw == null || hostRaw.isBlank()) {
            throw new BadRequestException("URL inválida: no tiene dominio");
        }

        final String host = hostRaw.toLowerCase();

        // Bloquear IPs directas
        if (host.matches("^\\d{1,3}(\\.\\d{1,3}){3}$")) {
            throw new BadRequestException("No se permiten IPs directas");
        }

        // Bloquear localhost / IPs privadas
        if (host.equals("localhost") || host.startsWith("127.") ||
                host.startsWith("192.168.") || host.startsWith("10.") ||
                host.startsWith("172.")) {
            throw new BadRequestException("No se permiten URLs internas");
        }

        // Bloquear acortadores
        final List<String> acortadores = List.of(
                "bit.ly", "tinyurl.com", "t.co", "goo.gl", "ow.ly",
                "is.gd", "buff.ly", "shorturl.at", "cutt.ly", "rb.gy"
        );
        for (final String acortador : acortadores) {
            if (host.equals(acortador) || host.endsWith("." + acortador)) {
                throw new BadRequestException("No se permiten acortadores de URL");
            }
        }

        // Lista blanca de dominios
        final boolean permitido = DOMINIOS_PERMITIDOS.stream()
                .anyMatch(d -> host.equals(d) || host.endsWith("." + d));

        if (!permitido) {
            throw new BadRequestException(
                    "El dominio '" + host + "' no está en la lista de sitios permitidos"
            );
        }
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