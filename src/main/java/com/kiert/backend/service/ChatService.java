package com.kiert.backend.service;

import com.kiert.backend.dto.*;
import com.kiert.backend.entity.*;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    // ========== CONVERSACIONES ==========
    @Transactional(readOnly = true)
    public List<ConversacionDTO> listarConversaciones(Long usuarioId) {
        log.info("📋 Listando conversaciones para usuario: {}", usuarioId);

        List<Mensaje> mensajes = mensajeRepository.findTodosLosMensajesDeUsuario(usuarioId);

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
    @Transactional(readOnly = true)
    public List<MensajeChatDTO> obtenerMensajes(Long usuarioId, Long otroUsuarioId) {
        log.info("💬 Obteniendo mensajes entre {} y {}", usuarioId, otroUsuarioId);

        List<Mensaje> mensajes = mensajeRepository.findConversacion(usuarioId, otroUsuarioId);

        mensajes.stream()
                .filter(m -> m.getReceptor().getId().equals(usuarioId) && !m.isLeido())
                .forEach(m -> m.setLeido(true));
        mensajeRepository.saveAll(mensajes);

        return mensajes.stream()
                .map(m -> {
                    // ✅ Crear lista de archivos (si el mensaje tiene archivo)
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
                            archivos
                    );
                })
                .collect(Collectors.toList());
    }

    // ========== ENVIAR MENSAJE (SOLO TEXTO) ==========
    @Transactional
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

        return new MensajeChatDTO(
                mensaje.getId(),
                mensaje.getEmisor().getId(),
                mensaje.getContenido(),
                mensaje.getFechaEnvio(),
                true,
                null
        );
    }

    // ========== ENVIAR MENSAJE CON ARCHIVOS ==========
    @Transactional
    public MensajeChatDTO enviarMensajeConArchivos(
            Long emisorId,
            Long receptorId,
            String contenido,
            List<MultipartFile> archivos) {

        log.info("📤 Enviando mensaje de {} a {} con {} archivos", emisorId, receptorId,
                archivos != null ? archivos.size() : 0);

        Usuario emisor = usuarioRepository.findById(emisorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Emisor no encontrado"));
        Usuario receptor = usuarioRepository.findById(receptorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Receptor no encontrado"));

        // Guardar mensaje primero
        Mensaje mensaje = Mensaje.builder()
                .emisor(emisor)
                .receptor(receptor)
                .contenido(contenido != null ? contenido : "")
                .leido(false)
                .build();

        mensaje = mensajeRepository.save(mensaje);
        log.info("✅ Mensaje guardado con ID: {}", mensaje.getId());

        // ✅ Variables para almacenar la URL del archivo
        String urlArchivo = null;
        String nombreArchivo = null;

        // Subir archivos a Cloudinary y actualizar mensaje
        if (archivos != null && !archivos.isEmpty()) {
            for (MultipartFile archivo : archivos) {
                try {
                    String url = storageService.subirArchivo(archivo);
                    log.info("✅ Archivo subido a Cloudinary: {}", url);

                    // ✅ Guardar la URL y nombre en el mensaje
                    urlArchivo = url;
                    nombreArchivo = archivo.getOriginalFilename();

                    // ✅ Actualizar el mensaje con la información del archivo
                    mensaje.setUrlArchivo(url);
                    mensaje.setNombreArchivo(archivo.getOriginalFilename());
                    mensaje.setTipoMensaje("IMAGEN");
                    mensaje = mensajeRepository.save(mensaje);

                } catch (Exception e) {
                    log.error("❌ Error al subir archivo: {}", e.getMessage());
                }
            }
        }

        // ✅ Crear la lista de archivos para el DTO
        List<MensajeArchivoDTO> archivosDTO = new ArrayList<>();
        if (urlArchivo != null) {
            archivosDTO.add(new MensajeArchivoDTO(
                    null,
                    nombreArchivo,
                    urlArchivo,
                    "imagen",
                    null,
                    false
            ));
        }

        return new MensajeChatDTO(
                mensaje.getId(),
                mensaje.getEmisor().getId(),
                mensaje.getContenido(),
                mensaje.getFechaEnvio(),
                true,
                archivosDTO
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
        solicitud.setFechaRespuesta(Instant.now());
        solicitudRepository.save(solicitud);

        Usuario emisor = solicitud.getEmisor();
        Usuario receptor = solicitud.getReceptor();

        Mensaje mensaje1 = Mensaje.builder()
                .emisor(emisor)
                .receptor(receptor)
                .contenido("¡Hola! Ahora somos contactos. ¡Bienvenido al chat!")
                .leido(false)
                .build();
        mensajeRepository.save(mensaje1);

        Mensaje mensaje2 = Mensaje.builder()
                .emisor(receptor)
                .receptor(emisor)
                .contenido("¡Hola! Ahora somos contactos. ¡Gracias por aceptar mi solicitud!")
                .leido(false)
                .build();
        mensajeRepository.save(mensaje2);
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
        solicitud.setFechaRespuesta(Instant.now());
        solicitudRepository.save(solicitud);
    }

    public boolean sonContactos(Long usuario1, Long usuario2) {
        return solicitudRepository.sonContactos(usuario1, usuario2);
    }

    @Transactional(readOnly = true)
    public List<UsuarioDisponibleDTO> listarUsuariosDisponibles(Long usuarioId) {
        log.info("📋 Listando usuarios disponibles para {}", usuarioId);

        List<Long> contactosIds = mensajeRepository.findContactosId(usuarioId);

        List<Long> solicitudesIds = solicitudRepository.findByEmisorIdAndEstado(usuarioId, SolicitudContacto.EstadoSolicitud.PENDIENTE)
                .stream()
                .map(s -> s.getReceptor().getId())
                .collect(Collectors.toList());

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

    @Transactional
    public void eliminarContacto(Long usuarioId, Long contactoId) {
        log.info("🗑️ Eliminando contacto {} para usuario {}", contactoId, usuarioId);

        List<Mensaje> mensajes = mensajeRepository.findConversacion(usuarioId, contactoId);
        mensajeRepository.deleteAll(mensajes);

        solicitudRepository.findByEmisorIdAndReceptorIdAndEstado(
                        usuarioId, contactoId, SolicitudContacto.EstadoSolicitud.ACEPTADA)
                .ifPresent(solicitud -> {
                    solicitud.setEstado(SolicitudContacto.EstadoSolicitud.RECHAZADA);
                    solicitudRepository.save(solicitud);
                });
    }
}