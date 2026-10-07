package com.kiert.backend.controller;

import com.kiert.backend.dto.CrearGrupoDTO;
import com.kiert.backend.dto.GrupoDTO;
import com.kiert.backend.dto.InvitacionGrupoDTO;
import com.kiert.backend.dto.MensajeGrupoDTO;
import com.kiert.backend.dto.MiembroGrupoDTO;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.service.GrupoChatService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/grupos")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class GrupoChatController {

    private final GrupoChatService grupoService;
    private final UsuarioActual usuarioActual;

    @PostMapping
    public ResponseEntity<?> crearGrupo(@RequestBody CrearGrupoDTO dto) {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return noAuth();
        }

        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(grupoService.crearGrupo(usuarioId, dto));
        } catch (RuntimeException exception) {
            log.error("No se pudo crear el grupo", exception);
            return badRequest(exception);
        }
    }

    @GetMapping("/mis-grupos")
    public ResponseEntity<List<GrupoDTO>> misGrupos() {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(grupoService.listarMisGrupos(usuarioId));
    }

    @GetMapping("/publicos")
    public ResponseEntity<List<GrupoDTO>> gruposPublicos() {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(grupoService.listarGruposPublicos(usuarioId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerGrupo(@PathVariable Long id) {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return noAuth();
        }

        try {
            return ResponseEntity.ok(grupoService.obtenerGrupo(id, usuarioId));
        } catch (RuntimeException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(error(exception.getMessage()));
        }
    }

    @GetMapping("/{id}/miembros")
    public ResponseEntity<?> listarMiembros(@PathVariable Long id) {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return noAuth();
        }

        try {
            grupoService.obtenerGrupo(id, usuarioId);
            List<MiembroGrupoDTO> miembros = grupoService.listarMiembros(id);
            return ResponseEntity.ok(miembros);
        } catch (SecurityException exception) {
            return forbidden(exception);
        } catch (RuntimeException exception) {
            return badRequest(exception);
        }
    }

    @GetMapping("/{id}/mensajes")
    public ResponseEntity<?> obtenerMensajes(@PathVariable Long id) {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return noAuth();
        }

        try {
            List<MensajeGrupoDTO> mensajes = grupoService.obtenerMensajes(id, usuarioId);
            return ResponseEntity.ok(mensajes);
        } catch (SecurityException exception) {
            return forbidden(exception);
        } catch (RuntimeException exception) {
            return badRequest(exception);
        }
    }

    @PostMapping("/{id}/mensajes")
    public ResponseEntity<?> enviarMensaje(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return noAuth();
        }

        try {
            String contenido = body == null ? null : body.get("contenido");
            return ResponseEntity.ok(
                    grupoService.enviarMensaje(id, usuarioId, contenido)
            );
        } catch (SecurityException exception) {
            return forbidden(exception);
        } catch (RuntimeException exception) {
            return badRequest(exception);
        }
    }

    @PostMapping(
            value = "/{id}/mensajes/archivos",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> enviarMensajeConArchivos(
            @PathVariable Long id,
            @RequestParam(value = "contenido", required = false) String contenido,
            @RequestParam(value = "archivos", required = false) List<MultipartFile> archivos
    ) {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return noAuth();
        }

        try {
            return ResponseEntity.ok(
                    grupoService.enviarMensajeConArchivos(
                            id,
                            usuarioId,
                            contenido,
                            archivos
                    )
            );
        } catch (SecurityException exception) {
            return forbidden(exception);
        } catch (RuntimeException exception) {
            return badRequest(exception);
        }
    }

    @PostMapping("/{id}/invitar")
    public ResponseEntity<?> invitarUsuarios(
            @PathVariable Long id,
            @RequestBody Map<String, List<Long>> body
    ) {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return noAuth();
        }

        try {
            List<Long> usuariosIds = body == null ? null : body.get("usuariosIds");
            grupoService.invitarUsuarios(id, usuarioId, usuariosIds);
            return ResponseEntity.ok(Map.of("mensaje", "Invitaciones enviadas"));
        } catch (SecurityException exception) {
            return forbidden(exception);
        } catch (RuntimeException exception) {
            return badRequest(exception);
        }
    }

    @PostMapping("/{id}/aceptar")
    public ResponseEntity<?> aceptarInvitacion(@PathVariable Long id) {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return noAuth();
        }

        try {
            grupoService.aceptarInvitacion(id, usuarioId);
            return ResponseEntity.ok(Map.of("mensaje", "Te uniste al grupo"));
        } catch (RuntimeException exception) {
            return badRequest(exception);
        }
    }

    @PostMapping("/{id}/rechazar")
    public ResponseEntity<?> rechazarInvitacion(@PathVariable Long id) {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return noAuth();
        }

        try {
            grupoService.rechazarInvitacion(id, usuarioId);
            return ResponseEntity.ok(Map.of("mensaje", "Invitacion rechazada"));
        } catch (RuntimeException exception) {
            return badRequest(exception);
        }
    }

    @PostMapping("/{id}/unirse")
    public ResponseEntity<?> unirseAGrupo(@PathVariable Long id) {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return noAuth();
        }

        try {
            grupoService.unirseAGrupoPublico(id, usuarioId);
            return ResponseEntity.ok(Map.of("mensaje", "Te uniste al grupo"));
        } catch (RuntimeException exception) {
            return badRequest(exception);
        }
    }

    @DeleteMapping("/{id}/salir")
    public ResponseEntity<?> salirDelGrupo(@PathVariable Long id) {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return noAuth();
        }

        try {
            grupoService.salirDelGrupo(id, usuarioId);
            return ResponseEntity.ok(Map.of("mensaje", "Saliste del grupo"));
        } catch (SecurityException exception) {
            return forbidden(exception);
        } catch (RuntimeException exception) {
            return badRequest(exception);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarGrupo(@PathVariable Long id) {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return noAuth();
        }

        try {
            grupoService.eliminarGrupo(id, usuarioId);
            return ResponseEntity.ok(
                    Map.of("mensaje", "Grupo eliminado correctamente")
            );
        } catch (SecurityException exception) {
            return forbidden(exception);
        } catch (RuntimeException exception) {
            log.error("No se pudo eliminar el grupo {}", id, exception);
            return badRequest(exception);
        }
    }

    @DeleteMapping("/{id}/miembros/{usuarioId}")
    public ResponseEntity<?> expulsarMiembro(
            @PathVariable Long id,
            @PathVariable Long usuarioId
    ) {
        Long adminId = usuarioActual.id();
        if (adminId == null) {
            return noAuth();
        }

        try {
            grupoService.expulsarMiembro(id, adminId, usuarioId);
            return ResponseEntity.ok(
                    Map.of("mensaje", "Miembro expulsado correctamente")
            );
        } catch (SecurityException exception) {
            return forbidden(exception);
        } catch (RuntimeException exception) {
            log.error("No se pudo expulsar al miembro {}", usuarioId, exception);
            return badRequest(exception);
        }
    }

    @GetMapping("/invitaciones")
    public ResponseEntity<List<InvitacionGrupoDTO>> invitacionesPendientes() {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(
                grupoService.listarInvitacionesPendientes(usuarioId)
        );
    }

    private ResponseEntity<?> noAuth() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(error("Usuario no autenticado"));
    }

    private ResponseEntity<?> badRequest(RuntimeException exception) {
        return ResponseEntity.badRequest().body(error(exception.getMessage()));
    }

    private ResponseEntity<?> forbidden(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(error(exception.getMessage()));
    }

    private Map<String, String> error(String mensaje) {
        return Map.of("error", mensaje == null ? "Error no especificado" : mensaje);
    }
}
