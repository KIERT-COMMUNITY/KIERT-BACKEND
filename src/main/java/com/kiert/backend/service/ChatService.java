package com.kiert.backend.service;

import com.kiert.backend.dto.*;
import com.kiert.backend.entity.*;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final UsuarioRepository usuarioRepository;
    private final MensajeRepository mensajeRepository;
    private final SolicitudContactoRepository solicitudRepository;
    private final StorageService storageService;
    private final SimpMessagingTemplate messagingTemplate;  // ✅ WEBSOCKET

    // ========== CONVERSACIONES ==========
    @Transactional(readOnly = true)
    public List<ConversacionDTO> listarConversaciones(Long usuarioId) {
        log.info("📋 Listando conversaciones para usuario: {}", usuarioId);

        List<Mensaje> mensajes = mensajeRepository.findTodosLosMensajesDeUsuario(usuarioId);

        if (mensajes.isEmpty()) {
            return new ArrayList<>();
        }

        return mensajes.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getEmisor().getId().equals(usuarioId) ? m.getReceptor().getId() : m.getEmisor().getId()
                ))
                .entrySet().stream()
                .map(entry -> {
                    Long otroUsuarioId = entry.getKey();
                    List<Mensaje> mensajesConUsuario = entry.getValue();

                    Mensaje ultimo = mensajesConUsuario.stream()
                            .max((m1, m2) -> m1.getFechaEnvio().compareTo(m2.getFechaEnvio()))
                            .orElse(null);

                    long noLeidos = mensajesConUsuario.stream()
                            .filter(m -> m.getReceptor().getId().equals(usuarioId) && !m.isLeido())
                            .count();

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
    @Transactional
    public List<MensajeChatDTO> obtenerMensajes(Long usuarioId, Long otroUsuarioId) {
        log.info("💬 Obteniendo mensajes entre {} y {}", usuarioId, otroUsuarioId);

        List<Mensaje> mensajes = mensajeRepository.findConversacion(usuarioId, otroUsuarioId);

        mensajes.stream()
                .filter(m -> m.getReceptor().getId().equals(usuarioId) && !m.isLeido())
                .forEach(m -> m.setLeido(true));
        mensajeRepository.saveAll(mensajes);

        return mensajes.stream()
                .map(m -> {
                    List<MensajeArchivoDTO> archivos = new ArrayList<>();
                    if (m.getUrlArchivo() != null) {
                        archivos.add(new MensajeArchivoDTO(
                                null,
                                m.getNombreArchivo(),
                                m.getUrlArchivo(),
                                "imagen",
                                null,
                                false
                        ));
                    }

                    return new MensajeChatDTO(
                            m.getId(),
                            m.getEmisor().getId(),
                            m.getContenido(),
                            m.getFechaEnvio(),
                            m.getEmisor().getId().equals(usuarioId),
                            archivos.isEmpty() ? null : archivos
                    );
                })
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = {"conversaciones", "mensajes"}, allEntries = true)
    public MensajeChatDTO enviarMensaje(Long emisorId, Long receptorId, String contenido) {
        log.info("📤 Enviando mensaje de {} a {}", emisorId, receptorId);

        Usuario emisor = usuarioRepository.findById(emisorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Emisor no encontrado"));
        Usuario receptor = usuarioRepository.findById(receptorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Receptor no encontrado"));

        Mensaje mensaje = Mensaje.builder()
                .emisor(emisor)
                .receptor(receptor)
                .contenido(contenido)
                .leido(false)
                .build();

        mensaje = mensajeRepository.save(mensaje);

        // ✅ CREAR DTO PARA ENVIAR
        MensajeChatDTO dto = new MensajeChatDTO(
                mensaje.getId(),
                mensaje.getEmisor().getId(),
                mensaje.getContenido(),
                mensaje.getFechaEnvio(),
                false,  // ⚠️ desde la perspectiva del receptor
                null
        );

        // ✅ ENVIAR POR WEBSOCKET AL RECEPTOR
        try {
            messagingTemplate.convertAndSendToUser(
                    receptorId.toString(),
                    "/queue/mensajes",
                    dto
            );
            log.info("📡 Mensaje enviado por WebSocket a usuario {}", receptorId);
        } catch (Exception e) {
            log.error("❌ Error al enviar por WebSocket: {}", e.getMessage());
        }

        // ✅ Devolver con propio=true para el emisor
        return new MensajeChatDTO(
                mensaje.getId(),
                mensaje.getEmisor().getId(),
                mensaje.getContenido(),
                mensaje.getFechaEnvio(),
                true,
                null
        );
    }

    @Transactional
    @CacheEvict(value = {"conversaciones", "mensajes"}, allEntries = true)
    public MensajeChatDTO enviarMensajeConArchivos(
            Long emisorId,
            Long receptorId,
            String contenido,
            List<MultipartFile> archivos) {

        log.info("📤 Enviando mensaje con archivos de {} a {}", emisorId, receptorId);

        Usuario emisor = usuarioRepository.findById(emisorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Emisor no encontrado"));
        Usuario receptor = usuarioRepository.findById(receptorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Receptor no encontrado"));

        Mensaje mensaje = Mensaje.builder()
                .emisor(emisor)
                .receptor(receptor)
                .contenido(contenido != null ? contenido : "")
                .leido(false)
                .build();

        mensaje = mensajeRepository.save(mensaje);

        List<MensajeArchivoDTO> archivosDTO = new ArrayList<>();

        if (archivos != null && !archivos.isEmpty()) {
            for (MultipartFile archivo : archivos) {
                try {
                    String url = storageService.subirArchivo(archivo);
                    String nombreArchivo = archivo.getOriginalFilename();
                    String tipoArchivo = determinarTipoArchivo(archivo);

                    mensaje.setUrlArchivo(url);
                    mensaje.setNombreArchivo(nombreArchivo);
                    mensaje.setTipoMensaje(tipoArchivo);
                    mensaje = mensajeRepository.save(mensaje);

                    archivosDTO.add(new MensajeArchivoDTO(
                            null,
                            nombreArchivo,
                            url,
                            tipoArchivo,
                            (int) (archivo.getSize() / 1024),
                            false
                    ));
                } catch (Exception e) {
                    log.error("❌ Error al subir archivo: {}", e.getMessage());
                }
            }
        }

        MensajeChatDTO dto = new MensajeChatDTO(
                mensaje.getId(),
                mensaje.getEmisor().getId(),
                mensaje.getContenido(),
                mensaje.getFechaEnvio(),
                false,
                archivosDTO.isEmpty() ? null : archivosDTO
        );

        // ✅ ENVIAR POR WEBSOCKET
        try {
            messagingTemplate.convertAndSendToUser(
                    receptorId.toString(),
                    "/queue/mensajes",
                    dto
            );
        } catch (Exception e) {
            log.error("❌ Error WebSocket: {}", e.getMessage());
        }

        return new MensajeChatDTO(
                mensaje.getId(),
                mensaje.getEmisor().getId(),
                mensaje.getContenido(),
                mensaje.getFechaEnvio(),
                true,
                archivosDTO.isEmpty() ? null : archivosDTO
        );
    }

    private String determinarTipoArchivo(MultipartFile archivo) {
        String nombre = archivo.getOriginalFilename();
        if (nombre == null) return "documento";

        String extension = nombre.substring(nombre.lastIndexOf(".") + 1).toLowerCase();

        return switch (extension) {
            case "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg" -> "imagen";
            case "pdf" -> "pdf";
            case "doc", "docx" -> "word";
            case "xls", "xlsx" -> "excel";
            case "ppt", "pptx" -> "powerpoint";
            case "zip", "rar" -> "comprimido";
            case "txt" -> "texto";
            default -> "documento";
        };
    }

    @Transactional
    @CacheEvict(value = {"conversaciones", "mensajes"}, allEntries = true)
    public void marcarMensajesComoLeidos(Long usuarioId, Long otroUsuarioId) {
        log.info("📬 Marcando mensajes como leídos entre {} y {}", usuarioId, otroUsuarioId);

        List<Mensaje> mensajesNoLeidos = mensajeRepository.findConversacionNoLeidos(usuarioId, otroUsuarioId);
        if (!mensajesNoLeidos.isEmpty()) {
            mensajesNoLeidos.forEach(m -> m.setLeido(true));
            mensajeRepository.saveAll(mensajesNoLeidos);
        }
    }

    // ========== SOLICITUDES (sin cambios) ==========
    // ... (mantén el resto igual)

    @Transactional(readOnly = true)
    public List<SolicitudContactoDTO> listarSolicitudes(Long usuarioId) {
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
    @CacheEvict(value = {"solicitudes", "conversaciones", "contactos"}, allEntries = true)
    public SolicitudContactoDTO enviarSolicitud(Long emisorId, Long receptorId) {
        if (emisorId.equals(receptorId)) {
            throw new IllegalArgumentException("No puedes enviarte una solicitud a ti mismo");
        }

        Usuario emisor = usuarioRepository.findById(emisorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Emisor no encontrado"));
        Usuario receptor = usuarioRepository.findById(receptorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Receptor no encontrado"));

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
    @CacheEvict(value = {"solicitudes", "conversaciones", "contactos"}, allEntries = true)
    public void aceptarSolicitud(Long solicitudId, Long usuarioId) {
        SolicitudContacto solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud no encontrada"));

        if (!solicitud.getReceptor().getId().equals(usuarioId)) {
            throw new SecurityException("No tienes permiso");
        }

        solicitud.setEstado(SolicitudContacto.EstadoSolicitud.ACEPTADA);
        solicitud.setFechaRespuesta(Instant.now());
        solicitudRepository.save(solicitud);

        Usuario emisor = solicitud.getEmisor();
        Usuario receptor = solicitud.getReceptor();

        Mensaje mensaje1 = Mensaje.builder()
                .emisor(emisor).receptor(receptor)
                .contenido("¡Hola! Ahora somos contactos.")
                .leido(false).build();
        mensajeRepository.save(mensaje1);

        Mensaje mensaje2 = Mensaje.builder()
                .emisor(receptor).receptor(emisor)
                .contenido("¡Hola! Gracias por aceptar.")
                .leido(false).build();
        mensajeRepository.save(mensaje2);
    }

    @Transactional
    @CacheEvict(value = {"solicitudes", "conversaciones", "contactos"}, allEntries = true)
    public void rechazarSolicitud(Long solicitudId, Long usuarioId) {
        SolicitudContacto solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud no encontrada"));

        if (!solicitud.getReceptor().getId().equals(usuarioId)) {
            throw new SecurityException("No tienes permiso");
        }

        solicitud.setEstado(SolicitudContacto.EstadoSolicitud.RECHAZADA);
        solicitud.setFechaRespuesta(Instant.now());
        solicitudRepository.save(solicitud);
    }

    public boolean sonContactos(Long usuario1, Long usuario2) {
        return solicitudRepository.sonContactos(usuario1, usuario2);
    }

    @Transactional(readOnly = true)
    public List<UsuarioDisponibleDTO> listarUsuariosDisponibles(Long usuarioId) {
        List<Long> contactosIds = mensajeRepository.findContactosId(usuarioId);
        List<Long> solicitudesEnviadasIds = solicitudRepository.findByEmisorIdAndEstado(
                        usuarioId, SolicitudContacto.EstadoSolicitud.PENDIENTE)
                .stream().map(s -> s.getReceptor().getId()).collect(Collectors.toList());
        List<Long> solicitudesRecibidasIds = solicitudRepository.findByReceptorIdAndEstado(
                        usuarioId, SolicitudContacto.EstadoSolicitud.PENDIENTE)
                .stream().map(s -> s.getEmisor().getId()).collect(Collectors.toList());

        return usuarioRepository.findAll().stream()
                .filter(u -> !u.getId().equals(usuarioId))
                .filter(u -> !contactosIds.contains(u.getId()))
                .filter(u -> !solicitudesEnviadasIds.contains(u.getId()))
                .filter(u -> !solicitudesRecibidasIds.contains(u.getId()))
                .map(u -> new UsuarioDisponibleDTO(
                        u.getId(),
                        u.getNombreUsuario(),
                        u.getFotoPerfilUrl()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = {"conversaciones", "mensajes", "contactos"}, allEntries = true)
    public void eliminarContacto(Long usuarioId, Long contactoId) {
        List<Mensaje> mensajes = mensajeRepository.findConversacion(usuarioId, contactoId);
        if (!mensajes.isEmpty()) {
            mensajeRepository.deleteAll(mensajes);
        }

        solicitudRepository.findByEmisorIdAndReceptorIdAndEstado(
                        usuarioId, contactoId, SolicitudContacto.EstadoSolicitud.ACEPTADA)
                .ifPresent(solicitud -> {
                    solicitud.setEstado(SolicitudContacto.EstadoSolicitud.RECHAZADA);
                    solicitud.setFechaRespuesta(Instant.now());
                    solicitudRepository.save(solicitud);
                });

        solicitudRepository.findByEmisorIdAndReceptorIdAndEstado(
                        contactoId, usuarioId, SolicitudContacto.EstadoSolicitud.ACEPTADA)
                .ifPresent(solicitud -> {
                    solicitud.setEstado(SolicitudContacto.EstadoSolicitud.RECHAZADA);
                    solicitud.setFechaRespuesta(Instant.now());
                    solicitudRepository.save(solicitud);
                });
    }

    public long obtenerMensajesNoLeidos(Long usuarioId) {
        return mensajeRepository.countByReceptorIdAndLeidoFalse(usuarioId);
    }
}