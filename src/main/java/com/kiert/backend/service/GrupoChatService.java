// src/main/java/com/kiert/backend/service/GrupoChatService.java
package com.kiert.backend.service;

import com.kiert.backend.dto.*;
import com.kiert.backend.entity.*;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GrupoChatService {

    private final GrupoChatRepository grupoRepository;
    private final MiembroGrupoRepository miembroRepository;
    private final MensajeGrupoRepository mensajeRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;

    // ============================================================
    // CREAR GRUPO
    // ============================================================
    @Transactional
    public GrupoDTO crearGrupo(Long creadorId, CrearGrupoDTO dto) {
        log.info("📢 Usuario {} creando grupo: {}", creadorId, dto.nombre());

        if (dto.nombre() == null || dto.nombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del grupo es obligatorio");
        }

        Usuario creador = usuarioRepository.findById(creadorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        GrupoChat grupo = GrupoChat.builder()
                .nombre(dto.nombre().trim())
                .descripcion(dto.descripcion())
                .creador(creador)
                .tipo(dto.tipo() != null ? dto.tipo() : "PRIVADO")
                .fechaCreacion(Instant.now())
                .activo(true)
                .build();

        grupo = grupoRepository.save(grupo);

        // Creador → ADMIN/ACTIVO
        MiembroGrupo admin = MiembroGrupo.builder()
                .grupo(grupo)
                .usuario(creador)
                .rol("ADMIN")
                .estado("ACTIVO")
                .fechaUnion(Instant.now())
                .build();
        miembroRepository.save(admin);

        // Invitar usuarios
        if (dto.usuariosInvitados() != null && !dto.usuariosInvitados().isEmpty()) {
            for (Long usuarioId : dto.usuariosInvitados()) {
                if (usuarioId.equals(creadorId)) continue;

                Usuario invitado = usuarioRepository.findById(usuarioId).orElse(null);
                if (invitado == null) continue;

                String estadoInvitacion = grupo.getTipo().equals("PUBLICO") ? "ACTIVO" : "PENDIENTE";

                MiembroGrupo miembro = MiembroGrupo.builder()
                        .grupo(grupo)
                        .usuario(invitado)
                        .rol("MIEMBRO")
                        .estado(estadoInvitacion)
                        .fechaUnion(Instant.now())
                        .fechaInvitacion(Instant.now())
                        .invitadoPor(creador)
                        .build();
                miembroRepository.save(miembro);

                if (grupo.getTipo().equals("PRIVADO")) {
                    try {
                        notificacionService.crearNotificacionGrupo(
                                usuarioId,
                                creadorId,
                                "<strong>" + creador.getNombreUsuario() + "</strong> te invitó al grupo <strong>" + grupo.getNombre() + "</strong>",
                                grupo.getId(),
                                "/chat/grupo/" + grupo.getId()
                        );
                    } catch (Exception e) {
                        log.error("❌ Error notificación: {}", e.getMessage());
                    }
                }
            }
        }

        return mapearADTO(grupo, creadorId);
    }

    // ============================================================
    // LISTAR
    // ============================================================
    @Transactional(readOnly = true)
    public List<GrupoDTO> listarMisGrupos(Long usuarioId) {
        return grupoRepository.findGruposDeUsuario(usuarioId)
                .stream()
                .map(g -> mapearADTO(g, usuarioId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GrupoDTO> listarGruposPublicos(Long usuarioId) {
        return grupoRepository.findGruposPublicosDisponibles(usuarioId)
                .stream()
                .map(g -> mapearADTO(g, usuarioId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GrupoDTO obtenerGrupo(Long grupoId, Long usuarioId) {
        GrupoChat grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Grupo no encontrado"));
        return mapearADTO(grupo, usuarioId);
    }

    // ============================================================
    // INVITAR
    // ============================================================
    @Transactional
    public void invitarUsuarios(Long grupoId, Long invitadorId, List<Long> usuariosIds) {
        GrupoChat grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Grupo no encontrado"));

        Usuario invitador = usuarioRepository.findById(invitadorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        if (!miembroRepository.esMiembroActivo(grupoId, invitadorId)) {
            throw new SecurityException("No eres miembro de este grupo");
        }

        for (Long usuarioId : usuariosIds) {
            if (miembroRepository.findByGrupoIdAndUsuarioId(grupoId, usuarioId).isPresent()) {
                continue;
            }

            Usuario invitado = usuarioRepository.findById(usuarioId).orElse(null);
            if (invitado == null) continue;

            MiembroGrupo miembro = MiembroGrupo.builder()
                    .grupo(grupo)
                    .usuario(invitado)
                    .rol("MIEMBRO")
                    .estado(grupo.getTipo().equals("PUBLICO") ? "ACTIVO" : "PENDIENTE")
                    .fechaUnion(Instant.now())
                    .fechaInvitacion(Instant.now())
                    .invitadoPor(invitador)
                    .build();
            miembroRepository.save(miembro);

            if (grupo.getTipo().equals("PRIVADO")) {
                notificacionService.crearNotificacionGrupo(
                        usuarioId,
                        invitadorId,
                        "<strong>" + invitador.getNombreUsuario() + "</strong> te invitó al grupo <strong>" + grupo.getNombre() + "</strong>",
                        grupoId,
                        "/chat/grupo/" + grupoId
                );
            }
        }
    }

    // ============================================================
    // ACEPTAR / RECHAZAR / UNIRSE / SALIR
    // ============================================================
    @Transactional
    public void aceptarInvitacion(Long grupoId, Long usuarioId) {
        MiembroGrupo miembro = miembroRepository.findByGrupoIdAndUsuarioId(grupoId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Invitación no encontrada"));

        if (!miembro.getEstado().equals("PENDIENTE")) {
            throw new IllegalArgumentException("Esta invitación ya fue procesada");
        }

        miembro.setEstado("ACTIVO");
        miembro.setFechaUnion(Instant.now());
        miembroRepository.save(miembro);
    }

    @Transactional
    public void rechazarInvitacion(Long grupoId, Long usuarioId) {
        MiembroGrupo miembro = miembroRepository.findByGrupoIdAndUsuarioId(grupoId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Invitación no encontrada"));

        miembro.setEstado("RECHAZADO");
        miembroRepository.save(miembro);
    }

    @Transactional
    public void unirseAGrupoPublico(Long grupoId, Long usuarioId) {
        GrupoChat grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Grupo no encontrado"));

        if (!grupo.getTipo().equals("PUBLICO")) {
            throw new IllegalArgumentException("Este grupo no es público");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        var existente = miembroRepository.findByGrupoIdAndUsuarioId(grupoId, usuarioId);
        if (existente.isPresent()) {
            MiembroGrupo m = existente.get();
            if (m.getEstado().equals("ACTIVO")) {
                throw new IllegalArgumentException("Ya eres miembro de este grupo");
            }
            m.setEstado("ACTIVO");
            m.setFechaUnion(Instant.now());
            miembroRepository.save(m);
        } else {
            MiembroGrupo miembro = MiembroGrupo.builder()
                    .grupo(grupo)
                    .usuario(usuario)
                    .rol("MIEMBRO")
                    .estado("ACTIVO")
                    .fechaUnion(Instant.now())
                    .build();
            miembroRepository.save(miembro);
        }
    }

    @Transactional
    public void salirDelGrupo(Long grupoId, Long usuarioId) {
        MiembroGrupo miembro = miembroRepository.findByGrupoIdAndUsuarioId(grupoId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No perteneces a este grupo"));

        // Si es el creador, no puede salir (debe eliminar el grupo)
        GrupoChat grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Grupo no encontrado"));

        if (grupo.getCreador().getId().equals(usuarioId)) {
            throw new SecurityException("Como creador, debes eliminar el grupo en vez de salir");
        }

        miembro.setEstado("SALIO");
        miembroRepository.save(miembro);

        log.info("👋 Usuario {} salió del grupo {}", usuarioId, grupoId);
    }

    // ============================================================
    // 🔥 ELIMINAR GRUPO (solo creador o ADMIN)
    // ============================================================
    @Transactional
    public void eliminarGrupo(Long grupoId, Long usuarioId) {
        log.info("🗑️ Usuario {} intentando eliminar grupo {}", usuarioId, grupoId);

        GrupoChat grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Grupo no encontrado"));

        if (!grupo.isActivo()) {
            throw new IllegalArgumentException("El grupo ya fue eliminado");
        }

        // Verificar permisos
        MiembroGrupo miembro = miembroRepository.findByGrupoIdAndUsuarioId(grupoId, usuarioId)
                .orElseThrow(() -> new SecurityException("No eres miembro de este grupo"));

        boolean esCreador = grupo.getCreador().getId().equals(usuarioId);
        boolean esAdmin = "ADMIN".equals(miembro.getRol());

        if (!esCreador && !esAdmin) {
            throw new SecurityException("Solo el creador o un administrador puede eliminar el grupo");
        }

        // Soft delete
        grupo.setActivo(false);
        grupoRepository.save(grupo);

        log.info("✅ Grupo {} eliminado por usuario {}", grupoId, usuarioId);
    }

    // ============================================================
    // 🔥 EXPULSAR MIEMBRO (solo ADMIN)
    // ============================================================
    @Transactional
    public void expulsarMiembro(Long grupoId, Long adminId, Long usuarioAExpulsarId) {
        log.info("🚫 Admin {} expulsa a {} del grupo {}", adminId, usuarioAExpulsarId, grupoId);

        if (adminId.equals(usuarioAExpulsarId)) {
            throw new IllegalArgumentException("No puedes expulsarte a ti mismo. Usa 'Salir del grupo'.");
        }

        GrupoChat grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Grupo no encontrado"));

        // Verificar que adminId es ADMIN del grupo
        MiembroGrupo admin = miembroRepository.findByGrupoIdAndUsuarioId(grupoId, adminId)
                .orElseThrow(() -> new SecurityException("No eres miembro de este grupo"));

        if (!"ADMIN".equals(admin.getRol())) {
            throw new SecurityException("Solo los administradores pueden expulsar miembros");
        }

        // Verificar que el usuario a expulsar existe
        MiembroGrupo miembro = miembroRepository.findByGrupoIdAndUsuarioId(grupoId, usuarioAExpulsarId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no pertenece al grupo"));

        if (!"ACTIVO".equals(miembro.getEstado())) {
            throw new IllegalArgumentException("El usuario no es miembro activo del grupo");
        }

        // No permitir expulsar al creador
        if (grupo.getCreador().getId().equals(usuarioAExpulsarId)) {
            throw new SecurityException("No puedes expulsar al creador del grupo");
        }

        // Marcar como EXPULSADO
        miembro.setEstado("EXPULSADO");
        miembroRepository.save(miembro);

        log.info("✅ Usuario {} expulsado del grupo {}", usuarioAExpulsarId, grupoId);
    }

    // ============================================================
    // LISTADOS AUXILIARES
    // ============================================================
    @Transactional(readOnly = true)
    public List<InvitacionGrupoDTO> listarInvitacionesPendientes(Long usuarioId) {
        return miembroRepository.findInvitacionesPendientes(usuarioId)
                .stream()
                .map(m -> new InvitacionGrupoDTO(
                        m.getId(),
                        m.getGrupo().getId(),
                        m.getGrupo().getNombre(),
                        m.getGrupo().getFotoUrl(),
                        m.getInvitadoPor() != null ? m.getInvitadoPor().getId() : null,
                        m.getInvitadoPor() != null ? m.getInvitadoPor().getNombreUsuario() : "Sistema",
                        m.getFechaInvitacion()
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MiembroGrupoDTO> listarMiembros(Long grupoId) {
        return miembroRepository.findMiembrosActivos(grupoId)
                .stream()
                .map(m -> new MiembroGrupoDTO(
                        m.getId(),
                        m.getUsuario().getId(),
                        m.getUsuario().getNombreUsuario(),
                        m.getUsuario().getEmail(),
                        m.getUsuario().getFotoPerfilUrl(),
                        m.getRol(),
                        m.getEstado(),
                        m.getFechaUnion(),
                        m.getInvitadoPor() != null ? m.getInvitadoPor().getNombreUsuario() : null
                ))
                .collect(Collectors.toList());
    }

    private GrupoDTO mapearADTO(GrupoChat grupo, Long usuarioId) {
        List<MiembroGrupoDTO> miembros = listarMiembros(grupo.getId());

        String rolDelUsuario = miembros.stream()
                .filter(m -> m.usuarioId().equals(usuarioId))
                .findFirst()
                .map(MiembroGrupoDTO::rol)
                .orElse(null);

        return new GrupoDTO(
                grupo.getId(),
                grupo.getNombre(),
                grupo.getDescripcion(),
                grupo.getFotoUrl(),
                grupo.getCreador().getId(),
                grupo.getCreador().getNombreUsuario(),
                grupo.getTipo(),
                grupo.getFechaCreacion(),
                miembros.size(),
                miembros,
                rolDelUsuario
        );
    }

    // ============================================================
    // MENSAJES
    // ============================================================
    @Transactional(readOnly = true)
    public List<MensajeGrupoDTO> obtenerMensajes(Long grupoId) {
        return obtenerMensajes(grupoId, null);
    }

    @Transactional(readOnly = true)
    public List<MensajeGrupoDTO> obtenerMensajes(Long grupoId, Long usuarioActualId) {
        List<MensajeGrupo> mensajes = mensajeRepository.findMensajesDeGrupo(grupoId);
        return mensajes.stream()
                .map(m -> new MensajeGrupoDTO(
                        m.getId(),
                        m.getGrupo().getId(),
                        m.getEmisor().getId(),
                        m.getEmisor().getNombreUsuario(),
                        m.getEmisor().getFotoPerfilUrl(),
                        m.getContenido(),
                        m.getTipoMensaje(),
                        m.getUrlArchivo(),
                        m.getNombreArchivo(),
                        m.getFechaEnvio(),
                        usuarioActualId != null && m.getEmisor().getId().equals(usuarioActualId)
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public MensajeGrupoDTO enviarMensaje(Long grupoId, Long emisorId, String contenido) {
        GrupoChat grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Grupo no encontrado"));

        if (!miembroRepository.esMiembroActivo(grupoId, emisorId)) {
            throw new SecurityException("No eres miembro de este grupo");
        }

        Usuario emisor = usuarioRepository.findById(emisorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        MensajeGrupo mensaje = MensajeGrupo.builder()
                .grupo(grupo)
                .emisor(emisor)
                .contenido(contenido)
                .tipoMensaje("TEXTO")
                .fechaEnvio(Instant.now())
                .eliminado(false)
                .build();

        mensaje = mensajeRepository.save(mensaje);

        return new MensajeGrupoDTO(
                mensaje.getId(),
                grupo.getId(),
                emisor.getId(),
                emisor.getNombreUsuario(),
                emisor.getFotoPerfilUrl(),
                mensaje.getContenido(),
                mensaje.getTipoMensaje(),
                mensaje.getUrlArchivo(),
                mensaje.getNombreArchivo(),
                mensaje.getFechaEnvio(),
                true
        );
    }
}