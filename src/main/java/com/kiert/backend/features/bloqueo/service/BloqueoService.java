// src/main/java/com/kiert/backend/service/BloqueoService.java
package com.kiert.backend.features.bloqueo.service;

import com.kiert.backend.features.bloqueo.dto.BloqueoDTO;
import com.kiert.backend.features.bloqueo.dto.CrearBloqueoDTO;
import com.kiert.backend.features.bloqueo.dto.EstadoBloqueoDTO;
import com.kiert.backend.entity.Bloqueo;
import com.kiert.backend.entity.Usuario;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.features.bloqueo.repository.BloqueoRepository;
import com.kiert.backend.features.usuario.repository.UsuarioRepository;
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
    // BLOQUEAR
    // ============================================================
    @Transactional
    @CacheEvict(value = CACHE_BLOQUEOS, allEntries = true)
    public BloqueoDTO bloquear(Long usuarioBloqueadorId, CrearBloqueoDTO dto) {
        log.info("Usuario {} bloquea a {}", usuarioBloqueadorId, dto.usuarioBloqueadoId());

        // Validaciones básicas
        if (usuarioBloqueadorId == null) {
            throw new IllegalArgumentException("Usuario autenticado requerido");
        }
        if (dto.usuarioBloqueadoId() == null) {
            throw new IllegalArgumentException("ID del usuario a bloquear requerido");
        }
        if (usuarioBloqueadorId.equals(dto.usuarioBloqueadoId())) {
            throw new IllegalArgumentException("No puedes bloquearte a ti mismo");
        }

        Usuario bloqueador = usuarioRepository.findById(usuarioBloqueadorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        Usuario bloqueado = usuarioRepository.findById(dto.usuarioBloqueadoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario a bloquear no encontrado"));

        // Si ya existe un bloqueo activo, devolverlo (idempotencia)
        var existente = bloqueoRepository.findBloqueoActivo(
                usuarioBloqueadorId, dto.usuarioBloqueadoId());
        if (existente.isPresent()) {
            log.info("Ya existe bloqueo activo entre {} y {}",
                    usuarioBloqueadorId, dto.usuarioBloqueadoId());
            return mapearADTO(existente.get());
        }

        // Sanitizar motivo
        String motivo = dto.motivo() != null && !dto.motivo().isBlank()
                ? dto.motivo().trim()
                : "Sin motivo especificado";

        if (motivo.length() > 500) {
            motivo = motivo.substring(0, 500);
        }

        Bloqueo bloqueo = Bloqueo.builder()
                .usuarioBloqueador(bloqueador)
                .usuarioBloqueado(bloqueado)
                .tipo("BLOQUEO")
                .motivo(motivo)
                .fechaCreacion(Instant.now())
                .activo(true)
                .build();

        bloqueo = bloqueoRepository.save(bloqueo);
        log.info("Usuario bloqueado con ID: {}", bloqueo.getId());

        return mapearADTO(bloqueo);
    }

    // ============================================================
    // DESBLOQUEAR
    // ============================================================
    @Transactional
    @CacheEvict(value = CACHE_BLOQUEOS, allEntries = true)
    public void desbloquear(Long usuarioBloqueadorId, Long usuarioBloqueadoId) {
        log.info("Usuario {} desbloquea a {}", usuarioBloqueadorId, usuarioBloqueadoId);

        if (usuarioBloqueadorId == null || usuarioBloqueadoId == null) {
            throw new IllegalArgumentException("Parámetros inválidos");
        }

        Bloqueo bloqueo = bloqueoRepository
                .findBloqueoActivo(usuarioBloqueadorId, usuarioBloqueadoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe bloqueo activo entre estos usuarios"));

        bloqueo.setActivo(false);
        bloqueoRepository.save(bloqueo);

        log.info("Usuario desbloqueado correctamente");
    }

    // ============================================================
    // VERIFICAR ESTADO (direccional)
    // ============================================================
    @Transactional(readOnly = true)
    @Cacheable(
            value = CACHE_BLOQUEOS,
            key = "'estado:' + #usuarioActualId + ':' + #otroUsuarioId"
    )
    public EstadoBloqueoDTO verificarEstado(Long usuarioActualId, Long otroUsuarioId) {
        log.debug("Consultando estado de bloqueo: {} → {}", usuarioActualId, otroUsuarioId);

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

    // ============================================================
    // LISTAR BLOQUEADOS
    // ============================================================
    @Transactional(readOnly = true)
    @Cacheable(
            value = CACHE_BLOQUEOS,
            key = "'lista:' + #usuarioId"
    )
    public List<BloqueoDTO> listarBloqueados(Long usuarioId) {
        log.debug("Listando bloqueados de usuario {}", usuarioId);

        return bloqueoRepository.findBloqueosActivosDeUsuario(usuarioId)
                .stream()
                .map(this::mapearADTO)
                .toList();
    }

    // ============================================================
    // VERIFICAR SI HAY BLOQUEO BIDIRECCIONAL
    // ============================================================
    @Transactional(readOnly = true)
    @Cacheable(
            value = CACHE_BLOQUEOS,
            key = "'entre:' + T(java.lang.Math).min(#usuarioA, #usuarioB) + ':' + T(java.lang.Math).max(#usuarioA, #usuarioB)"
    )
    public boolean hayBloqueoEntre(Long usuarioA, Long usuarioB) {
        log.debug("Verificando bloqueo entre {} y {}", usuarioA, usuarioB);

        if (usuarioA == null || usuarioB == null) {
            return false;
        }

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