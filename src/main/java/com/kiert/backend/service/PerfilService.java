// src/main/java/com/kiert/backend/service/PerfilService.java
package com.kiert.backend.service;

import com.kiert.backend.dto.UsuarioDTO;
import com.kiert.backend.entity.Usuario;
import com.kiert.backend.exception.BadRequestException;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class PerfilService {

    private static final long MAX_FOTO_PERFIL_BYTES = 5L * 1024 * 1024;

    private final UsuarioRepository usuarioRepository;
    private final CloudinaryService cloudinaryService;

    // ❌ ELIMINADO: PersonalizacionRepository (ya no se usa)
    // ✅ La foto vive SOLO en usuarios.foto_perfil_url

    // ============================================================
    // OBTENER PERFIL
    // ============================================================
    @Transactional(readOnly = true)
    @Cacheable(value = "perfil", key = "#usuarioId")
    public UsuarioDTO obtenerPerfil(Long usuarioId) {
        log.info("[DB] Obteniendo perfil del usuario {}", usuarioId);
        Usuario usuario = buscar(usuarioId);
        return aDTO(usuario);
    }

    // ============================================================
    // SUBIR FOTO DE PERFIL
    // ✅ Solo actualiza usuarios.foto_perfil_url
    // ============================================================
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "perfil", key = "#usuarioId"),
            @CacheEvict(value = "personalizacion", allEntries = true),
            @CacheEvict(value = "posts", allEntries = true),
            @CacheEvict(value = "post", allEntries = true),
            @CacheEvict(value = "comentarios", allEntries = true),
            @CacheEvict(value = "respuestas", allEntries = true),
            @CacheEvict(value = "documentos", allEntries = true),
            @CacheEvict(value = "documento", allEntries = true),
            @CacheEvict(value = "conversaciones", allEntries = true),
            @CacheEvict(value = "miembrosGrupo", allEntries = true),
            @CacheEvict(value = "gruposUsuario", allEntries = true)
    })
    public UsuarioDTO subirFotoPerfil(Long usuarioId, MultipartFile archivo) {
        log.info("Subiendo foto para usuario: {}", usuarioId);

        if (archivo == null || archivo.isEmpty()) {
            throw new BadRequestException("Debes enviar una imagen.");
        }
        if (archivo.getContentType() == null || !archivo.getContentType().startsWith("image/")) {
            throw new BadRequestException("El archivo debe ser una imagen.");
        }
        if (archivo.getSize() > MAX_FOTO_PERFIL_BYTES) {
            throw new BadRequestException("La imagen no debe superar los 5MB.");
        }

        Usuario usuario = buscar(usuarioId);
        String urlAnterior = usuario.getFotoPerfilUrl();

        try {
            // ✅ Subir nueva a Cloudinary
            String urlNueva = cloudinaryService.subirArchivo(archivo, "perfiles");
            log.info("Foto de perfil subida: {}", urlNueva);

            // ✅ Eliminar foto anterior de Cloudinary
            if (urlAnterior != null && !urlAnterior.isBlank()) {
                log.info("Eliminando foto anterior de Cloudinary: {}", urlAnterior);
                cloudinaryService.eliminarArchivoPorUrl(urlAnterior);
            }

            // ✅ Actualizar en usuario (ÚNICA fuente de verdad)
            usuario.setFotoPerfilUrl(urlNueva);
            usuario = usuarioRepository.save(usuario);

            return aDTO(usuario);

        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error al subir foto de perfil: {}", ex.getMessage(), ex);
            throw new BadRequestException("No se pudo subir la imagen. Intenta nuevamente.");
        }
    }

    // ============================================================
    // ACTUALIZAR FOTO POR URL
    // ============================================================
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "perfil", key = "#usuarioId"),
            @CacheEvict(value = "personalizacion", allEntries = true),
            @CacheEvict(value = "posts", allEntries = true),
            @CacheEvict(value = "post", allEntries = true),
            @CacheEvict(value = "comentarios", allEntries = true),
            @CacheEvict(value = "respuestas", allEntries = true),
            @CacheEvict(value = "documentos", allEntries = true),
            @CacheEvict(value = "documento", allEntries = true),
            @CacheEvict(value = "conversaciones", allEntries = true),
            @CacheEvict(value = "miembrosGrupo", allEntries = true),
            @CacheEvict(value = "gruposUsuario", allEntries = true)
    })
    public UsuarioDTO actualizarFotoPerfil(Long usuarioId, String urlFoto) {
        log.info("Actualizando foto de perfil del usuario {}: {}", usuarioId, urlFoto);

        Usuario usuario = buscar(usuarioId);
        String urlAnterior = usuario.getFotoPerfilUrl();

        // ✅ Eliminar la anterior de Cloudinary si cambia
        if (urlAnterior != null && !urlAnterior.isBlank() && !urlAnterior.equals(urlFoto)) {
            log.info("Eliminando foto anterior de Cloudinary: {}", urlAnterior);
            cloudinaryService.eliminarArchivoPorUrl(urlAnterior);
        }

        // ✅ Actualizar en usuario (ÚNICA fuente de verdad)
        usuario.setFotoPerfilUrl(urlFoto);
        usuario = usuarioRepository.save(usuario);

        return aDTO(usuario);
    }

    // ============================================================
    // ✅ ELIMINAR FOTO DE PERFIL
    // ✅ Solo actualiza usuarios.foto_perfil_url = null
    // ============================================================
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "perfil", key = "#usuarioId"),
            @CacheEvict(value = "personalizacion", allEntries = true),
            @CacheEvict(value = "posts", allEntries = true),
            @CacheEvict(value = "post", allEntries = true),
            @CacheEvict(value = "comentarios", allEntries = true),
            @CacheEvict(value = "respuestas", allEntries = true),
            @CacheEvict(value = "documentos", allEntries = true),
            @CacheEvict(value = "documento", allEntries = true),
            @CacheEvict(value = "conversaciones", allEntries = true),
            @CacheEvict(value = "miembrosGrupo", allEntries = true),
            @CacheEvict(value = "gruposUsuario", allEntries = true)
    })
    public UsuarioDTO eliminarFotoPerfil(Long usuarioId) {
        log.info("Eliminando foto de perfil del usuario {}", usuarioId);

        Usuario usuario = buscar(usuarioId);
        String urlAnterior = usuario.getFotoPerfilUrl();

        if (urlAnterior == null || urlAnterior.isBlank()) {
            log.warn("El usuario {} no tiene foto de perfil", usuarioId);
            return aDTO(usuario);
        }

        // ✅ Eliminar de Cloudinary
        log.info("Eliminando foto de Cloudinary: {}", urlAnterior);
        cloudinaryService.eliminarArchivoPorUrl(urlAnterior);

        // ✅ Poner null en usuario (ÚNICA fuente de verdad)
        usuario.setFotoPerfilUrl(null);
        usuario = usuarioRepository.save(usuario);

        log.info("✅ Foto de perfil eliminada para el usuario {}", usuarioId);
        return aDTO(usuario);
    }

    // ============================================================
    // LIMPIAR CACHÉ
    // ============================================================
    @CacheEvict(value = "perfil", key = "#usuarioId")
    public void eliminarCachePerfil(Long usuarioId) {
        log.info("Eliminando caché del usuario {}", usuarioId);
    }

    // ============================================================
    // HELPERS
    // ============================================================
    private Usuario buscar(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado."));
    }

    private UsuarioDTO aDTO(Usuario usuario) {
        return new UsuarioDTO(
                usuario.getId(),
                usuario.getNombreUsuario(),
                usuario.getEmail(),
                usuario.getFotoPerfilUrl(),
                usuario.getBio()
        );
    }
}