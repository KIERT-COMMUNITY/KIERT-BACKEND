// src/main/java/com/kiert/backend/service/PersonalizacionService.java
package com.kiert.backend.service;

import com.kiert.backend.dto.*;
import com.kiert.backend.entity.*;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonalizacionService {

    private final PersonalizacionRepository personalizacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final CloudinaryService cloudinaryService;

    private static final String CACHE_PERSONALIZACION = "personalizacion";
    private static final String CACHE_MARCOS = "marcos";
    private static final String CACHE_FONDOS = "fondos";

    // ============================================================
    // OBTENER PERSONALIZACIÓN
    // ✅ La foto SIEMPRE viene de Usuario.fotoPerfilUrl
    // ============================================================
    @Transactional(readOnly = true)
    @Cacheable(
            value = CACHE_PERSONALIZACION,
            key = "#usuarioId",
            unless = "#result == null"
    )
    public PersonalizacionDTO obtenerPersonalizacion(Long usuarioId) {
        log.info("[DB] Obteniendo personalización para usuario: {}", usuarioId);

        if (usuarioId == null) {
            throw new IllegalArgumentException("Usuario ID no puede ser null");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        PersonalizacionUsuario personalizacion = personalizacionRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> crearPersonalizacionDefault(usuarioId));

        return toDTO(personalizacion, usuario);
    }

    // ============================================================
    // OBTENER PERSONALIZACIÓN POR USUARIO (público)
    // ============================================================
    @Transactional(readOnly = true)
    @Cacheable(
            value = CACHE_PERSONALIZACION,
            key = "'user:' + #usuarioId",
            unless = "#result == null"
    )
    public PersonalizacionDTO obtenerPersonalizacionPorUsuario(Long usuarioId) {
        log.info("[DB] Obteniendo personalización de usuario {} (público)", usuarioId);
        return obtenerPersonalizacion(usuarioId);
    }

    // ============================================================
    // GUARDAR PERSONALIZACIÓN
    // ============================================================
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_PERSONALIZACION, allEntries = true),
            @CacheEvict(value = "perfil", allEntries = true),
            @CacheEvict(value = "posts", allEntries = true),
            @CacheEvict(value = "post", allEntries = true),
            @CacheEvict(value = "comentarios", allEntries = true),
            @CacheEvict(value = "conversaciones", allEntries = true),
            @CacheEvict(value = "miembrosGrupo", allEntries = true),
            @CacheEvict(value = "documentos", allEntries = true)
    })
    public PersonalizacionDTO guardarPersonalizacion(Long usuarioId, String temaId, String marcoId, String fondoId) {
        log.info("Guardando personalización para usuario: {} con tema={}, marco={}, fondo={}",
                usuarioId, temaId, marcoId, fondoId);

        if (usuarioId == null) {
            throw new IllegalArgumentException("Usuario ID no puede ser null");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        PersonalizacionUsuario personalizacion = personalizacionRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> crearPersonalizacionDefault(usuarioId));

        if (temaId != null && !temaId.isEmpty() && !temaId.equals(personalizacion.getTemaId())) {
            personalizacion.setTemaId(temaId);
        }
        if (marcoId != null && !marcoId.isEmpty() && !marcoId.equals(personalizacion.getMarcoId())) {
            personalizacion.setMarcoId(marcoId);
        }
        if (fondoId != null && !fondoId.isEmpty() && !fondoId.equals(personalizacion.getFondoId())) {
            personalizacion.setFondoId(fondoId);
        }

        personalizacion = personalizacionRepository.save(personalizacion);
        return toDTO(personalizacion, usuario);
    }

    // ============================================================
    // SUBIR FOTO DE PERFIL
    // ✅ Solo actualiza USUARIO (fuente de verdad)
    // ============================================================
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_PERSONALIZACION, allEntries = true),
            @CacheEvict(value = "perfil", allEntries = true),
            @CacheEvict(value = "posts", allEntries = true),
            @CacheEvict(value = "post", allEntries = true),
            @CacheEvict(value = "comentarios", allEntries = true),
            @CacheEvict(value = "respuestas", allEntries = true),
            @CacheEvict(value = "documentos", allEntries = true),
            @CacheEvict(value = "conversaciones", allEntries = true),
            @CacheEvict(value = "miembrosGrupo", allEntries = true),
            @CacheEvict(value = "gruposUsuario", allEntries = true)
    })
    public PersonalizacionDTO subirFotoPerfil(Long usuarioId, MultipartFile archivo) {
        log.info("Subiendo foto de perfil para usuario: {}", usuarioId);

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        String urlAnterior = usuario.getFotoPerfilUrl();

        // ✅ Subir nueva a Cloudinary
        String url = cloudinaryService.subirArchivo(archivo, "perfiles");

        // ✅ Eliminar la anterior de Cloudinary
        if (urlAnterior != null && !urlAnterior.isBlank()) {
            log.info("Eliminando foto anterior de Cloudinary: {}", urlAnterior);
            cloudinaryService.eliminarArchivoPorUrl(urlAnterior);
        }

        // ✅ Actualizar en usuario (fuente de verdad)
        usuario.setFotoPerfilUrl(url);
        usuarioRepository.save(usuario);

        // Devolver DTO (la personalización ya no guarda foto)
        PersonalizacionUsuario personalizacion = personalizacionRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> crearPersonalizacionDefault(usuarioId));

        return toDTO(personalizacion, usuario);
    }

    // ============================================================
    // ✅ ELIMINAR FOTO DE PERFIL
    // ✅ Solo actualiza USUARIO (fuente de verdad)
    // ============================================================
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_PERSONALIZACION, allEntries = true),
            @CacheEvict(value = "perfil", allEntries = true),
            @CacheEvict(value = "posts", allEntries = true),
            @CacheEvict(value = "post", allEntries = true),
            @CacheEvict(value = "comentarios", allEntries = true),
            @CacheEvict(value = "respuestas", allEntries = true),
            @CacheEvict(value = "documentos", allEntries = true),
            @CacheEvict(value = "conversaciones", allEntries = true),
            @CacheEvict(value = "miembrosGrupo", allEntries = true),
            @CacheEvict(value = "gruposUsuario", allEntries = true)
    })
    public PersonalizacionDTO eliminarFotoPerfil(Long usuarioId) {
        log.info("Eliminando foto de perfil para usuario: {}", usuarioId);

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        String urlAnterior = usuario.getFotoPerfilUrl();

        if (urlAnterior != null && !urlAnterior.isBlank()) {
            log.info("Eliminando foto de Cloudinary: {}", urlAnterior);
            cloudinaryService.eliminarArchivoPorUrl(urlAnterior);
        }

        // ✅ Poner null en usuario (ÚNICA fuente de verdad)
        usuario.setFotoPerfilUrl(null);
        usuarioRepository.save(usuario);

        // Devolver DTO
        PersonalizacionUsuario personalizacion = personalizacionRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> crearPersonalizacionDefault(usuarioId));

        log.info("✅ Foto de perfil eliminada para el usuario {}", usuarioId);
        return toDTO(personalizacion, usuario);
    }

    // ============================================================
    // SUBIR FOTO DE PORTADA
    // ============================================================
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_PERSONALIZACION, allEntries = true),
            @CacheEvict(value = "perfil", allEntries = true)
    })
    public PersonalizacionDTO subirFotoPortada(Long usuarioId, MultipartFile archivo) {
        log.info("Subiendo foto de portada para usuario: {}", usuarioId);

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        PersonalizacionUsuario personalizacion = personalizacionRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> crearPersonalizacionDefault(usuarioId));

        String urlAnterior = personalizacion.getFotoPortadaUrl();
        String url = cloudinaryService.subirArchivo(archivo, "portadas");

        if (urlAnterior != null && !urlAnterior.isBlank()) {
            cloudinaryService.eliminarArchivoPorUrl(urlAnterior);
        }

        personalizacion.setFotoPortadaUrl(url);
        personalizacion = personalizacionRepository.save(personalizacion);

        return toDTO(personalizacion, usuario);
    }

    // ============================================================
    // MARCOS Y FONDOS
    // ============================================================
    @Cacheable(value = CACHE_MARCOS, key = "'all'")
    public List<MarcoDTO> obtenerMarcos(Long usuarioId) {
        return getMarcosDefault();
    }

    @Cacheable(value = CACHE_FONDOS, key = "'all'")
    public List<FondoDTO> obtenerFondos(Long usuarioId) {
        return getFondosDefault();
    }

    // ============================================================
    // LIMPIAR CACHÉ
    // ============================================================
    @Caching(evict = {
            @CacheEvict(value = CACHE_PERSONALIZACION, allEntries = true),
            @CacheEvict(value = CACHE_MARCOS, allEntries = true),
            @CacheEvict(value = CACHE_FONDOS, allEntries = true),
            @CacheEvict(value = "perfil", allEntries = true)
    })
    public void limpiarCache() {
        log.info("Limpiando caché de personalización");
    }

    // ============================================================
    // MÉTODOS PRIVADOS
    // ============================================================
    private List<MarcoDTO> getMarcosDefault() {
        List<MarcoDTO> marcos = new ArrayList<>();
        marcos.add(new MarcoDTO("none", "Sin marco", null, "circulo", 0.0, true));
        marcos.add(new MarcoDTO("classic", "Classic", null, "circulo", 0.0, true));
        marcos.add(new MarcoDTO("gold", "Gold", null, "circulo", 0.0, true));
        marcos.add(new MarcoDTO("silver", "Silver", null, "circulo", 0.0, true));
        marcos.add(new MarcoDTO("rainbow", "Rainbow", null, "circulo", 0.0, true));
        marcos.add(new MarcoDTO("pastel", "Pastel", null, "circulo", 0.0, true));
        marcos.add(new MarcoDTO("neon", "Neon", null, "circulo", 0.0, true));
        marcos.add(new MarcoDTO("ocean", "Ocean", null, "circulo", 0.0, true));
        marcos.add(new MarcoDTO("sunset", "Sunset", null, "circulo", 0.0, true));
        marcos.add(new MarcoDTO("galaxy", "Galaxy", null, "circulo", 0.0, true));
        marcos.add(new MarcoDTO("fire", "Fire", null, "circulo", 0.0, true));
        marcos.add(new MarcoDTO("ice", "Ice", null, "circulo", 0.0, true));
        marcos.add(new MarcoDTO("rose", "Rose", null, "circulo", 0.0, true));
        marcos.add(new MarcoDTO("cyber", "Cyber", null, "circulo", 0.0, true));
        marcos.add(new MarcoDTO("crystal", "Crystal", null, "circulo", 0.0, true));
        marcos.add(new MarcoDTO("double", "Double Gold", null, "circulo", 0.0, true));
        marcos.add(new MarcoDTO("star", "Star", null, "circulo", 0.0, true));
        marcos.add(new MarcoDTO("moon", "Moon", null, "circulo", 0.0, true));
        marcos.add(new MarcoDTO("sun", "Sun", null, "circulo", 0.0, true));
        marcos.add(new MarcoDTO("elite", "Elite", null, "circulo", 0.0, true));
        return marcos;
    }

    private List<FondoDTO> getFondosDefault() {
        List<FondoDTO> fondos = new ArrayList<>();
        fondos.add(new FondoDTO("default", "Default", null, "gradiente", "linear-gradient(135deg, #0d1117, #161b22)", 0.0, true));
        fondos.add(new FondoDTO("dark", "Dark", null, "gradiente", "linear-gradient(135deg, #1a1a2e, #0d1117)", 0.0, true));
        fondos.add(new FondoDTO("light", "Light", null, "gradiente", "linear-gradient(135deg, #ffffff, #f0f0f0)", 0.0, true));
        fondos.add(new FondoDTO("sunset", "Sunset", null, "gradiente", "linear-gradient(135deg, #ff6b6b, #feca57, #fd79a8)", 0.0, true));
        fondos.add(new FondoDTO("ocean", "Ocean", null, "gradiente", "linear-gradient(135deg, #00b894, #00cec9, #0984e3)", 0.0, true));
        fondos.add(new FondoDTO("aurora", "Aurora", null, "gradiente", "linear-gradient(135deg, #6c5ce7, #00b894, #fdcb6e)", 0.0, true));
        fondos.add(new FondoDTO("galaxy", "Galaxy", null, "gradiente", "linear-gradient(135deg, #2d3436, #6c5ce7, #fd79a8)", 0.0, true));
        fondos.add(new FondoDTO("lava", "Lava", null, "gradiente", "linear-gradient(135deg, #ff6b6b, #e17055, #d63031)", 0.0, true));
        fondos.add(new FondoDTO("forest", "Forest", null, "gradiente", "linear-gradient(135deg, #00b894, #55efc4, #00cec9)", 0.0, true));
        fondos.add(new FondoDTO("candy", "Candy", null, "gradiente", "linear-gradient(135deg, #fd79a8, #fdcb6e, #a29bfe)", 0.0, true));
        fondos.add(new FondoDTO("cyber", "Cyber", null, "gradiente", "linear-gradient(135deg, #00d4ff, #6c5ce7, #fd79a8)", 0.0, true));
        fondos.add(new FondoDTO("blood", "Blood", null, "gradiente", "linear-gradient(135deg, #ff0044, #d63031, #ff6b6b)", 0.0, true));
        fondos.add(new FondoDTO("royal", "Royal", null, "gradiente", "linear-gradient(135deg, #6c5ce7, #a29bfe, #fd79a8)", 0.0, true));
        fondos.add(new FondoDTO("gold", "Gold", null, "gradiente", "linear-gradient(135deg, #f9ca24, #fdcb6e, #feca57)", 0.0, true));
        fondos.add(new FondoDTO("pastel", "Pastel", null, "gradiente", "linear-gradient(135deg, #fd79a8, #a29bfe, #55efc4)", 0.0, true));
        return fondos;
    }

    private PersonalizacionUsuario crearPersonalizacionDefault(Long usuarioId) {
        log.info("Creando personalización default para usuario: {}", usuarioId);

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con ID: " + usuarioId));

        PersonalizacionUsuario personalizacion = PersonalizacionUsuario.builder()
                .usuario(usuario)
                .temaId("default")
                .marcoId("none")
                .fondoId("default")
                .build();

        return personalizacionRepository.save(personalizacion);
    }

    /**
     * ✅ toDTO: la foto SIEMPRE viene del USUARIO, no de la personalización.
     */
    private PersonalizacionDTO toDTO(PersonalizacionUsuario entity, Usuario usuario) {
        if (entity == null) {
            log.warn("Entity es null en toDTO");
            return null;
        }

        // ✅ La foto se toma del USUARIO (fuente de verdad)
        String fotoActual = usuario != null ? usuario.getFotoPerfilUrl() : null;

        return new PersonalizacionDTO(
                entity.getId(),
                entity.getUsuario() != null ? entity.getUsuario().getId() : null,
                entity.getTemaId() != null ? entity.getTemaId() : "default",
                entity.getMarcoId() != null ? entity.getMarcoId() : "none",
                entity.getFondoId() != null ? entity.getFondoId() : "default",
                fotoActual,   // ✅ Foto del usuario
                entity.getFotoPortadaUrl(),
                entity.getMarcoPersonalizadoUrl()
        );
    }
}