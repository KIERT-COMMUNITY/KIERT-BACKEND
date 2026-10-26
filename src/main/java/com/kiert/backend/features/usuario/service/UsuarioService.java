// src/main/java/com/kiert/backend/service/UsuarioService.java
package com.kiert.backend.features.usuario.service;

import com.kiert.backend.features.usuario.dto.UsuarioDTO;
import com.kiert.backend.features.usuario.dto.UsuarioDisponibleDTO;
import com.kiert.backend.entity.Usuario;
import com.kiert.backend.exception.BadRequestException;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.features.usuario.repository.UsuarioRepository;
import com.kiert.backend.features.presencia.service.PresenciaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
    private static final int MAX_NOMBRE_USUARIO = 20;
    private static final int MAX_EMAIL = 100;
    private static final int MAX_BIO = 255;

    // ============================================================
    // OBTENER USUARIO POR ID
    // ============================================================
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_USUARIOS, key = "#id")
    public UsuarioDTO obtenerUsuarioPorId(Long id) {
        log.info("[DB] Obteniendo usuario por ID: {}", id);

        if (id == null) {
            throw new BadRequestException("ID requerido");
        }

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        return new UsuarioDTO(
                usuario.getId(),
                usuario.getNombreUsuario(),
                usuario.getEmail(),
                usuario.getFotoPerfilUrl(),
                usuario.getBio()
        );
    }

    // ============================================================
    // BUSCAR USUARIOS
    // ============================================================
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

        if (usuarioActualId == null) {
            return List.of();
        }

        String queryLimpia = query.trim()
                .replace("%", "")
                .replace("_", "");

        if (queryLimpia.isEmpty()) {
            return List.of();
        }

        List<Usuario> usuarios = usuarioRepository
                .findByNombreUsuarioContainingIgnoreCase(queryLimpia);

        return usuarios.stream()
                .filter(u -> !u.getId().equals(usuarioActualId))
                .filter(u -> Boolean.TRUE.equals(u.getActivo()))
                .filter(u -> !Boolean.TRUE.equals(u.getEliminado()))
                .limit(LIMITE_BUSQUEDA)
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

        if (id == null) {
            throw new BadRequestException("ID requerido");
        }
        if (datos == null) {
            throw new BadRequestException("Datos requeridos");
        }

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        if (datos.nombreUsuario() != null) {
            String nombre = datos.nombreUsuario().trim();
            if (nombre.isEmpty()) {
                throw new BadRequestException("El nombre de usuario no puede estar vacío");
            }
            if (nombre.length() > MAX_NOMBRE_USUARIO) {
                throw new BadRequestException("El nombre de usuario es demasiado largo");
            }
            if (nombre.length() < 3) {
                throw new BadRequestException("El nombre de usuario es demasiado corto");
            }
            if (!nombre.equals(usuario.getNombreUsuario())
                    && usuarioRepository.existsByNombreUsuario(nombre)) {
                throw new BadRequestException("El nombre de usuario ya está en uso");
            }
            usuario.setNombreUsuario(nombre);
        }

        if (datos.email() != null) {
            String email = datos.email().trim().toLowerCase();
            if (email.isEmpty()) {
                throw new BadRequestException("El email no puede estar vacío");
            }
            if (email.length() > MAX_EMAIL) {
                throw new BadRequestException("El email es demasiado largo");
            }
            if (!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                throw new BadRequestException("El email no es válido");
            }
            if (!email.equals(usuario.getEmail())
                    && usuarioRepository.existsByEmail(email)) {
                throw new BadRequestException("El email ya está en uso");
            }
            usuario.setEmail(email);
        }

        if (datos.bio() != null) {
            String bio = datos.bio().trim();
            if (bio.length() > MAX_BIO) {
                throw new BadRequestException("La biografía es demasiado larga");
            }
            usuario.setBio(bio.isEmpty() ? null : bio);
        }

        usuario = usuarioRepository.save(usuario);

        return new UsuarioDTO(
                usuario.getId(),
                usuario.getNombreUsuario(),
                usuario.getEmail(),
                usuario.getFotoPerfilUrl(),
                usuario.getBio()
        );
    }

    // ============================================================
    // ELIMINAR CUENTA (SOFT DELETE)
    // ============================================================
    @Transactional
    @CacheEvict(value = {CACHE_USUARIOS, CACHE_BUSQUEDA_USUARIOS}, allEntries = true)
    public void eliminarCuenta(Long id) {
        log.info("Eliminando cuenta (soft delete) del usuario: {}", id);

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        // Aplicamos el Soft Delete manualmente (por si acaso)
        usuario.setEliminado(true);
        usuario.setFechaEliminacion(Instant.now());
        usuario.setActivo(false); // Desactivar la cuenta
        usuario.setEnLinea(false); // Forzar offline

        // Opcional: Anonimizar datos para cumplir con RGPD
        // usuario.setEmail("deleted_" + usuario.getId() + "@anonimizado.com");
        // usuario.setNombreUsuario("UsuarioEliminado" + usuario.getId());

        usuarioRepository.save(usuario);
        log.info("Cuenta del usuario {} marcada como eliminada.", id);
    }

    // ============================================================
    // ESTADO ONLINE / OFFLINE
    // ============================================================
    public void marcarEnLinea(Long usuarioId) {
        presenciaService.marcarEnLinea(usuarioId);
    }

    public void marcarDesconectado(Long usuarioId) {
        presenciaService.marcarDesconectado(usuarioId);
    }

    public UsuarioDTO enriquecerConPresencia(UsuarioDTO dto) {
        if (dto == null) return null;
        return dto;
    }
}