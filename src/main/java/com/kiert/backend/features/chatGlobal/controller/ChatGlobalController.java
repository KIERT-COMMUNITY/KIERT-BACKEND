// src/main/java/com/kiert/backend/features/chatGlobal/controller/ChatGlobalController.java
package com.kiert.backend.features.chatGlobal.controller;

import com.kiert.backend.features.chatGlobal.dto.EnviarMensajeGlobalDTO;
import com.kiert.backend.features.chatGlobal.dto.MensajeGlobalDTO;
import com.kiert.backend.entity.MensajeGlobal;
import com.kiert.backend.entity.Usuario;
import com.kiert.backend.exception.BadRequestException;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.features.chatGlobal.repository.MensajeGlobalRepository;
import com.kiert.backend.features.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.Instant;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/chat-global")
@RequiredArgsConstructor
public class ChatGlobalController {

    private static final int PAGE_SIZE_MAX = 100;
    private static final int PAGE_MAX = 1000;
    private static final int CONTENIDO_MAX_LENGTH = 500;

    private final MensajeGlobalRepository mensajeRepository;
    private final UsuarioRepository usuarioRepository;

    // ============================================================
    // REST: historial (público)
    // ============================================================
    @GetMapping("/mensajes")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<MensajeGlobalDTO>> obtenerMensajes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        if (page < 0 || page > PAGE_MAX) {
            return ResponseEntity.badRequest().build();
        }
        int sizeSeguro = Math.min(Math.max(size, 1), PAGE_SIZE_MAX);

        log.debug("[CHAT-GLOBAL] Historial solicitado (page={}, size={})", page, sizeSeguro);

        Pageable pageable = PageRequest.of(page, sizeSeguro);
        Page<MensajeGlobal> mensajes = mensajeRepository.findUltimosConAutor(pageable);

        List<MensajeGlobalDTO> dtos = mensajes.getContent().stream()
                .map(this::mapearADTO)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    // ============================================================
    // STOMP: enviar mensaje
    // ============================================================
    @MessageMapping("/chat-global/enviar")
    @SendTo("/topic/chat-global")
    public MensajeGlobalDTO enviarMensaje(
            @Payload EnviarMensajeGlobalDTO dto,
            Principal principal) {

        if (principal == null) {
            throw new BadRequestException("Usuario no autenticado en el chat");
        }

        Long usuarioId;
        try {
            usuarioId = Long.parseLong(principal.getName());
        } catch (NumberFormatException e) {
            throw new BadRequestException("Sesión inválida");
        }

        if (dto == null || dto.contenido() == null || dto.contenido().trim().isEmpty()) {
            throw new BadRequestException("El mensaje no puede estar vacío");
        }
        String contenido = dto.contenido().trim();
        if (contenido.length() > CONTENIDO_MAX_LENGTH) {
            throw new BadRequestException("El mensaje es demasiado largo (máx. "
                    + CONTENIDO_MAX_LENGTH + " caracteres)");
        }
        contenido = contenido.replaceAll("[\\p{Cntrl}&&[^\\n\\t]]", "");

        // Validar respuestaAId (si se envía). Si no existe, se ignora.
        Long respuestaValida = null;
        if (dto.respuestaAId() != null && mensajeRepository.existsById(dto.respuestaAId())) {
            respuestaValida = dto.respuestaAId();
        }

        Usuario autor = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        MensajeGlobal mensaje = MensajeGlobal.builder()
                .autor(autor)
                .contenido(contenido)
                .fechaEnvio(Instant.now())
                .respuestaAId(respuestaValida)
                .build();

        mensaje = mensajeRepository.save(mensaje);

        log.info("[CHAT-GLOBAL] Mensaje id={} de usuarioId={} respuestaAId={}",
                mensaje.getId(), autor.getId(), respuestaValida);

        // Recargar con LEFT JOIN FETCH para que el mapper tenga la personalización lista
        MensajeGlobal recargado = mensajeRepository.findByIdConAutor(mensaje.getId())
                .orElse(mensaje);

        return mapearADTO(recargado);
    }

    // ============================================================
    // MAPPER CORREGIDO
    // ============================================================
    /**
     * ✅ CORREGIDO: resuelve `marcoId` desde `Personalizacion` (relación principal),
     * con fallback al campo desnormalizado `Usuario.marcoId`.
     *
     * Antes se leía SOLO `autor.getMarcoId()` (campo desnormalizado), que suele estar
     * null aunque la personalización real tenga marco. Ese era el bug.
     */
    private MensajeGlobalDTO mapearADTO(MensajeGlobal m) {
        if (m == null) return null;

        Usuario autor = m.getAutor();
        Long autorId = autor != null ? autor.getId() : null;
        String nombre = autor != null && autor.getNombreUsuario() != null
                ? autor.getNombreUsuario()
                : "Usuario";
        String foto = autor != null ? autor.getFotoPerfilUrl() : null;
        String marcoId = resolverMarcoId(autor);

        return new MensajeGlobalDTO(
                m.getId(),
                autorId,
                nombre,
                foto,
                marcoId,
                m.getContenido(),
                m.getFechaEnvio(),
                m.getRespuestaAId()
        );
    }

    /**
     * Resuelve el marco del autor con triple fallback:
     *   1) Personalización (relación @OneToOne con marcoId real)
     *   2) Campo desnormalizado `usuario.marcoId`
     *   3) "none"
     */
    private String resolverMarcoId(Usuario autor) {
        if (autor == null) return "none";

        String marcoId = null;

        // 1) Intentar desde la relación Personalizacion
        try {
            if (autor.getPersonalizacion() != null
                    && Hibernate.isInitialized(autor.getPersonalizacion())) {
                String raw = autor.getPersonalizacion().getMarcoId();
                if (raw != null && !raw.isBlank()) {
                    marcoId = raw.trim().toLowerCase();
                }
            }
        } catch (Exception e) {
            log.debug("No se pudo leer personalizacion del usuario {}: {}",
                    autor.getId(), e.getMessage());
        }

        // 2) Fallback al campo desnormalizado
        if (marcoId == null) {
            String raw = autor.getMarcoId();
            if (raw != null && !raw.isBlank()) {
                marcoId = raw.trim().toLowerCase();
            }
        }

        // 3) Default
        return marcoId != null ? marcoId : "none";
    }
}