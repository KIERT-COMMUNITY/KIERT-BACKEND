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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class
PerfilService {

    private static final long MAX_FOTO_PERFIL_BYTES = 5L * 1024 * 1024;
    private static final String CACHE_KEY_USER = "perfil:";
    private static final long CACHE_TTL_MINUTES = 30;

    private final UsuarioRepository usuarioRepository;
    private final StorageService storageService;
    private final RedisTemplate<String, Object> redisTemplate;

    // ========== OBTENER PERFIL ==========
    @Transactional(readOnly = true)
    @Cacheable(value = "perfil", key = "#usuarioId")
    public UsuarioDTO obtenerPerfil(Long usuarioId) {
        log.info("📋 Obteniendo perfil del usuario {}", usuarioId);
        Usuario usuario = buscar(usuarioId);
        UsuarioDTO dto = aDTO(usuario);

        try {
            String cacheKey = CACHE_KEY_USER + usuarioId;
            redisTemplate.opsForValue().set(cacheKey, dto, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("⚠️ No se pudo guardar en Redis: {}", e.getMessage());
        }

        return dto;
    }

    // ========== ACTUALIZAR FOTO DE PERFIL (por URL) ==========
    @Transactional
    @CacheEvict(value = "perfil", key = "#usuarioId")
    public UsuarioDTO actualizarFotoPerfil(Long usuarioId, String urlFoto) {
        log.info("📸 Actualizando foto de perfil del usuario {}: {}", usuarioId, urlFoto);
        Usuario usuario = buscar(usuarioId);
        usuario.setFotoPerfilUrl(urlFoto);
        usuario = usuarioRepository.save(usuario);

        UsuarioDTO dto = aDTO(usuario);

        try {
            String cacheKey = CACHE_KEY_USER + usuarioId;
            redisTemplate.opsForValue().set(cacheKey, dto, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("⚠️ No se pudo guardar en Redis: {}", e.getMessage());
        }

        return dto;
    }

    // ========== SUBIR FOTO DE PERFIL (multipart) ==========
    @Transactional
    public UsuarioDTO subirFotoPerfil(Long usuarioId, MultipartFile archivo) {
        log.info("📸 Subiendo foto para usuario: {}", usuarioId);

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
            log.info("✅ Foto de perfil subida para el usuario {}: {}", usuarioId, urlFoto);
            return actualizarFotoPerfil(usuarioId, urlFoto);
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("❌ Error al subir foto de perfil para el usuario {}: {}", usuarioId, ex.getMessage());
            throw new BadRequestException("No se pudo subir la imagen. Intenta nuevamente.");
        }
    }

    // ========== LIMPIAR CACHÉ ==========
    @CacheEvict(value = "perfil", key = "#usuarioId")
    public void eliminarCachePerfil(Long usuarioId) {
        log.info("🧹 Eliminando caché del usuario {}", usuarioId);
        try {
            String cacheKey = CACHE_KEY_USER + usuarioId;
            redisTemplate.delete(cacheKey);
        } catch (Exception e) {
            log.warn("⚠️ No se pudo limpiar caché: {}", e.getMessage());
        }
    }

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