package com.kiert.backend.service;

import com.kiert.backend.dto.*;
import com.kiert.backend.entity.*;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final UsuarioRepository usuarioRepository;
    private final MensajeRepository mensajeRepository;
    private final SolicitudContactoRepository solicitudRepository;

    // ========== CONVERSACIONES ==========
    @Transactional(readOnly = true)
    public List<ConversacionDTO> listarConversaciones(Long usuarioId) {
        log.info("📋 Listando conversaciones para usuario: {}", usuarioId);

        // Obtener todos los mensajes del usuario
        List<Mensaje> mensajes = mensajeRepository.findTodosLosMensajesDeUsuario(usuarioId);

        // Agrupar por interlocutor y obtener último mensaje
        return mensajes.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getEmisor().getId().equals(usuarioId) ? m.getReceptor().getId() : m.getEmisor().getId()
                ))
                .entrySet().stream()
                .map(entry -> {
                    Long otroUsuarioId = entry.getKey();
                    List<Mensaje> mensajesConUsuario = entry.getValue();

                    // Obtener el último mensaje
                    Mensaje ultimo = mensajesConUsuario.stream()
                            .max((m1, m2) -> m1.getFechaEnvio().compareTo(m2.getFechaEnvio()))
                            .orElse(null);

                    // Contar no leídos
                    long noLeidos = mensajesConUsuario.stream()
                            .filter(m -> m.getReceptor().getId().equals(usuarioId) && !m.isLeido())
                            .count();

                    // Obtener información del otro usuario
                    Usuario otroUsuario = usuarioRepository.findById(otroUsuarioId)
                            .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

                    return new ConversacionDTO(
                            otroUsuario.getId(),
                            otroUsuario.getNombreUsuario(),
                            otroUsuario.getFotoPerfilUrl(),
                            ultimo != null ? ultimo.getContenido() : null,
                            ultimo != null ? ultimo.getFechaEnvio().toString() : null,
                            noLeidos
                    );
                })
                .sorted((c1, c2) -> {
                    if (c1.ultimoMensaje() == null) return 1;
                    if (c2.ultimoMensaje() == null) return -1;
                    return c2.ultimoMensaje().compareTo(c1.ultimoMensaje());
                })
                .collect(Collectors.toList());
    }

    // ========== MENSAJES ==========
    @Transactional(readOnly = true)
    public List<MensajeChatDTO> obtenerMensajes(Long usuarioId, Long otroUsuarioId) {
        log.info("💬 Obteniendo mensajes entre {} y {}", usuarioId, otroUsuarioId);

        // Marcar como leídos los mensajes del otro usuario
        List<Mensaje> mensajes = mensajeRepository.findConversacion(usuarioId, otroUsuarioId);

        // Marcar como leídos
        mensajes.stream()
                .filter(m -> m.getReceptor().getId().equals(usuarioId) && !m.isLeido())
                .forEach(m -> m.setLeido(true));
        mensajeRepository.saveAll(mensajes);

        return mensajes.stream()
                .map(m -> new MensajeChatDTO(
                        m.getId(),
                        m.getEmisor().getId(),
                        m.getContenido(),
                        m.getFechaEnvio(),
                        m.getEmisor().getId().equals(usuarioId)
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public MensajeChatDTO enviarMensaje(Long emisorId, Long receptorId, String contenido) {
        log.info("📤 Enviando mensaje de {} a {}", emisorId, receptorId);

        Usuario emisor = usuarioRepository.findById(emisorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Emisor no encontrado"));
        Usuario receptor = usuarioRepository.findById(receptorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Receptor no encontrado"));

        // Verificar que son contactos (opcional)
        // Aquí podrías verificar si hay una solicitud aceptada entre ellos

        Mensaje mensaje = Mensaje.builder()
                .emisor(emisor)
                .receptor(receptor)
                .contenido(contenido)
                .leido(false)
                .build();

        mensaje = mensajeRepository.save(mensaje);

        return new MensajeChatDTO(
                mensaje.getId(),
                mensaje.getEmisor().getId(),
                mensaje.getContenido(),
                mensaje.getFechaEnvio(),
                true
        );
    }

    // ========== SOLICITUDES ==========
    @Transactional(readOnly = true)
    public List<SolicitudContactoDTO> listarSolicitudes(Long usuarioId) {
        log.info("📋 Listando solicitudes para usuario: {}", usuarioId);

        List<SolicitudContacto> solicitudes = solicitudRepository.findByReceptorIdAndEstado(
                usuarioId, SolicitudContacto.EstadoSolicitud.PENDIENTE);

        return solicitudes.stream()
                .map(s -> new SolicitudContactoDTO(
                        s.getId(),
                        s.getEmisor().getId(),
                        s.getEmisor().getNombreUsuario(),
                        s.getEmisor().getFotoPerfilUrl(),
                        s.getEstado().name(),
                        s.getFechaSolicitud()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public SolicitudContactoDTO enviarSolicitud(Long emisorId, Long receptorId) {
        log.info("📤 Enviando solicitud de {} a {}", emisorId, receptorId);

        if (emisorId.equals(receptorId)) {
            throw new IllegalArgumentException("No puedes enviarte una solicitud a ti mismo");
        }

        Usuario emisor = usuarioRepository.findById(emisorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Emisor no encontrado"));
        Usuario receptor = usuarioRepository.findById(receptorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Receptor no encontrado"));

        // Verificar si ya existe una solicitud pendiente
        if (solicitudRepository.existsByEmisorIdAndReceptorIdAndEstado(
                emisorId, receptorId, SolicitudContacto.EstadoSolicitud.PENDIENTE)) {
            throw new IllegalStateException("Ya existe una solicitud pendiente");
        }

        SolicitudContacto solicitud = SolicitudContacto.builder()
                .emisor(emisor)
                .receptor(receptor)
                .estado(SolicitudContacto.EstadoSolicitud.PENDIENTE)
                .build();

        solicitud = solicitudRepository.save(solicitud);

        return new SolicitudContactoDTO(
                solicitud.getId(),
                solicitud.getEmisor().getId(),
                solicitud.getEmisor().getNombreUsuario(),
                solicitud.getEmisor().getFotoPerfilUrl(),
                solicitud.getEstado().name(),
                solicitud.getFechaSolicitud()
        );
    }

    @Transactional
    public void aceptarSolicitud(Long solicitudId, Long usuarioId) {
        log.info("✅ Aceptando solicitud {} por usuario {}", solicitudId, usuarioId);

        SolicitudContacto solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud no encontrada"));

        if (!solicitud.getReceptor().getId().equals(usuarioId)) {
            throw new SecurityException("No tienes permiso para aceptar esta solicitud");
        }

        solicitud.setEstado(SolicitudContacto.EstadoSolicitud.ACEPTADA);
        solicitudRepository.save(solicitud);
    }

    @Transactional
    public void rechazarSolicitud(Long solicitudId, Long usuarioId) {
        log.info("❌ Rechazando solicitud {} por usuario {}", solicitudId, usuarioId);

        SolicitudContacto solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud no encontrada"));

        if (!solicitud.getReceptor().getId().equals(usuarioId)) {
            throw new SecurityException("No tienes permiso para rechazar esta solicitud");
        }

        solicitud.setEstado(SolicitudContacto.EstadoSolicitud.RECHAZADA);
        solicitudRepository.save(solicitud);
    }

    // ========== USUARIOS DISPONIBLES ==========
    @Transactional(readOnly = true)
    public List<UsuarioDisponibleDTO> listarUsuariosDisponibles(Long usuarioId) {
        log.info("📋 Listando usuarios disponibles para {}", usuarioId);

        // Obtener IDs de usuarios con los que ya tiene conversación
        List<Long> contactosIds = mensajeRepository.findContactosId(usuarioId);

        // Obtener solicitudes pendientes
        List<Long> solicitudesIds = solicitudRepository.findByEmisorIdAndEstado(usuarioId, SolicitudContacto.EstadoSolicitud.PENDIENTE)
                .stream()
                .map(s -> s.getReceptor().getId())
                .collect(Collectors.toList());

        // Obtener todos los usuarios excepto el actual, los contactos y los que tienen solicitud pendiente
        return usuarioRepository.findAll().stream()
                .filter(u -> !u.getId().equals(usuarioId))
                .filter(u -> !contactosIds.contains(u.getId()))
                .filter(u -> !solicitudesIds.contains(u.getId()))
                .map(u -> new UsuarioDisponibleDTO(
                        u.getId(),
                        u.getNombreUsuario(),
                        u.getFotoPerfilUrl()
                ))
                .collect(Collectors.toList());
    }

    // ========== ELIMINAR CONTACTO ==========
    @Transactional
    public void eliminarContacto(Long usuarioId, Long contactoId) {
        log.info("🗑️ Eliminando contacto {} para usuario {}", contactoId, usuarioId);

        // Eliminar todos los mensajes entre ambos
        List<Mensaje> mensajes = mensajeRepository.findConversacion(usuarioId, contactoId);
        mensajeRepository.deleteAll(mensajes);
    }
}