// src/main/java/com/kiert/backend/service/BloqueoService.java
package com.kiert.backend.service;

import com.kiert.backend.dto.BloqueoDTO;
import com.kiert.backend.dto.CrearBloqueoDTO;
import com.kiert.backend.dto.EstadoBloqueoDTO;
import com.kiert.backend.entity.Bloqueo;
import com.kiert.backend.entity.Usuario;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.repository.BloqueoRepository;
import com.kiert.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BloqueoService {

    private final BloqueoRepository bloqueoRepository;
    private final UsuarioRepository usuarioRepository;

    private static final String CACHE_BLOQUEOS = "bloqueos";

    // ============================================================
    // ESCRITURA — invalida caché
    // ============================================================

    /**
     * Bloquear a un usuario
     */
    @Transactional
    @CacheEvict(value = CACHE_BLOQUEOS, allEntries = true)
    public BloqueoDTO bloquear(Long usuarioBloqueadorId, CrearBloqueoDTO dto) {
        log.info("🚫 Usuario {} bloquea a {}", usuarioBloqueadorId, dto.usuarioBloqueadoId());

        if (usuarioBloqueadorId.equals(dto.usuarioBloqueadoId())) {
            throw new IllegalArgumentException("No puedes bloquearte a ti mismo");
        }

        Usuario bloqueador = usuarioRepository.findById(usuarioBloqueadorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        Usuario bloqueado = usuarioRepository.findById(dto.usuarioBloqueadoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario a bloquear no encontrado"));

        // Si ya existe un bloqueo activo, devolverlo
        var existente = bloqueoRepository.findBloqueoActivo(
                usuarioBloqueadorId, dto.usuarioBloqueadoId());
        if (existente.isPresent()) {
            log.info("ℹ️ Ya existe bloqueo activo");
            return mapearADTO(existente.get());
        }

        Bloqueo bloqueo = Bloqueo.builder()
                .usuarioBloqueador(bloqueador)
                .usuarioBloqueado(bloqueado)
                .tipo("BLOQUEO")
                .motivo(dto.motivo() != null && !dto.motivo().isBlank()
                        ? dto.motivo()
                        : "Sin motivo especificado")
                .fechaCreacion(Instant.now())
                .activo(true)
                .build();

        bloqueo = bloqueoRepository.save(bloqueo);
        log.info("✅ Usuario bloqueado con ID: {}", bloqueo.getId());

        return mapearADTO(bloqueo);
    }

    /**
     * Desbloquear a un usuario
     */
    @Transactional
    @CacheEvict(value = CACHE_BLOQUEOS, allEntries = true)
    public void desbloquear(Long usuarioBloqueadorId, Long usuarioBloqueadoId) {
        log.info("🔓 Usuario {} desbloquea a {}", usuarioBloqueadorId, usuarioBloqueadoId);

        Bloqueo bloqueo = bloqueoRepository
                .findBloqueoActivo(usuarioBloqueadorId, usuarioBloqueadoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe bloqueo activo"));

        bloqueo.setActivo(false);
        bloqueoRepository.save(bloqueo);

        log.info("✅ Usuario desbloqueado correctamente");
    }

    // ============================================================
    // LECTURA — cacheada
    // ============================================================

    /**
     * Verificar si un usuario está bloqueado por el actual
     * Clave direccional: (A bloquea a B) ≠ (B bloquea a A)
     */
    @Transactional(readOnly = true)
    @Cacheable(
            value = CACHE_BLOQUEOS,
            key = "'estado:' + #usuarioActualId + ':' + #otroUsuarioId"
    )
    public EstadoBloqueoDTO verificarEstado(Long usuarioActualId, Long otroUsuarioId) {
        log.debug("🔍 Consultando estado de bloqueo: {} → {}", usuarioActualId, otroUsuarioId);

        var bloqueo = bloqueoRepository.findBloqueoActivo(usuarioActualId, otroUsuarioId);
        if (bloqueo.isPresent()) {
            Bloqueo b = bloqueo.get();
            return new EstadoBloqueoDTO(
                    true,
                    b.getId(),
                    b.getMotivo(),
                    b.getFechaCreacion().toString()
            );
        }
        return new EstadoBloqueoDTO(false, null, null, null);
    }

    /**
     * Listar todos los usuarios bloqueados por el actual
     */
    @Transactional(readOnly = true)
    @Cacheable(
            value = CACHE_BLOQUEOS,
            key = "'lista:' + #usuarioId"
    )
    public List<BloqueoDTO> listarBloqueados(Long usuarioId) {
        log.debug("🔍 Listando bloqueados de usuario {}", usuarioId);

        return bloqueoRepository.findBloqueosActivosDeUsuario(usuarioId)
                .stream()
                .map(this::mapearADTO)
                .toList();
    }

    /**
     * Verificar si hay bloqueo entre dos usuarios (en cualquier dirección)
     * 🔑 Clave normalizada con min/max para que (A,B) y (B,A) compartan caché
     */
    @Transactional(readOnly = true)
    @Cacheable(
            value = CACHE_BLOQUEOS,
            key = "'entre:' + T(java.lang.Math).min(#usuarioA, #usuarioB) + ':' + T(java.lang.Math).max(#usuarioA, #usuarioB)"
    )
    public boolean hayBloqueoEntre(Long usuarioA, Long usuarioB) {
        log.debug("🔍 Verificando bloqueo entre {} y {}", usuarioA, usuarioB);

        return bloqueoRepository.existeBloqueoEntre(usuarioA, usuarioB);
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private BloqueoDTO mapearADTO(Bloqueo b) {
        return new BloqueoDTO(
                b.getId(),
                b.getUsuarioBloqueador() != null ? b.getUsuarioBloqueador().getId() : null,
                b.getUsuarioBloqueador() != null ? b.getUsuarioBloqueador().getNombreUsuario() : null,
                b.getUsuarioBloqueado() != null ? b.getUsuarioBloqueado().getId() : null,
                b.getUsuarioBloqueado() != null ? b.getUsuarioBloqueado().getNombreUsuario() : null,
                b.getTipo(),
                b.getMotivo(),
                b.getFechaCreacion(),
                b.isActivo()
        );
    }
}