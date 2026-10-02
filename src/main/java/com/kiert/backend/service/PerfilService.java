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
    private final StorageService storageService;

    // ============================================================
    // OBTENER PERFIL (lectura frecuente)
    // ============================================================
    @Transactional(readOnly = true)
    @Cacheable(value = "perfil", key = "#usuarioId")
    public UsuarioDTO obtenerPerfil(Long usuarioId) {
        log.info("[DB] Obteniendo perfil del usuario {}", usuarioId);
        Usuario usuario = buscar(usuarioId);
        return aDTO(usuario);
    }

    // ============================================================
    // ACTUALIZAR FOTO DE PERFIL (por URL)
    // ============================================================
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "perfil", key = "#usuarioId"),
            // También invalidar listas donde aparece el usuario
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
        usuario.setFotoPerfilUrl(urlFoto);
        usuario = usuarioRepository.save(usuario);
        return aDTO(usuario);
    }

    // ============================================================
    // SUBIR FOTO DE PERFIL (multipart)
    // ============================================================
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "perfil", key = "#usuarioId"),
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

        try {
            String urlFoto = storageService.subirArchivo(archivo);
            log.info("Foto de perfil subida para el usuario {}: {}", usuarioId, urlFoto);

            // NO llamar a actualizarFotoPerfil() internamente.
            // El @CacheEvict de este método ya se encarga de la invalidación.
            Usuario usuario = buscar(usuarioId);
            usuario.setFotoPerfilUrl(urlFoto);
            usuario = usuarioRepository.save(usuario);
            return aDTO(usuario);
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error al subir foto de perfil para el usuario {}: {}", usuarioId, ex.getMessage());
            throw new BadRequestException("No se pudo subir la imagen. Intenta nuevamente.");
        }
    }

    // ============================================================
    // LIMPIAR CACHÉ MANUALMENTE
    // ============================================================
    @CacheEvict(value = "perfil", key = "#usuarioId")
    public void eliminarCachePerfil(Long usuarioId) {
        log.info("Eliminando caché del usuario {}", usuarioId);
        // @CacheEvict ya hace el trabajo, no necesitas redisTemplate.delete()
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
                usuario.getFotoPerfilUrl()
        );
    }
}