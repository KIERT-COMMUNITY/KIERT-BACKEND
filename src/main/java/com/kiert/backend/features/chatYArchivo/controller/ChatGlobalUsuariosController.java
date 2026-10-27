package com.kiert.backend.features.chatYArchivo.controller;

import com.kiert.backend.features.chatYArchivo.dto.EnviarMensajeGlobalUsuarioDTO;
import com.kiert.backend.features.chatYArchivo.dto.MensajeGlobalUsuarioDTO;
import com.kiert.backend.features.chatYArchivo.service.ChatGlobalUsuariosService;
import com.kiert.backend.features.mensajes.dto.MensajeSimpleDTO;
import com.kiert.backend.security.UsuarioActual;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/chat/global-usuarios")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class ChatGlobalUsuariosController {

    private final ChatGlobalUsuariosService chatGlobalUsuariosService;
    private final UsuarioActual usuarioActual;

    // ========== LISTAR (público: invitados permitidos) ==========
    @GetMapping("/mensajes")
    public ResponseEntity<List<MensajeGlobalUsuarioDTO>> listar(
            @RequestParam(required = false) Long desdeId) {
        Long usuarioId = usuarioActual.id();
        return ResponseEntity.ok(chatGlobalUsuariosService.listar(usuarioId, desdeId));
    }

    // ========== ENVIAR (solo autenticados) ==========
    @PostMapping("/mensajes")
    public ResponseEntity<?> enviar(@Valid @RequestBody EnviarMensajeGlobalUsuarioDTO request) {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MensajeSimpleDTO("Debes iniciar sesión para escribir en el chat de usuarios"));
        }
        try {
            return ResponseEntity.ok(chatGlobalUsuariosService.enviar(usuarioId, request.contenido()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new MensajeSimpleDTO(e.getMessage()));
        }
    }
}