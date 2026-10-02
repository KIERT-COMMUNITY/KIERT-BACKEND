// src/main/java/com/kiert/backend/service/UsuarioService.java
package com.kiert.backend.service;

import com.kiert.backend.dto.UsuarioDTO;
import com.kiert.backend.dto.UsuarioDisponibleDTO;
import com.kiert.backend.entity.Usuario;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PresenciaService presenciaService;

    private static final String CACHE_USUARIOS = "usuarios";
    private static final String CACHE_BUSQUEDA_USUARIOS = "busquedaUsuarios";
    private static final int LIMITE_BUSQUEDA = 20;

    // ============================================================
    // OBTENER USUARIO POR ID
    // ============================================================
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_USUARIOS, key = "#id")
    public UsuarioDTO obtenerUsuarioPorId(Long id) {
        log.info("[DB] Obteniendo usuario por ID: {}", id);

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        return new UsuarioDTO(
                usuario.getId(),
                usuario.getNombreUsuario(),
                usuario.getEmail(),
                usuario.getFotoPerfilUrl()
        );
    }

    // ============================================================
    // BUSCAR USUARIOS
    // ============================================================
    /**
     * CAMBIO CLAVE: la clave incluye `usuarioActualId` porque el resultado
     * filtra al usuario actual. Sin esto, dos usuarios distintos verían
     * los mismos resultados cacheados.
     *
     * Además, se limita a LIMITE_BUSQUEDA resultados para no llenar Redis.
     */
    @Transactional(readOnly = true)
    @Cacheable(
            value = CACHE_BUSQUEDA_USUARIOS,
            key = "#usuarioActualId + ':' + #query.toLowerCase().trim()",
            condition = "#query != null && #query.trim().length() >= 3"
    )
    public List<UsuarioDisponibleDTO> buscarUsuarios(String query, Long usuarioActualId) {
        log.info("[DB] Buscando usuarios con: '{}' (usuario: {})", query, usuarioActualId);

        if (query == null || query.trim().length() < 1) {
            return List.of();
        }

        List<Usuario> usuarios = usuarioRepository
                .findByNombreUsuarioContainingIgnoreCase(query.trim());

        return usuarios.stream()
                .filter(u -> !u.getId().equals(usuarioActualId))
                .limit(LIMITE_BUSQUEDA)   // límite
                .map(u -> new UsuarioDisponibleDTO(
                        u.getId(),
                        u.getNombreUsuario(),
                        u.getFotoPerfilUrl()
                ))
                .collect(Collectors.toList());
    }

    // ============================================================
    // ACTUALIZAR USUARIO
    // ============================================================
    @Transactional
    @CacheEvict(value = {CACHE_USUARIOS, CACHE_BUSQUEDA_USUARIOS}, allEntries = true)
    public UsuarioDTO actualizarUsuario(Long id, UsuarioDTO datos) {
        log.info("Actualizando usuario: {}", id);

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        if (datos.nombreUsuario() != null) {
            usuario.setNombreUsuario(datos.nombreUsuario());
        }
        if (datos.email() != null) {
            usuario.setEmail(datos.email());
        }

        usuario = usuarioRepository.save(usuario);

        return new UsuarioDTO(
                usuario.getId(),
                usuario.getNombreUsuario(),
                usuario.getEmail(),
                usuario.getFotoPerfilUrl()
        );
    }

    // ============================================================
    // ESTADO ONLINE / OFFLINE (ahora en Redis)
    // ============================================================
    public void marcarEnLinea(Long usuarioId) {
        presenciaService.marcarEnLinea(usuarioId);
    }

    public void marcarDesconectado(Long usuarioId) {
        presenciaService.marcarDesconectado(usuarioId);
    }

    /**
     * ELIMINADO: marcarTodosDesconectados()
     * Ya no se necesita porque las claves Redis expiran solas.
     * Si quieres forzar limpieza al arranque, hazlo desde un CommandLineRunner
     * que borre las claves presencia:online:* (no necesario).
     */

    /**
     * ELIMINADO: limpiarUsuariosInactivos()
     * Ya no se necesita porque el TTL de 5 min en Redis se encarga.
     */

    // ============================================================
    // HELPERS PARA EL DTO
    // ============================================================
    /**
     * Rellena el estado online/ultima conexión en un DTO.
     * Llamar desde el mapper o controller cuando se necesite.
     */
    public UsuarioDTO enriquecerConPresencia(UsuarioDTO dto) {
        if (dto == null) return null;
        // El DTO es un record, así que creamos uno nuevo con los datos
        // (o lo dejamos así si el frontend consulta el estado por separado)
        return dto;
    }
}