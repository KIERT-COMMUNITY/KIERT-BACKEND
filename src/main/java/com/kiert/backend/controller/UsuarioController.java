package com.kiert.backend.controller;

import com.kiert.backend.dto.SolicitudContactoDTO;
import com.kiert.backend.dto.UsuarioDTO;
import com.kiert.backend.dto.UsuarioDisponibleDTO;
import com.kiert.backend.entity.SolicitudContacto;
import com.kiert.backend.entity.SolicitudContacto.EstadoSolicitud;
import com.kiert.backend.entity.Usuario;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.repository.SolicitudContactoRepository;
import com.kiert.backend.repository.UsuarioRepository;
import com.kiert.backend.security.UsuarioActual;
import com.kiert.backend.service.UsuarioService; // ✅ IMPORTANTE
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final SolicitudContactoRepository solicitudContactoRepository;
    private final UsuarioActual usuarioActual;
    private final UsuarioService usuarioService; // ✅ Inyectado

    // ============================================================
    // OBTENER USUARIO
    // ============================================================
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerUsuario(@PathVariable Long id) {
        try {
            log.info("Obteniendo usuario con ID: {}", id);

            Usuario usuario = usuarioRepository.findById(id)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

            UsuarioDTO dto = new UsuarioDTO(
                    usuario.getId(),
                    usuario.getNombreUsuario(),
                    usuario.getEmail(),
                    usuario.getFotoPerfilUrl(),
                    usuario.getBio() // ✅ Incluimos la bio
            );

            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("Error al obtener usuario {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al obtener usuario: " + e.getMessage());
        }
    }

    // ============================================================
    // ACTUALIZAR USUARIO (Guardar bio, nombre, etc.)
    // ============================================================
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarUsuario(@PathVariable Long id, @RequestBody UsuarioDTO datos) {
        try {
            Long usuarioLogueadoId = usuarioActual.id();

            if (usuarioLogueadoId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
            }

            if (!usuarioLogueadoId.equals(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No tienes permiso para editar este perfil");
            }

            UsuarioDTO actualizado = usuarioService.actualizarUsuario(id, datos);
            return ResponseEntity.ok(actualizado);

        } catch (Exception e) {
            log.error("Error al actualizar usuario {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al actualizar usuario: " + e.getMessage());
        }
    }

    // ============================================================
    // BÚSQUEDA DE USUARIOS
    // ============================================================
    @GetMapping("/buscar")
    public ResponseEntity<?> buscarUsuarios(@RequestParam String q) {
        try {
            log.info("Buscando usuarios con: '{}'", q);

            Long usuarioActualId = usuarioActual.id();
            if (usuarioActualId == null) {
                log.warn("Usuario no autenticado para búsqueda");
                return ResponseEntity.ok(List.of());
            }

            List<Usuario> usuarios = usuarioRepository.findByNombreUsuarioContainingIgnoreCase(q.trim());
            log.info("Usuarios encontrados en BD: {}", usuarios.size());

            List<Usuario> usuariosFiltrados = usuarios.stream()
                    .filter(u -> !u.getId().equals(usuarioActualId))
                    .collect(Collectors.toList());

            List<UsuarioDisponibleDTO> resultado = usuariosFiltrados.stream()
                    .map(u -> new UsuarioDisponibleDTO(
                            u.getId(),
                            u.getNombreUsuario(),
                            u.getFotoPerfilUrl()
                    ))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            log.error("Error en buscarUsuarios: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al buscar usuarios: " + e.getMessage());
        }
    }

    // ============================================================
    // ENVIAR SOLICITUD DE CONTACTO
    // ============================================================
    @PostMapping("/solicitud/{usuarioId}")
    public ResponseEntity<?> enviarSolicitud(@PathVariable Long usuarioId) {
        try {
            Long emisorId = usuarioActual.id();
            if (emisorId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
            }

            if (emisorId.equals(usuarioId)) {
                return ResponseEntity.badRequest().body("No puedes enviarte solicitud a ti mismo");
            }

            Usuario receptor = usuarioRepository.findById(usuarioId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

            boolean existeSolicitud = solicitudContactoRepository.existsByEmisorIdAndReceptorIdAndEstado(
                    emisorId, usuarioId, EstadoSolicitud.PENDIENTE
            );

            if (existeSolicitud) {
                return ResponseEntity.badRequest().body("Ya has enviado una solicitud a este usuario");
            }

            boolean sonContactos = solicitudContactoRepository.sonContactos(emisorId, usuarioId);
            if (sonContactos) {
                return ResponseEntity.badRequest().body("Ya son contactos");
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

        } catch (Exception e) {
            log.error("Error en enviarSolicitud: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al enviar solicitud: " + e.getMessage());
        }
    }

    // ============================================================
    // ACEPTAR SOLICITUD
    // ============================================================
    @PutMapping("/solicitud/{solicitudId}/aceptar")
    public ResponseEntity<?> aceptarSolicitud(@PathVariable Long solicitudId) {
        try {
            Long receptorId = usuarioActual.id();
            if (receptorId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
            }

            SolicitudContacto solicitud = solicitudContactoRepository.findById(solicitudId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud no encontrada"));

            if (!solicitud.getReceptor().getId().equals(receptorId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No tienes permiso para aceptar esta solicitud");
            }

            if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
                return ResponseEntity.badRequest().body("Esta solicitud ya ha sido procesada");
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

        } catch (Exception e) {
            log.error("Error en aceptarSolicitud: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al aceptar solicitud: " + e.getMessage());
        }
    }

    // ============================================================
    // RECHAZAR SOLICITUD
    // ============================================================
    @DeleteMapping("/solicitud/{solicitudId}")
    public ResponseEntity<?> rechazarSolicitud(@PathVariable Long solicitudId) {
        try {
            Long receptorId = usuarioActual.id();
            if (receptorId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
            }

            SolicitudContacto solicitud = solicitudContactoRepository.findById(solicitudId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud no encontrada"));

            if (!solicitud.getReceptor().getId().equals(receptorId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No tienes permiso para rechazar esta solicitud");
            }

            solicitud.setEstado(EstadoSolicitud.RECHAZADA);
            solicitud.setFechaRespuesta(Instant.now());
            solicitudContactoRepository.save(solicitud);

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Error en rechazarSolicitud: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al rechazar solicitud: " + e.getMessage());
        }
    }

    // ============================================================
    // SOLICITUDES PENDIENTES
    // ============================================================
    @GetMapping("/solicitudes/pendientes")
    public ResponseEntity<?> obtenerSolicitudesPendientes() {
        try {
            Long usuarioId = usuarioActual.id();
            if (usuarioId == null) {
                return ResponseEntity.ok(List.of());
            }

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
            log.error("Error en obtenerSolicitudesPendientes: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al obtener solicitudes: " + e.getMessage());
        }
    }

    // ============================================================
    // CONTACTOS
    // ============================================================
    @GetMapping("/contactos")
    public ResponseEntity<?> obtenerContactos() {
        try {
            Long usuarioId = usuarioActual.id();
            if (usuarioId == null) {
                return ResponseEntity.ok(List.of());
            }

            List<SolicitudContacto> solicitudes = solicitudContactoRepository
                    .findAllByUsuarioIdAndEstado(usuarioId, EstadoSolicitud.ACEPTADA);

            List<UsuarioDisponibleDTO> contactos = solicitudes.stream()
                    .map(s -> {
                        Long contactoId = s.getEmisor().getId().equals(usuarioId) ?
                                s.getReceptor().getId() : s.getEmisor().getId();
                        Usuario contacto = usuarioRepository.findById(contactoId)
                                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
                        return new UsuarioDisponibleDTO(
                                contacto.getId(),
                                contacto.getNombreUsuario(),
                                contacto.getFotoPerfilUrl()
                        );
                    })
                    .distinct()
                    .collect(Collectors.toList());

            return ResponseEntity.ok(contactos);

        } catch (Exception e) {
            log.error("Error en obtenerContactos: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al obtener contactos: " + e.getMessage());
        }
    }
}