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
import org.springframework.scheduling.annotation.Scheduled;
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

    @Transactional(readOnly = true)
    @Cacheable(value = "usuarios", key = "#id")
    public UsuarioDTO obtenerUsuarioPorId(Long id) {
        log.info("📋 Obteniendo usuario por ID: {} (desde BD)", id);

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        return new UsuarioDTO(
                usuario.getId(),
                usuario.getNombreUsuario(),
                usuario.getEmail(),
                usuario.getFotoPerfilUrl()
        );
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "busquedaUsuarios", key = "#query")
    public List<UsuarioDisponibleDTO> buscarUsuarios(String query, Long usuarioActualId) {
        log.info("🔍 Buscando usuarios con: '{}' (desde BD)", query);

        if (query == null || query.trim().length() < 1) {
            return List.of();
        }

        List<Usuario> usuarios = usuarioRepository.findByNombreUsuarioContainingIgnoreCase(query.trim());

        return usuarios.stream()
                .filter(u -> !u.getId().equals(usuarioActualId))
                .map(u -> new UsuarioDisponibleDTO(
                        u.getId(),
                        u.getNombreUsuario(),
                        u.getFotoPerfilUrl()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = {"usuarios", "busquedaUsuarios"}, allEntries = true)
    public UsuarioDTO actualizarUsuario(Long id, UsuarioDTO datos) {
        log.info("✏️ Actualizando usuario: {}", id);

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
    // 🔥 ESTADO ONLINE / OFFLINE
    // ============================================================

    @Transactional
    public void marcarEnLinea(Long usuarioId) {
        log.info("🟢 Usuario {} marcado como EN LÍNEA", usuarioId);
        usuarioRepository.findById(usuarioId).ifPresent(u -> {
            u.setEnLinea(true);
            u.setUltimaConexion(Instant.now());
            usuarioRepository.save(u);
        });
    }

    @Transactional
    public void marcarDesconectado(Long usuarioId) {
        log.info("🔴 Usuario {} marcado como DESCONECTADO", usuarioId);
        usuarioRepository.findById(usuarioId).ifPresent(u -> {
            u.setEnLinea(false);
            u.setUltimaConexion(Instant.now());
            usuarioRepository.save(u);
        });
    }

    @Transactional
    public void marcarTodosDesconectados() {
        log.info("🔴 Marcando TODOS los usuarios como desconectados (arranque)");
        List<Usuario> todos = usuarioRepository.findAll();
        todos.forEach(u -> u.setEnLinea(false));
        usuarioRepository.saveAll(todos);
    }

    /**
     * 🧹 Limpieza automática: cada minuto marca como desconectados
     * a usuarios que llevan más de 5 minutos sin actividad.
     * Esto evita usuarios "fantasma" en línea por errores de red.
     */
    @Scheduled(fixedRate = 60000) // Cada 60 segundos
    @Transactional
    public void limpiarUsuariosInactivos() {
        Instant hace5Minutos = Instant.now().minusSeconds(300);
        List<Usuario> usuarios = usuarioRepository.findAll();

        List<Usuario> inactivos = usuarios.stream()
                .filter(u -> Boolean.TRUE.equals(u.getEnLinea()))
                .filter(u -> u.getUltimaConexion() != null && u.getUltimaConexion().isBefore(hace5Minutos))
                .collect(Collectors.toList());

        if (!inactivos.isEmpty()) {
            log.info("🧹 Limpiando {} usuarios inactivos", inactivos.size());
            inactivos.forEach(u -> u.setEnLinea(false));
            usuarioRepository.saveAll(inactivos);
        }
    }
}