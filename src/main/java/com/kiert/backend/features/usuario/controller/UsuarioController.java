// src/main/java/com/kiert/backend/features/usuario/controller/UsuarioController.java
package com.kiert.backend.features.usuario.controller;

import com.kiert.backend.features.solicitud.dto.SolicitudContactoDTO;
import com.kiert.backend.features.usuario.dto.UsuarioDTO;
import com.kiert.backend.features.usuario.dto.UsuarioDisponibleDTO;
import com.kiert.backend.entity.SolicitudContacto;
import com.kiert.backend.entity.SolicitudContacto.EstadoSolicitud;
import com.kiert.backend.entity.Usuario;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.features.solicitud.repository.SolicitudContactoRepository;
import com.kiert.backend.features.usuario.repository.UsuarioRepository;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.features.usuario.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final SolicitudContactoRepository solicitudContactoRepository;
    private final UsuarioActual usuarioActual;
    private final UsuarioService usuarioService;

    // ============================================================
    // OBTENER USUARIO
    // ============================================================
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> obtenerUsuario(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "ID de usuario invalido"));
        }

        try {
            Usuario usuario = usuarioRepository.findById(id)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

            UsuarioDTO dto = new UsuarioDTO(
                    usuario.getId(),
                    usuario.getNombreUsuario(),
                    usuario.getEmail(),
                    usuario.getFotoPerfilUrl(),
                    usuario.getBio()
            );

            return ResponseEntity.ok(dto);
        } catch (RecursoNoEncontradoException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Usuario no encontrado"));
        } catch (Exception e) {
            log.warn("Error al obtener usuario {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "No se pudo obtener el usuario"));
        }
    }

    // ============================================================
    // ACTUALIZAR USUARIO
    // ============================================================
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> actualizarUsuario(@PathVariable Long id, @RequestBody UsuarioDTO datos) {
        Long usuarioLogueadoId = usuarioActual.id();
        if (usuarioLogueadoId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no autenticado"));
        }

        if (!usuarioLogueadoId.equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "No tienes permiso para editar este perfil"));
        }

        if (datos == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Datos requeridos"));
        }

        try {
            UsuarioDTO actualizado = usuarioService.actualizarUsuario(id, datos);
            return ResponseEntity.ok(actualizado);
        } catch (Exception e) {
            log.warn("Error al actualizar usuario {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "No se pudo actualizar el usuario"));
        }
    }

    // ============================================================
    // ELIMINAR MI CUENTA
    // ============================================================
    @DeleteMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> eliminarMiCuenta() {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no autenticado"));
        }

        try {
            usuarioService.eliminarCuenta(usuarioId);
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Tu cuenta ha sido eliminada correctamente.",
                    "exito", true
            ));
        } catch (Exception e) {
            log.warn("Error al eliminar la cuenta del usuario {}: {}", usuarioId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "No se pudo eliminar la cuenta"));
        }
    }

    // ============================================================
    // BUSQUEDA DE USUARIOS
    // ============================================================
    @GetMapping("/buscar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> buscarUsuarios(@RequestParam("q") String q) {
        Long usuarioActualId = usuarioActual.id();
        if (usuarioActualId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no autenticado"));
        }

        if (q == null || q.trim().isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        try {
            List<UsuarioDisponibleDTO> resultado = usuarioService.buscarUsuarios(q, usuarioActualId);
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            log.warn("Error en buscarUsuarios: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "No se pudo buscar usuarios"));
        }
    }

    // ============================================================
    // ENVIAR SOLICITUD DE CONTACTO
    // ============================================================
    @PostMapping("/solicitud/{usuarioId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> enviarSolicitud(@PathVariable Long usuarioId) {
        Long emisorId = usuarioActual.id();
        if (emisorId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no autenticado"));
        }

        if (emisorId.equals(usuarioId)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No puedes enviarte solicitud a ti mismo"));
        }

        try {
            Usuario receptor = usuarioRepository.findById(usuarioId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

            boolean existeSolicitud = solicitudContactoRepository.existsByEmisorIdAndReceptorIdAndEstado(
                    emisorId, usuarioId, EstadoSolicitud.PENDIENTE
            );

            if (existeSolicitud) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Ya has enviado una solicitud a este usuario"));
            }

            boolean sonContactos = solicitudContactoRepository.sonContactos(emisorId, usuarioId);
            if (sonContactos) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Ya son contactos"));
            }

            Usuario emisor = usuarioRepository.findById(emisorId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

            SolicitudContacto solicitud = SolicitudContacto.builder()
                    .emisor(emisor)
                    .receptor(receptor)
                    .estado(EstadoSolicitud.PENDIENTE)
                    .fechaSolicitud(Instant.now())
                    .build();

            solicitud = solicitudContactoRepository.save(solicitud);

            SolicitudContactoDTO dto = new SolicitudContactoDTO(
                    solicitud.getId(),
                    receptor.getId(),
                    receptor.getNombreUsuario(),
                    receptor.getFotoPerfilUrl(),
                    solicitud.getEstado().name(),
                    solicitud.getFechaSolicitud()
            );

            return ResponseEntity.ok(dto);
        } catch (RecursoNoEncontradoException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Usuario no encontrado"));
        } catch (Exception e) {
            log.warn("Error en enviarSolicitud: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "No se pudo enviar la solicitud"));
        }
    }

    // ============================================================
    // ACEPTAR SOLICITUD
    // ============================================================
    @PutMapping("/solicitud/{solicitudId}/aceptar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> aceptarSolicitud(@PathVariable Long solicitudId) {
        Long receptorId = usuarioActual.id();
        if (receptorId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no autenticado"));
        }

        try {
            SolicitudContacto solicitud = solicitudContactoRepository.findById(solicitudId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud no encontrada"));

            if (!solicitud.getReceptor().getId().equals(receptorId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No tienes permiso para aceptar esta solicitud"));
            }

            if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Esta solicitud ya ha sido procesada"));
            }

            solicitud.setEstado(EstadoSolicitud.ACEPTADA);
            solicitud.setFechaRespuesta(Instant.now());
            solicitud = solicitudContactoRepository.save(solicitud);

            SolicitudContactoDTO dto = new SolicitudContactoDTO(
                    solicitud.getId(),
                    solicitud.getEmisor().getId(),
                    solicitud.getEmisor().getNombreUsuario(),
                    solicitud.getEmisor().getFotoPerfilUrl(),
                    solicitud.getEstado().name(),
                    solicitud.getFechaSolicitud()
            );

            return ResponseEntity.ok(dto);
        } catch (RecursoNoEncontradoException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Solicitud no encontrada"));
        } catch (Exception e) {
            log.warn("Error en aceptarSolicitud: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "No se pudo aceptar la solicitud"));
        }
    }

    // ============================================================
    // RECHAZAR SOLICITUD
    // ============================================================
    @DeleteMapping("/solicitud/{solicitudId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> rechazarSolicitud(@PathVariable Long solicitudId) {
        Long receptorId = usuarioActual.id();
        if (receptorId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no autenticado"));
        }

        try {
            SolicitudContacto solicitud = solicitudContactoRepository.findById(solicitudId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud no encontrada"));

            if (!solicitud.getReceptor().getId().equals(receptorId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No tienes permiso para rechazar esta solicitud"));
            }

            if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Esta solicitud ya ha sido procesada"));
            }

            solicitud.setEstado(EstadoSolicitud.RECHAZADA);
            solicitud.setFechaRespuesta(Instant.now());
            solicitudContactoRepository.save(solicitud);

            return ResponseEntity.ok(Map.of("mensaje", "Solicitud rechazada"));
        } catch (RecursoNoEncontradoException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Solicitud no encontrada"));
        } catch (Exception e) {
            log.warn("Error en rechazarSolicitud: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "No se pudo rechazar la solicitud"));
        }
    }

    // ============================================================
    // SOLICITUDES PENDIENTES
    // ============================================================
    @GetMapping("/solicitudes/pendientes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> obtenerSolicitudesPendientes() {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no autenticado"));
        }

        try {
            List<SolicitudContacto> solicitudes = solicitudContactoRepository
                    .findByReceptorIdAndEstado(usuarioId, EstadoSolicitud.PENDIENTE);

            List<SolicitudContactoDTO> resultado = solicitudes.stream()
                    .map(s -> new SolicitudContactoDTO(
                            s.getId(),
                            s.getEmisor().getId(),
                            s.getEmisor().getNombreUsuario(),
                            s.getEmisor().getFotoPerfilUrl(),
                            s.getEstado().name(),
                            s.getFechaSolicitud()
                    ))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            log.warn("Error en obtenerSolicitudesPendientes: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "No se pudieron obtener las solicitudes"));
        }
    }

    // ============================================================
    // CONTACTOS
    // ============================================================
    @GetMapping("/contactos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> obtenerContactos() {
        Long usuarioId = usuarioActual.id();
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no autenticado"));
        }

        try {
            List<Long> contactosIds = solicitudContactoRepository.findContactosIds(usuarioId);

            if (contactosIds.isEmpty()) {
                return ResponseEntity.ok(List.of());
            }

            List<Usuario> contactos = usuarioRepository.findAllById(contactosIds);

            List<UsuarioDisponibleDTO> resultado = contactos.stream()
                    .filter(u -> !Boolean.TRUE.equals(u.getEliminado()))
                    .map(u -> new UsuarioDisponibleDTO(
                            u.getId(),
                            u.getNombreUsuario(),
                            u.getFotoPerfilUrl()
                    ))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            log.warn("Error en obtenerContactos: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "No se pudieron obtener los contactos"));
        }
    }
}