package com.kiert.backend.service;

import com.kiert.backend.dto.*;
import com.kiert.backend.entity.*;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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

    // ========== OBTENER PERSONALIZACIÓN ==========
    @Transactional
    // ✅ QUITAR CACHÉ TEMPORALMENTE PARA EVITAR ClassCastException
    // @Cacheable(value = "personalizacion", key = "#usuarioId", unless = "#result == null")
    public PersonalizacionDTO obtenerPersonalizacion(Long usuarioId) {
        log.info("📋 Obteniendo personalización para usuario: {} (desde BD)", usuarioId);

        if (usuarioId == null) {
            log.error("❌ usuarioId es null");
            throw new IllegalArgumentException("Usuario ID no puede ser null");
        }

        try {
            PersonalizacionUsuario personalizacion = personalizacionRepository.findByUsuarioId(usuarioId)
                    .orElseGet(() -> {
                        log.info("🆕 No existe personalización, creando default para usuario: {}", usuarioId);
                        return crearPersonalizacionDefault(usuarioId);
                    });

            log.info("✅ Personalización encontrada: ID={}, tema={}, marco={}, fondo={}",
                    personalizacion.getId(),
                    personalizacion.getTemaId(),
                    personalizacion.getMarcoId(),
                    personalizacion.getFondoId());

            return toDTO(personalizacion);
        } catch (Exception e) {
            log.error("❌ Error al obtener personalización: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ========== GUARDAR PERSONALIZACIÓN ==========
    @Transactional
    @CacheEvict(value = {"personalizacion", "perfil", "usuarios"}, allEntries = true)
    public PersonalizacionDTO guardarPersonalizacion(Long usuarioId, String temaId, String marcoId, String fondoId) {
        log.info("💾 Guardando personalización para usuario: {} con parámetros: tema={}, marco={}, fondo={}",
                usuarioId, temaId, marcoId, fondoId);

        if (usuarioId == null) {
            log.error("❌ usuarioId es null");
            throw new IllegalArgumentException("Usuario ID no puede ser null");
        }

        try {
            PersonalizacionUsuario personalizacion = personalizacionRepository.findByUsuarioId(usuarioId)
                    .orElseGet(() -> {
                        log.info("🆕 No existe personalización, creando nueva para usuario: {}", usuarioId);
                        return crearPersonalizacionDefault(usuarioId);
                    });

            // ✅ ACTUALIZAR SOLO SI LOS VALORES SON DIFERENTES
            if (temaId != null && !temaId.isEmpty() && !temaId.equals(personalizacion.getTemaId())) {
                log.info("📝 Actualizando tema: {} → {}", personalizacion.getTemaId(), temaId);
                personalizacion.setTemaId(temaId);
            }
            if (marcoId != null && !marcoId.isEmpty() && !marcoId.equals(personalizacion.getMarcoId())) {
                log.info("📝 Actualizando marco: {} → {}", personalizacion.getMarcoId(), marcoId);
                personalizacion.setMarcoId(marcoId);
            }
            if (fondoId != null && !fondoId.isEmpty() && !fondoId.equals(personalizacion.getFondoId())) {
                log.info("📝 Actualizando fondo: {} → {}", personalizacion.getFondoId(), fondoId);
                personalizacion.setFondoId(fondoId);
            }

            personalizacion = personalizacionRepository.save(personalizacion);
            log.info("✅ Personalización guardada para usuario: {} con ID: {}", usuarioId, personalizacion.getId());
            log.info("📌 Valores guardados: tema={}, marco={}, fondo={}",
                    personalizacion.getTemaId(), personalizacion.getMarcoId(), personalizacion.getFondoId());

            return toDTO(personalizacion);
        } catch (Exception e) {
            log.error("❌ Error al guardar personalización: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ========== SUBIR FOTO DE PERFIL ==========
    @Transactional
    @CacheEvict(value = {"personalizacion", "perfil", "usuarios"}, allEntries = true)
    public PersonalizacionDTO subirFotoPerfil(Long usuarioId, MultipartFile archivo) {
        log.info("📸 Subiendo foto de perfil para usuario: {}", usuarioId);

        String url = cloudinaryService.subirArchivo(archivo, "perfiles");

        PersonalizacionUsuario personalizacion = personalizacionRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> crearPersonalizacionDefault(usuarioId));

        personalizacion.setFotoPerfilUrl(url);
        personalizacion = personalizacionRepository.save(personalizacion);

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        usuario.setFotoPerfilUrl(url);
        usuarioRepository.save(usuario);

        return toDTO(personalizacion);
    }

    // ========== SUBIR FOTO DE PORTADA ==========
    @Transactional
    @CacheEvict(value = {"personalizacion", "perfil"}, allEntries = true)
    public PersonalizacionDTO subirFotoPortada(Long usuarioId, MultipartFile archivo) {
        log.info("📸 Subiendo foto de portada para usuario: {}", usuarioId);

        String url = cloudinaryService.subirArchivo(archivo, "portadas");

        PersonalizacionUsuario personalizacion = personalizacionRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> crearPersonalizacionDefault(usuarioId));

        personalizacion.setFotoPortadaUrl(url);
        personalizacion = personalizacionRepository.save(personalizacion);

        return toDTO(personalizacion);
    }

    // ========== OBTENER MARCOS ==========
    public List<MarcoDTO> obtenerMarcos(Long usuarioId) {
        log.info("📋 Obteniendo marcos para usuario: {}", usuarioId);
        return getMarcosDefault();
    }

    // ========== OBTENER FONDOS ==========
    public List<FondoDTO> obtenerFondos(Long usuarioId) {
        log.info("📋 Obteniendo fondos para usuario: {}", usuarioId);
        return getFondosDefault();
    }

    // ========== LIMPIAR CACHÉ ==========
    @CacheEvict(value = {"personalizacion", "perfil", "usuarios", "marcos", "fondos", "busquedaUsuarios"}, allEntries = true)
    public void limpiarCache() {
        log.info("🧹 Limpiando toda la caché de personalización");
    }

    // ========== MÉTODOS PRIVADOS ==========

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
        log.info("🆕 Creando personalización default para usuario: {}", usuarioId);

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> {
                    log.error("❌ Usuario no encontrado con ID: {}", usuarioId);
                    return new RecursoNoEncontradoException("Usuario no encontrado con ID: " + usuarioId);
                });

        PersonalizacionUsuario personalizacion = PersonalizacionUsuario.builder()
                .usuario(usuario)
                .temaId("default")
                .marcoId("none")
                .fondoId("default")
                .fotoPerfilUrl(usuario.getFotoPerfilUrl())
                .build();

        log.info("✅ Personalización default creada para usuario: {}", usuarioId);
        return personalizacionRepository.save(personalizacion);
    }

    private PersonalizacionDTO toDTO(PersonalizacionUsuario entity) {
        if (entity == null) {
            log.warn("⚠️ Entity es null en toDTO");
            return null;
        }

        return new PersonalizacionDTO(
                entity.getId(),
                entity.getUsuario() != null ? entity.getUsuario().getId() : null,
                entity.getTemaId() != null ? entity.getTemaId() : "default",
                entity.getMarcoId() != null ? entity.getMarcoId() : "none",
                entity.getFondoId() != null ? entity.getFondoId() : "default",
                entity.getFotoPerfilUrl(),
                entity.getFotoPortadaUrl(),
                entity.getMarcoPersonalizadoUrl()
        );
    }
}