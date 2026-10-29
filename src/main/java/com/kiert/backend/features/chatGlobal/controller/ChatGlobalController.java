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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

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

    @MessageMapping("/chat-global.enviar")
    @SendTo("/topic/chat-global")
    public MensajeGlobalDTO enviarMensaje(
            @Payload EnviarMensajeGlobalDTO dto,
            SimpMessageHeaderAccessor headerAccessor) {

        Map<String, Object> attrs = headerAccessor.getSessionAttributes();
        String usuarioIdStr = attrs != null ? (String) attrs.get("usuarioId") : null;
        if (usuarioIdStr == null) {
            throw new BadRequestException("Usuario no autenticado en el chat");
        }

        Long usuarioId;
        try {
            usuarioId = Long.parseLong(usuarioIdStr);
        } catch (NumberFormatException e) {
            throw new BadRequestException("Sesion invalida");
        }

        if (dto == null || dto.contenido() == null || dto.contenido().trim().isEmpty()) {
            throw new BadRequestException("El mensaje no puede estar vacio");
        }
        String contenido = dto.contenido().trim();
        if (contenido.length() > CONTENIDO_MAX_LENGTH) {
            throw new BadRequestException("El mensaje es demasiado largo (max. "
                    + CONTENIDO_MAX_LENGTH + " caracteres)");
        }
        contenido = contenido.replaceAll("[\\p{Cntrl}&&[^\\n\\t]]", "");

        Usuario autor = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        MensajeGlobal mensaje = MensajeGlobal.builder()
                .autor(autor)
                .contenido(contenido)
                .fechaEnvio(Instant.now())
                .build();

        mensaje = mensajeRepository.save(mensaje);

        log.info("[CHAT-GLOBAL] Mensaje id={} de usuarioId={}", mensaje.getId(), autor.getId());

        return mapearADTO(mensaje);
    }

    private MensajeGlobalDTO mapearADTO(MensajeGlobal m) {
        if (m == null) return null;

        Usuario autor = m.getAutor();
        Long autorId = autor != null ? autor.getId() : null;
        String nombre = autor != null ? autor.getNombreUsuario() : "Usuario";
        String foto = autor != null ? autor.getFotoPerfilUrl() : null;
        String marcoId = autor != null ? autor.getMarcoId() : "none";

        return new MensajeGlobalDTO(
                m.getId(),
                autorId,
                nombre,
                foto,
                marcoId,
                m.getContenido(),
                m.getFechaEnvio()
        );
    }
}