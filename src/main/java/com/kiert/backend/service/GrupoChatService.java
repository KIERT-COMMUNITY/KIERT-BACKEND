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
    // ===== CREAR GRUPO =====
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

        // 1️⃣ Agregar al creador como ADMIN (siempre ACTIVO)
        MiembroGrupo admin = MiembroGrupo.builder()
                .grupo(grupo)
                .usuario(creador)
                .rol("ADMIN")
                .estado("ACTIVO")
                .fechaUnion(Instant.now())
                .build();
        miembroRepository.save(admin);
        log.info("✅ Creador agregado como ADMIN");

        // 2️⃣ Invitar a los usuarios seleccionados
        if (dto.usuariosInvitados() != null && !dto.usuariosInvitados().isEmpty()) {
            log.info("📨 Enviando {} invitaciones", dto.usuariosInvitados().size());

            for (Long usuarioId : dto.usuariosInvitados()) {
                if (usuarioId.equals(creadorId)) continue;

                Usuario invitado = usuarioRepository.findById(usuarioId).orElse(null);
                if (invitado == null) {
                    log.warn("⚠️ Usuario {} no encontrado", usuarioId);
                    continue;
                }

                // Si el grupo es PÚBLICO, se une directo como ACTIVO
                // Si el grupo es PRIVADO, queda PENDIENTE hasta que acepte
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

                log.info("✅ Invitación guardada para {}", invitado.getNombreUsuario());

                // 3️⃣ ✅ ENVIAR NOTIFICACIÓN (esto es lo que faltaba)
                if (grupo.getTipo().equals("PRIVADO")) {
                    try {
                        Notificacion notif = notificacionService.crearNotificacionGrupo(
                                usuarioId,
                                creadorId,
                                "<strong>" + creador.getNombreUsuario() + "</strong> te invitó al grupo <strong>" + grupo.getNombre() + "</strong>",
                                grupo.getId(),
                                "/chat"
                        );
                        log.info("📩 Notificación enviada a {} (ID: {})", invitado.getNombreUsuario(), notif.getId());
                    } catch (Exception e) {
                        log.error("❌ Error al crear notificación para {}: {}", invitado.getNombreUsuario(), e.getMessage());
                    }
                }
            }
        } else {
            log.info("ℹ️ No hay usuarios para invitar");
        }

        log.info("✅ Grupo creado con ID: {}", grupo.getId());
        return mapearADTO(grupo, creadorId);
    }

    // ===== LISTAR GRUPOS DEL USUARIO =====
    @Transactional(readOnly = true)
    public List<GrupoDTO> listarMisGrupos(Long usuarioId) {
        return grupoRepository.findGruposDeUsuario(usuarioId)
                .stream()
                .map(g -> mapearADTO(g, usuarioId))
                .collect(Collectors.toList());
    }

    // ===== LISTAR GRUPOS PÚBLICOS DISPONIBLES =====
    @Transactional(readOnly = true)
    public List<GrupoDTO> listarGruposPublicos(Long usuarioId) {
        return grupoRepository.findGruposPublicosDisponibles(usuarioId)
                .stream()
                .map(g -> mapearADTO(g, usuarioId))
                .collect(Collectors.toList());
    }

    // ===== OBTENER GRUPO POR ID =====
    @Transactional(readOnly = true)
    public GrupoDTO obtenerGrupo(Long grupoId, Long usuarioId) {
        GrupoChat grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Grupo no encontrado"));
        return mapearADTO(grupo, usuarioId);
    }

    // ===== INVITAR USUARIOS AL GRUPO =====
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

            // ✅ ENVIAR NOTIFICACIÓN si el grupo es privado (requiere aceptación)
            if (grupo.getTipo().equals("PRIVADO")) {
                notificacionService.crearNotificacionGrupo(
                        usuarioId,
                        invitadorId,
                        "<strong>" + invitador.getNombreUsuario() + "</strong> te invitó al grupo <strong>" + grupo.getNombre() + "</strong>",
                        grupoId,
                        "/chat"
                );
            }
        }
    }
    // ===== ACEPTAR INVITACIÓN =====
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

        log.info("✅ Usuario {} se unió al grupo {}", usuarioId, grupoId);
    }

    // ===== RECHAZAR INVITACIÓN =====
    @Transactional
    public void rechazarInvitacion(Long grupoId, Long usuarioId) {
        MiembroGrupo miembro = miembroRepository.findByGrupoIdAndUsuarioId(grupoId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Invitación no encontrada"));

        miembro.setEstado("RECHAZADO");
        miembroRepository.save(miembro);

        log.info("❌ Usuario {} rechazó la invitación al grupo {}", usuarioId, grupoId);
    }

    // ===== UNIRSE A GRUPO PÚBLICO =====
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

        log.info("✅ Usuario {} se unió al grupo público {}", usuarioId, grupoId);
    }

    // ===== SALIR DEL GRUPO =====
    @Transactional
    public void salirDelGrupo(Long grupoId, Long usuarioId) {
        MiembroGrupo miembro = miembroRepository.findByGrupoIdAndUsuarioId(grupoId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No perteneces a este grupo"));

        miembro.setEstado("SALIO");
        miembroRepository.save(miembro);

        log.info("👋 Usuario {} salió del grupo {}", usuarioId, grupoId);
    }

    // ===== LISTAR INVITACIONES PENDIENTES =====
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

    // ===== LISTAR MIEMBROS DEL GRUPO =====
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

    // ===== MAPEAR A DTO =====
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
    // ===== MENSAJES DEL GRUPO =====
    @Transactional(readOnly = true)
    public List<MensajeGrupoDTO> obtenerMensajes(Long grupoId) {
        Long usuarioId = null; // Será reemplazado en el controller
        return obtenerMensajes(grupoId, usuarioId);
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

        // Verificar que el usuario sea miembro activo
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