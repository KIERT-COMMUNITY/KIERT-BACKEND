// src/main/java/com/kiert/backend/features/personalizacion/service/PersonalizacionService.java
package com.kiert.backend.features.personalizacion.service;

import com.kiert.backend.entity.*;
import com.kiert.backend.exception.BadRequestException;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.features.fondoYmarco.dto.FondoDTO;
import com.kiert.backend.features.fondoYmarco.dto.MarcoDTO;
import com.kiert.backend.features.personalizacion.dto.PersonalizacionDTO;
import com.kiert.backend.features.personalizacion.repository.PersonalizacionRepository;
import com.kiert.backend.features.usuario.repository.UsuarioRepository;
import com.kiert.backend.features.cloudinary.service.CloudinaryService;
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
import java.util.Set;

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

    private static final Set<String> TEMAS_VALIDOS = Set.of(
            "default", "dark", "light", "sunset", "ocean", "aurora", "galaxy",
            "lava", "forest", "candy", "cyber", "blood", "royal", "gold", "pastel"
    );

    private static final Set<String> MARCOS_VALIDOS = Set.of(
            "none", "classic", "gold", "silver", "rainbow", "pastel", "neon",
            "ocean", "sunset", "galaxy", "fire", "ice", "rose", "cyber",
            "crystal", "double", "star", "moon", "sun", "elite"
    );

    private static final Set<String> FONDOS_VALIDOS = Set.of(
            "default", "dark", "light", "sunset", "ocean", "aurora", "galaxy",
            "lava", "forest", "candy", "cyber", "blood", "royal", "gold", "pastel"
    );

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;

    // ============================================================
    // OBTENER PERSONALIZACION (crea default si no existe)
    // ============================================================
    @Transactional
    public PersonalizacionDTO obtenerPersonalizacion(Long usuarioId) {
        log.debug("[DB] Obteniendo personalizacion para usuario: {}", usuarioId);

        if (usuarioId == null) {
            throw new BadRequestException("Usuario ID requerido");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        PersonalizacionUsuario personalizacion = personalizacionRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> crearPersonalizacionDefault(usuarioId));

        return toDTO(personalizacion, usuario);
    }

    // ============================================================
    // OBTENER (cacheable)
    // ============================================================
    @Transactional
    @Cacheable(
            value = CACHE_PERSONALIZACION,
            key = "#usuarioId",
            unless = "#result == null || #result.id() == null"
    )
    public PersonalizacionDTO obtenerOCrearPersonalizacion(Long usuarioId) {
        log.debug("[DB] obtenerOCrearPersonalizacion para usuario: {}", usuarioId);

        if (usuarioId == null) {
            throw new BadRequestException("Usuario ID requerido");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        PersonalizacionUsuario personalizacion = personalizacionRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> crearPersonalizacionDefault(usuarioId));

        return toDTO(personalizacion, usuario);
    }

    // ============================================================
    // OBTENER PERSONALIZACION POR USUARIO (publico autenticado)
    // ============================================================
    @Transactional
    @Cacheable(
            value = CACHE_PERSONALIZACION,
            key = "'user:' + #usuarioId",
            unless = "#result == null || #result.id() == null"
    )
    public PersonalizacionDTO obtenerPersonalizacionPorUsuario(Long usuarioId) {
        log.debug("[DB] Obteniendo personalizacion de usuario {} (publico)", usuarioId);
        return obtenerOCrearPersonalizacion(usuarioId);
    }

    // ============================================================
    // GUARDAR PERSONALIZACION
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
        log.info("Guardando personalizacion para usuario: {}", usuarioId);

        if (usuarioId == null) {
            throw new BadRequestException("Usuario ID requerido");
        }

        if (temaId != null && !temaId.isEmpty() && !TEMAS_VALIDOS.contains(temaId)) {
            throw new BadRequestException("Tema no valido");
        }
        if (marcoId != null && !marcoId.isEmpty() && !MARCOS_VALIDOS.contains(marcoId)) {
            throw new BadRequestException("Marco no valido");
        }
        if (fondoId != null && !fondoId.isEmpty() && !FONDOS_VALIDOS.contains(fondoId)) {
            throw new BadRequestException("Fondo no valido");
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

        if (usuarioId == null) {
            throw new BadRequestException("Usuario ID requerido");
        }
        if (archivo == null || archivo.isEmpty()) {
            throw new BadRequestException("El archivo es obligatorio");
        }

        String contentType = archivo.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("Solo se permiten imagenes");
        }
        if (archivo.getSize() > MAX_IMAGE_SIZE) {
            throw new BadRequestException("La imagen no debe superar 5MB");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        String urlAnterior = usuario.getFotoPerfilUrl();

        String url;
        try {
            url = cloudinaryService.subirImagen(archivo, "perfiles");
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Error al subir foto: {}", e.getMessage());
            throw new BadRequestException("No se pudo subir la foto");
        }

        if (urlAnterior != null && !urlAnterior.isBlank()) {
            try {
                cloudinaryService.eliminarArchivoPorUrl(urlAnterior);
            } catch (Exception e) {
                log.warn("No se pudo eliminar foto anterior: {}", e.getMessage());
            }
        }

        usuario.setFotoPerfilUrl(url);
        usuarioRepository.save(usuario);

        PersonalizacionUsuario personalizacion = personalizacionRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> crearPersonalizacionDefault(usuarioId));

        return toDTO(personalizacion, usuario);
    }

    // ============================================================
    // ELIMINAR FOTO DE PERFIL
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

        if (usuarioId == null) {
            throw new BadRequestException("Usuario ID requerido");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        String urlAnterior = usuario.getFotoPerfilUrl();

        if (urlAnterior != null && !urlAnterior.isBlank()) {
            try {
                cloudinaryService.eliminarArchivoPorUrl(urlAnterior);
            } catch (Exception e) {
                log.warn("No se pudo eliminar foto: {}", e.getMessage());
            }
        }

        usuario.setFotoPerfilUrl(null);
        usuarioRepository.save(usuario);

        PersonalizacionUsuario personalizacion = personalizacionRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> crearPersonalizacionDefault(usuarioId));

        log.info("Foto de perfil eliminada para el usuario {}", usuarioId);
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

        if (usuarioId == null) {
            throw new BadRequestException("Usuario ID requerido");
        }
        if (archivo == null || archivo.isEmpty()) {
            throw new BadRequestException("El archivo es obligatorio");
        }

        String contentType = archivo.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("Solo se permiten imagenes");
        }
        if (archivo.getSize() > MAX_IMAGE_SIZE) {
            throw new BadRequestException("La imagen no debe superar 5MB");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        PersonalizacionUsuario personalizacion = personalizacionRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> crearPersonalizacionDefault(usuarioId));

        String urlAnterior = personalizacion.getFotoPortadaUrl();

        String url;
        try {
            url = cloudinaryService.subirImagen(archivo, "portadas");
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Error al subir portada: {}", e.getMessage());
            throw new BadRequestException("No se pudo subir la portada");
        }

        if (urlAnterior != null && !urlAnterior.isBlank()) {
            try {
                cloudinaryService.eliminarArchivoPorUrl(urlAnterior);
            } catch (Exception e) {
                log.warn("No se pudo eliminar portada anterior: {}", e.getMessage());
            }
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
    // LIMPIAR CACHE
    // ============================================================
    @Caching(evict = {
            @CacheEvict(value = CACHE_PERSONALIZACION, allEntries = true),
            @CacheEvict(value = CACHE_MARCOS, allEntries = true),
            @CacheEvict(value = CACHE_FONDOS, allEntries = true),
            @CacheEvict(value = "perfil", allEntries = true)
    })
    public void limpiarCache() {
        log.info("Limpiando cache de personalizacion");
    }

    // ============================================================
    // METODOS PRIVADOS
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
        log.info("Creando personalizacion default para usuario: {}", usuarioId);

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

    private PersonalizacionDTO toDTO(PersonalizacionUsuario entity, Usuario usuario) {
        if (entity == null) return null;

        String fotoActual = usuario != null ? usuario.getFotoPerfilUrl() : null;

        return new PersonalizacionDTO(
                entity.getId(),
                entity.getUsuario() != null ? entity.getUsuario().getId() : null,
                entity.getTemaId() != null ? entity.getTemaId() : "default",
                entity.getMarcoId() != null ? entity.getMarcoId() : "none",
                entity.getFondoId() != null ? entity.getFondoId() : "default",
                fotoActual,
                entity.getFotoPortadaUrl(),
                entity.getMarcoPersonalizadoUrl()
        );
    }
}