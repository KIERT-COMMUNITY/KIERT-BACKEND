package com.kiert.backend.controller;

import com.kiert.backend.dto.EnviarMensajeDTO;
import com.kiert.backend.dto.EnviarSolicitudDTO;
import com.kiert.backend.dto.MensajeSimpleDTO;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.service.ChatService;
import com.kiert.backend.service.NotificacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class ChatController {

    private final ChatService chatService;
    private final NotificacionService notificacionService;
    private final UsuarioActual usuarioActual;

    @GetMapping("/conversaciones")
    public ResponseEntity<?> listarConversaciones() {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return noAuth();
        }
        return ResponseEntity.ok(chatService.listarConversaciones(usuarioId));
    }

    @GetMapping("/{usuarioId}")
    public ResponseEntity<?> obtenerMensajes(@PathVariable Long usuarioId) {
        Long usuarioActualId = usuarioActual.id();
        if (usuarioActualId == null) {
            return noAuth();
        }
        return ResponseEntity.ok(
                chatService.obtenerMensajes(usuarioActualId, usuarioId)
        );
    }

    @PostMapping("/{usuarioId}")
    public ResponseEntity<?> enviarMensaje(
            @PathVariable Long usuarioId,
            @Valid @RequestBody EnviarMensajeDTO request
    ) {
        Long usuarioActualId = usuarioActual.id();
        if (usuarioActualId == null) {
            return noAuth();
        }

        try {
            return ResponseEntity.ok(
                    chatService.enviarMensaje(
                            usuarioActualId,
                            usuarioId,
                            request.contenido()
                    )
            );
        } catch (IllegalArgumentException exception) {
            return badRequest(exception);
        } catch (IllegalStateException exception) {
            return forbidden(exception);
        }
    }

    @PostMapping(
            value = "/{usuarioId}/archivos",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> enviarMensajeConArchivos(
            @PathVariable Long usuarioId,
            @RequestParam(value = "contenido", required = false) String contenido,
            @RequestParam(value = "archivos", required = false) List<MultipartFile> archivos
    ) {
        Long usuarioActualId = usuarioActual.id();
        if (usuarioActualId == null) {
            return noAuth();
        }

        try {
            return ResponseEntity.ok(
                    chatService.enviarMensajeConArchivos(
                            usuarioActualId,
                            usuarioId,
                            contenido,
                            archivos
                    )
            );
        } catch (IllegalArgumentException exception) {
            return badRequest(exception);
        } catch (IllegalStateException exception) {
            return forbidden(exception);
        }
    }

    @PutMapping("/mensajes/{usuarioId}/leidos")
    public ResponseEntity<?> marcarMensajesComoLeidos(
            @PathVariable Long usuarioId
    ) {
        Long usuarioActualId = usuarioActual.id();
        if (usuarioActualId == null) {
            return noAuth();
        }

        chatService.marcarMensajesComoLeidos(usuarioActualId, usuarioId);
        return ResponseEntity.ok(
                new MensajeSimpleDTO("Mensajes marcados como leidos")
        );
    }

    @GetMapping("/no-leidos")
    public ResponseEntity<?> obtenerMensajesNoLeidos() {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return noAuth();
        }
        return ResponseEntity.ok(chatService.obtenerMensajesNoLeidos(usuarioId));
    }

    @GetMapping("/solicitudes")
    public ResponseEntity<?> listarSolicitudes() {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return noAuth();
        }
        return ResponseEntity.ok(chatService.listarSolicitudes(usuarioId));
    }

    @GetMapping("/solicitudes/enviadas")
    public ResponseEntity<?> listarSolicitudesEnviadas() {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return noAuth();
        }
        return ResponseEntity.ok(chatService.listarSolicitudesEnviadas(usuarioId));
    }

    @PostMapping("/solicitudes")
    public ResponseEntity<?> enviarSolicitud(
            @Valid @RequestBody EnviarSolicitudDTO request
    ) {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return noAuth();
        }

        try {
            Object solicitud = chatService.enviarSolicitud(
                    usuarioId,
                    request.usuarioId()
            );

            try {
                notificacionService.crearNotificacionSolicitud(
                        usuarioId,
                        request.usuarioId()
                );
            } catch (RuntimeException exception) {
                log.error(
                        "No se pudo crear la notificacion de solicitud",
                        exception
                );
            }

            return ResponseEntity.ok(solicitud);
        } catch (IllegalArgumentException exception) {
            return badRequest(exception);
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new MensajeSimpleDTO(exception.getMessage()));
        }
    }

    @PutMapping("/solicitudes/{solicitudId}/aceptar")
    public ResponseEntity<?> aceptarSolicitud(
            @PathVariable Long solicitudId
    ) {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return noAuth();
        }

        chatService.aceptarSolicitud(solicitudId, usuarioId);
        return ResponseEntity.ok(new MensajeSimpleDTO("Solicitud aceptada"));
    }

    @PutMapping("/solicitudes/{solicitudId}/rechazar")
    public ResponseEntity<?> rechazarSolicitud(
            @PathVariable Long solicitudId
    ) {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return noAuth();
        }

        chatService.rechazarSolicitud(solicitudId, usuarioId);
        return ResponseEntity.ok(new MensajeSimpleDTO("Solicitud rechazada"));
    }

    @GetMapping("/contactos/{usuarioId}")
    public ResponseEntity<?> sonContactos(@PathVariable Long usuarioId) {
        Long usuarioActualId = usuarioActual.id();
        if (usuarioActualId == null) {
            return noAuth();
        }
        return ResponseEntity.ok(
                chatService.sonContactos(usuarioActualId, usuarioId)
        );
    }

    @GetMapping("/usuarios/disponibles")
    public ResponseEntity<?> listarUsuariosDisponibles() {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return noAuth();
        }
        return ResponseEntity.ok(
                chatService.listarUsuariosDisponibles(usuarioId)
        );
    }

    @DeleteMapping("/contactos/{usuarioId}")
    public ResponseEntity<?> eliminarContacto(@PathVariable Long usuarioId) {
        Long usuarioActualId = usuarioActual.id();
        if (usuarioActualId == null) {
            return noAuth();
        }

        chatService.eliminarContacto(usuarioActualId, usuarioId);
        return ResponseEntity.ok(new MensajeSimpleDTO("Contacto eliminado"));
    }

    private ResponseEntity<?> noAuth() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new MensajeSimpleDTO("Usuario no autenticado"));
    }

    private ResponseEntity<?> badRequest(RuntimeException exception) {
        return ResponseEntity.badRequest()
                .body(new MensajeSimpleDTO(exception.getMessage()));
    }

    private ResponseEntity<?> forbidden(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new MensajeSimpleDTO(exception.getMessage()));
    }
}
