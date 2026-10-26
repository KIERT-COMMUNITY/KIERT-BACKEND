// src/main/java/com/kiert/backend/controller/ChatGlobalController.java
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
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/chat-global")
@RequiredArgsConstructor
public class ChatGlobalController {

    private final MensajeGlobalRepository mensajeRepository;
    private final UsuarioRepository usuarioRepository;

    // ============================================================
    // REST: historial paginado (público — permite invitados)
    // ============================================================
    @GetMapping("/mensajes")
    public ResponseEntity<List<MensajeGlobalDTO>> obtenerMensajes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        log.info("[CHAT-GLOBAL] Historial solicitado (page={}, size={})", page, size);

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<MensajeGlobal> mensajes = mensajeRepository.findUltimosConAutor(pageable);

        List<MensajeGlobalDTO> dtos = mensajes.getContent().stream()
                .map(this::mapearADTO)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    // ============================================================
    // STOMP: enviar mensaje (requiere usuario autenticado)
    // ============================================================
    @MessageMapping("/chat-global.enviar")
    @SendTo("/topic/chat-global")
    public MensajeGlobalDTO enviarMensaje(
            @Payload EnviarMensajeGlobalDTO dto,
            SimpMessageHeaderAccessor headerAccessor) {

        // ✅ Requiere que el usuario esté autenticado en la sesión STOMP
        String usuarioIdStr = (String) headerAccessor.getSessionAttributes().get("usuarioId");
        if (usuarioIdStr == null) {
            throw new BadRequestException("Usuario no autenticado en el chat");
        }

        Long usuarioId = Long.parseLong(usuarioIdStr);

        if (dto.contenido() == null || dto.contenido().trim().isEmpty()) {
            throw new BadRequestException("El mensaje no puede estar vacío");
        }
        if (dto.contenido().length() > 500) {
            throw new BadRequestException("El mensaje es demasiado largo (máx. 500 caracteres)");
        }

        Usuario autor = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        MensajeGlobal mensaje = MensajeGlobal.builder()
                .autor(autor)
                .contenido(dto.contenido().trim())
                .fechaEnvio(Instant.now())
                .build();

        mensaje = mensajeRepository.save(mensaje);

        log.info("[CHAT-GLOBAL] {}: {}", autor.getNombreUsuario(), mensaje.getContenido());

        return mapearADTO(mensaje);
    }

    // ============================================================
    // DTO MAPPING
    // ============================================================
    private MensajeGlobalDTO mapearADTO(MensajeGlobal m) {
        return new MensajeGlobalDTO(
                m.getId(),
                m.getAutor().getId(),
                m.getAutor().getNombreUsuario(),
                m.getAutor().getFotoPerfilUrl(),
                m.getContenido(),
                m.getFechaEnvio()
        );
    }
}