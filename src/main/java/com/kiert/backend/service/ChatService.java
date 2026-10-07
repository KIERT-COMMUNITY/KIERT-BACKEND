package com.kiert.backend.service;

import com.kiert.backend.dto.ArchivoSubidoDTO;
import com.kiert.backend.dto.ConversacionDTO;
import com.kiert.backend.dto.MensajeArchivoDTO;
import com.kiert.backend.dto.MensajeChatDTO;
import com.kiert.backend.dto.SolicitudContactoDTO;
import com.kiert.backend.dto.UsuarioDisponibleDTO;
import com.kiert.backend.entity.Mensaje;
import com.kiert.backend.entity.MensajeArchivo;
import com.kiert.backend.entity.PersonalizacionUsuario;
import com.kiert.backend.entity.SolicitudContacto;
import com.kiert.backend.entity.Usuario;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.repository.BloqueoRepository;
import com.kiert.backend.repository.MensajeArchivoRepository;
import com.kiert.backend.repository.MensajeRepository;
import com.kiert.backend.repository.NotificacionRepository;
import com.kiert.backend.repository.PersonalizacionRepository;
import com.kiert.backend.repository.SolicitudContactoRepository;
import com.kiert.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final UsuarioRepository usuarioRepository;
    private final MensajeRepository mensajeRepository;
    private final MensajeArchivoRepository mensajeArchivoRepository;
    private final SolicitudContactoRepository solicitudRepository;
    private final NotificacionRepository notificacionRepository;
    private final BloqueoRepository bloqueoRepository;
    private final PersonalizacionRepository personalizacionRepository;
    private final ArchivoChatService archivoChatService;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional(readOnly = true)
    public List<ConversacionDTO> listarConversaciones(Long usuarioId) {
        List<Long> bloqueadosIds = bloqueoRepository.findUsuariosBloqueadosIds(usuarioId);
        List<Long> bloqueadoresIds = bloqueoRepository.findUsuariosQueMeBloquearonIds(usuarioId);
        List<Mensaje> mensajes = mensajeRepository.findTodosLosMensajesDeUsuario(usuarioId);

        if (mensajes.isEmpty()) {
            return new ArrayList<>();
        }

        return mensajes.stream()
                .filter(mensaje -> {
                    Long otroId = mensaje.getEmisor().getId().equals(usuarioId)
                            ? mensaje.getReceptor().getId()
                            : mensaje.getEmisor().getId();
                    return !bloqueadosIds.contains(otroId)
                            && !bloqueadoresIds.contains(otroId);
                })
                .collect(Collectors.groupingBy(mensaje ->
                        mensaje.getEmisor().getId().equals(usuarioId)
                                ? mensaje.getReceptor().getId()
                                : mensaje.getEmisor().getId()
                ))
                .entrySet()
                .stream()
                .map(entry -> crearConversacionDTO(usuarioId, entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(
                        ConversacionDTO::ultimoMensajeFecha,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();
    }

    @Transactional
    public List<MensajeChatDTO> obtenerMensajes(Long usuarioId, Long otroUsuarioId) {
        List<Mensaje> mensajes = mensajeRepository.findConversacion(usuarioId, otroUsuarioId);
        Instant fechaLectura = Instant.now();

        List<Mensaje> noLeidos = mensajes.stream()
                .filter(mensaje -> mensaje.getEmisor().getId().equals(otroUsuarioId))
                .filter(mensaje -> mensaje.getReceptor().getId().equals(usuarioId))
                .filter(mensaje -> !mensaje.isLeido())
                .toList();

        if (!noLeidos.isEmpty()) {
            noLeidos.forEach(mensaje -> {
                mensaje.setLeido(true);
                mensaje.setFechaLeido(fechaLectura);
            });
            mensajeRepository.saveAll(noLeidos);
        }

        return mensajes.stream()
                .map(mensaje -> convertirMensajeDTO(mensaje, usuarioId))
                .toList();
    }

    @Transactional
    @CacheEvict(value = {"conversaciones", "mensajes"}, allEntries = true)
    public MensajeChatDTO enviarMensaje(Long emisorId, Long receptorId, String contenido) {
        return enviarMensajeConArchivos(emisorId, receptorId, contenido, List.of());
    }

    @Transactional
    @CacheEvict(value = {"conversaciones", "mensajes"}, allEntries = true)
    public MensajeChatDTO enviarMensajeConArchivos(
            Long emisorId,
            Long receptorId,
            String contenido,
            List<MultipartFile> archivos
    ) {
        validarEnvioPermitido(emisorId, receptorId);

        String contenidoNormalizado = normalizarContenido(contenido);
        List<MultipartFile> archivosValidos = normalizarArchivos(archivos);

        if (contenidoNormalizado == null && archivosValidos.isEmpty()) {
            throw new IllegalArgumentException("El mensaje debe contener texto o al menos un archivo");
        }

        Usuario emisor = obtenerUsuario(emisorId, "Emisor no encontrado");
        Usuario receptor = obtenerUsuario(receptorId, "Receptor no encontrado");
        List<ArchivoSubidoDTO> archivosSubidos = new ArrayList<>();

        try {
            if (!archivosValidos.isEmpty()) {
                archivosSubidos = archivoChatService.validarYSubir(
                        archivosValidos,
                        "chat/privado/" + emisorId + "-" + receptorId
                );
            }

            Mensaje mensaje = Mensaje.builder()
                    .emisor(emisor)
                    .receptor(receptor)
                    .contenido(contenidoNormalizado)
                    .tipoMensaje(determinarTipoMensaje(contenidoNormalizado, archivosSubidos))
                    .leido(false)
                    .eliminado(false)
                    .build();

            if (!archivosSubidos.isEmpty()) {
                ArchivoSubidoDTO primero = archivosSubidos.get(0);
                mensaje.setUrlArchivo(primero.secureUrl());
                mensaje.setNombreArchivo(primero.nombreOriginal());
            }

            mensaje = mensajeRepository.save(mensaje);

            for (ArchivoSubidoDTO subido : archivosSubidos) {
                mensaje.agregarArchivo(crearEntidadArchivo(subido));
            }

            if (!archivosSubidos.isEmpty()) {
                mensaje = mensajeRepository.save(mensaje);
            }

            MensajeChatDTO destinatario = convertirMensajeDTO(mensaje, receptorId);
            enviarPorWebSocket(receptorId, destinatario);

            return convertirMensajeDTO(mensaje, emisorId);
        } catch (RuntimeException exception) {
            if (!archivosSubidos.isEmpty()) {
                archivoChatService.eliminarArchivos(archivosSubidos);
            }
            throw exception;
        }
    }

    @Transactional
    @CacheEvict(value = {"conversaciones", "mensajes"}, allEntries = true)
    public void marcarMensajesComoLeidos(Long usuarioId, Long otroUsuarioId) {
        List<Mensaje> mensajesNoLeidos = mensajeRepository.findConversacionNoLeidos(
                usuarioId,
                otroUsuarioId
        );

        if (mensajesNoLeidos.isEmpty()) {
            return;
        }

        Instant fechaLectura = Instant.now();
        mensajesNoLeidos.forEach(mensaje -> {
            mensaje.setLeido(true);
            mensaje.setFechaLeido(fechaLectura);
        });
        mensajeRepository.saveAll(mensajesNoLeidos);
    }

    @Transactional(readOnly = true)
    public List<SolicitudContactoDTO> listarSolicitudes(Long usuarioId) {
        return solicitudRepository.findByReceptorIdAndEstado(
                        usuarioId,
                        SolicitudContacto.EstadoSolicitud.PENDIENTE
                )
                .stream()
                .map(solicitud -> new SolicitudContactoDTO(
                        solicitud.getId(),
                        solicitud.getEmisor().getId(),
                        solicitud.getEmisor().getNombreUsuario(),
                        solicitud.getEmisor().getFotoPerfilUrl(),
                        solicitud.getEstado().name(),
                        solicitud.getFechaSolicitud()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SolicitudContactoDTO> listarSolicitudesEnviadas(Long usuarioId) {
        return solicitudRepository.findByEmisorIdAndEstado(
                        usuarioId,
                        SolicitudContacto.EstadoSolicitud.PENDIENTE
                )
                .stream()
                .map(solicitud -> new SolicitudContactoDTO(
                        solicitud.getId(),
                        solicitud.getReceptor().getId(),
                        solicitud.getReceptor().getNombreUsuario(),
                        solicitud.getReceptor().getFotoPerfilUrl(),
                        solicitud.getEstado().name(),
                        solicitud.getFechaSolicitud()
                ))
                .toList();
    }

    @Transactional
    @CacheEvict(value = {"solicitudes", "conversaciones", "contactos"}, allEntries = true)
    public SolicitudContactoDTO enviarSolicitud(Long emisorId, Long receptorId) {
        if (emisorId.equals(receptorId)) {
            throw new IllegalArgumentException("No puedes enviarte una solicitud a ti mismo");
        }

        validarEnvioPermitido(emisorId, receptorId);

        Usuario emisor = obtenerUsuario(emisorId, "Emisor no encontrado");
        Usuario receptor = obtenerUsuario(receptorId, "Receptor no encontrado");

        if (sonContactos(emisorId, receptorId)) {
            throw new IllegalStateException("Ya son contactos");
        }

        if (solicitudRepository.existsByEmisorIdAndReceptorIdAndEstado(
                emisorId,
                receptorId,
                SolicitudContacto.EstadoSolicitud.PENDIENTE
        )) {
            throw new IllegalStateException("Ya enviaste una solicitud a este usuario");
        }

        if (solicitudRepository.existsByEmisorIdAndReceptorIdAndEstado(
                receptorId,
                emisorId,
                SolicitudContacto.EstadoSolicitud.PENDIENTE
        )) {
            throw new IllegalStateException(
                    "Este usuario ya te envio una solicitud. Revisa tus solicitudes."
            );
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
        SolicitudContacto solicitud = obtenerSolicitud(solicitudId);
        validarReceptorSolicitud(solicitud, usuarioId);

        solicitud.setEstado(SolicitudContacto.EstadoSolicitud.ACEPTADA);
        solicitud.setFechaRespuesta(Instant.now());
        solicitudRepository.save(solicitud);

        notificacionRepository.marcarNotificacionesSolicitudComoLeidas(
                usuarioId,
                solicitud.getEmisor().getId()
        );

        Usuario emisor = solicitud.getEmisor();
        Usuario receptor = solicitud.getReceptor();

        mensajeRepository.save(Mensaje.builder()
                .emisor(emisor)
                .receptor(receptor)
                .contenido("Hola, ahora somos contactos.")
                .tipoMensaje("TEXTO")
                .leido(false)
                .build());

        mensajeRepository.save(Mensaje.builder()
                .emisor(receptor)
                .receptor(emisor)
                .contenido("Hola, gracias por aceptar.")
                .tipoMensaje("TEXTO")
                .leido(false)
                .build());
    }

    @Transactional
    @CacheEvict(value = {"solicitudes", "conversaciones", "contactos"}, allEntries = true)
    public void rechazarSolicitud(Long solicitudId, Long usuarioId) {
        SolicitudContacto solicitud = obtenerSolicitud(solicitudId);
        validarReceptorSolicitud(solicitud, usuarioId);

        solicitud.setEstado(SolicitudContacto.EstadoSolicitud.RECHAZADA);
        solicitud.setFechaRespuesta(Instant.now());
        solicitudRepository.save(solicitud);

        notificacionRepository.marcarNotificacionesSolicitudComoLeidas(
                usuarioId,
                solicitud.getEmisor().getId()
        );
    }

    public boolean sonContactos(Long usuario1, Long usuario2) {
        return solicitudRepository.sonContactos(usuario1, usuario2);
    }

    @Transactional(readOnly = true)
    public List<UsuarioDisponibleDTO> listarUsuariosDisponibles(Long usuarioId) {
        List<Long> contactosIds = mensajeRepository.findContactosId(usuarioId);
        List<Long> solicitudesEnviadasIds = solicitudRepository.findByEmisorIdAndEstado(
                        usuarioId,
                        SolicitudContacto.EstadoSolicitud.PENDIENTE
                )
                .stream()
                .map(solicitud -> solicitud.getReceptor().getId())
                .toList();
        List<Long> solicitudesRecibidasIds = solicitudRepository.findByReceptorIdAndEstado(
                        usuarioId,
                        SolicitudContacto.EstadoSolicitud.PENDIENTE
                )
                .stream()
                .map(solicitud -> solicitud.getEmisor().getId())
                .toList();
        List<Long> bloqueadosIds = bloqueoRepository.findUsuariosBloqueadosIds(usuarioId);
        List<Long> bloqueadoresIds = bloqueoRepository.findUsuariosQueMeBloquearonIds(usuarioId);

        return usuarioRepository.findAll()
                .stream()
                .filter(usuario -> !usuario.getId().equals(usuarioId))
                .filter(usuario -> !contactosIds.contains(usuario.getId()))
                .filter(usuario -> !solicitudesEnviadasIds.contains(usuario.getId()))
                .filter(usuario -> !solicitudesRecibidasIds.contains(usuario.getId()))
                .filter(usuario -> !bloqueadosIds.contains(usuario.getId()))
                .filter(usuario -> !bloqueadoresIds.contains(usuario.getId()))
                .map(usuario -> new UsuarioDisponibleDTO(
                        usuario.getId(),
                        usuario.getNombreUsuario(),
                        usuario.getFotoPerfilUrl()
                ))
                .toList();
    }

    @Transactional
    @CacheEvict(value = {"conversaciones", "mensajes", "contactos"}, allEntries = true)
    public void eliminarContacto(Long usuarioId, Long contactoId) {
        List<Mensaje> mensajes = mensajeRepository.findConversacion(usuarioId, contactoId);

        if (!mensajes.isEmpty()) {
            mensajes.forEach(mensaje -> {
                mensaje.setEliminado(true);
                mensaje.setFechaEliminacion(Instant.now());
            });
            mensajeRepository.saveAll(mensajes);
        }

        solicitudRepository.findByEmisorIdAndReceptorIdAndEstado(
                usuarioId,
                contactoId,
                SolicitudContacto.EstadoSolicitud.ACEPTADA
        ).ifPresent(this::marcarSolicitudComoRechazada);

        solicitudRepository.findByEmisorIdAndReceptorIdAndEstado(
                contactoId,
                usuarioId,
                SolicitudContacto.EstadoSolicitud.ACEPTADA
        ).ifPresent(this::marcarSolicitudComoRechazada);
    }

    @Transactional(readOnly = true)
    public long obtenerMensajesNoLeidos(Long usuarioId) {
        return mensajeRepository.countByReceptorIdAndLeidoFalseAndEliminadoFalse(usuarioId);
    }

    private ConversacionDTO crearConversacionDTO(
            Long usuarioId,
            Long otroUsuarioId,
            List<Mensaje> mensajes
    ) {
        Mensaje ultimo = mensajes.stream()
                .max(Comparator.comparing(Mensaje::getFechaEnvio))
                .orElse(null);

        long noLeidos = mensajes.stream()
                .filter(mensaje -> mensaje.getReceptor().getId().equals(usuarioId))
                .filter(mensaje -> !mensaje.isLeido())
                .count();

        Usuario otroUsuario = obtenerUsuario(otroUsuarioId, "Usuario no encontrado");
        String marcoId = personalizacionRepository.findByUsuarioId(otroUsuarioId)
                .map(PersonalizacionUsuario::getMarcoId)
                .orElse("none");
        boolean online = Boolean.TRUE.equals(otroUsuario.getEnLinea());

        return new ConversacionDTO(
                otroUsuario.getId(),
                otroUsuario.getNombreUsuario(),
                otroUsuario.getFotoPerfilUrl(),
                marcoId,
                obtenerResumenMensaje(ultimo),
                ultimo == null ? null : ultimo.getFechaEnvio().toString(),
                otroUsuario.getUltimaConexion() == null
                        ? null
                        : otroUsuario.getUltimaConexion().toString(),
                noLeidos,
                online
        );
    }

    private MensajeChatDTO convertirMensajeDTO(Mensaje mensaje, Long usuarioActualId) {
        List<MensajeArchivoDTO> archivos = mensaje.getArchivos() == null
                ? new ArrayList<>()
                : mensaje.getArchivos()
                .stream()
                .map(this::convertirArchivoDTO)
                .toList();

        if (archivos.isEmpty() && mensaje.getUrlArchivo() != null) {
            archivos = List.of(new MensajeArchivoDTO(
                    null,
                    mensaje.getNombreArchivo(),
                    mensaje.getUrlArchivo(),
                    normalizarTipoParaFrontend(mensaje.getTipoMensaje()),
                    null,
                    false
            ));
        }

        return new MensajeChatDTO(
                mensaje.getId(),
                mensaje.getEmisor().getId(),
                mensaje.getContenido(),
                mensaje.getFechaEnvio(),
                mensaje.getEmisor().getId().equals(usuarioActualId),
                archivos.isEmpty() ? null : archivos
        );
    }

    private MensajeArchivoDTO convertirArchivoDTO(MensajeArchivo archivo) {
        Integer pesoKb = archivo.getTamanoBytes() == null
                ? null
                : Math.toIntExact((archivo.getTamanoBytes() + 1023L) / 1024L);

        return new MensajeArchivoDTO(
                archivo.getId(),
                archivo.getNombreArchivo(),
                archivo.getUrlArchivo(),
                normalizarTipoParaFrontend(archivo.getTipoArchivo()),
                pesoKb,
                false,
                archivo.getPublicId(),
                archivo.getTipoMime(),
                archivo.getFormato(),
                archivo.getResourceType(),
                archivo.getTamanoBytes(),
                archivo.getDuracionSegundos(),
                archivo.getAncho(),
                archivo.getAlto()
        );
    }

    private MensajeArchivo crearEntidadArchivo(ArchivoSubidoDTO archivo) {
        return MensajeArchivo.builder()
                .nombreArchivo(archivo.nombreOriginal())
                .urlArchivo(archivo.secureUrl())
                .publicId(archivo.publicId())
                .tipoMime(archivo.tipoMime())
                .tipoArchivo(archivo.tipoArchivo())
                .formato(archivo.formato())
                .resourceType(archivo.resourceType())
                .tamanoBytes(archivo.tamanoBytes())
                .duracionSegundos(archivo.duracionSegundos())
                .ancho(archivo.ancho())
                .alto(archivo.alto())
                .build();
    }

    private String determinarTipoMensaje(
            String contenido,
            List<ArchivoSubidoDTO> archivos
    ) {
        if (archivos.isEmpty()) {
            return "TEXTO";
        }

        boolean todosMismoTipo = archivos.stream()
                .map(ArchivoSubidoDTO::tipoArchivo)
                .distinct()
                .count() == 1;

        if (contenido == null && todosMismoTipo) {
            return archivos.get(0).tipoArchivo();
        }

        return "MULTIMEDIA";
    }

    private String obtenerResumenMensaje(Mensaje mensaje) {
        if (mensaje == null) {
            return null;
        }
        if (mensaje.getContenido() != null && !mensaje.getContenido().isBlank()) {
            return mensaje.getContenido();
        }
        int cantidad = mensaje.getArchivos() == null ? 0 : mensaje.getArchivos().size();
        if (cantidad > 1) {
            return cantidad + " archivos";
        }
        if (cantidad == 1 || mensaje.getUrlArchivo() != null) {
            return "Archivo adjunto";
        }
        return null;
    }

    private String normalizarContenido(String contenido) {
        if (contenido == null) {
            return null;
        }
        String valor = contenido.trim();
        return valor.isEmpty() ? null : valor;
    }

    private List<MultipartFile> normalizarArchivos(List<MultipartFile> archivos) {
        if (archivos == null) {
            return List.of();
        }
        return archivos.stream()
                .filter(archivo -> archivo != null && !archivo.isEmpty())
                .toList();
    }

    private String normalizarTipoParaFrontend(String tipo) {
        return tipo == null ? "documento" : tipo.toLowerCase(Locale.ROOT);
    }

    private void validarEnvioPermitido(Long emisorId, Long receptorId) {
        if (!bloqueoRepository.existeBloqueoEntre(emisorId, receptorId)) {
            return;
        }
        if (bloqueoRepository.findBloqueoActivo(emisorId, receptorId).isPresent()) {
            throw new IllegalStateException(
                    "Has bloqueado a este usuario. Desbloquealo para continuar."
            );
        }
        throw new IllegalStateException("No puedes comunicarte con este usuario.");
    }

    private Usuario obtenerUsuario(Long usuarioId, String mensajeError) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(mensajeError));
    }

    private SolicitudContacto obtenerSolicitud(Long solicitudId) {
        return solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud no encontrada"));
    }

    private void validarReceptorSolicitud(
            SolicitudContacto solicitud,
            Long usuarioId
    ) {
        if (!solicitud.getReceptor().getId().equals(usuarioId)) {
            throw new SecurityException("No tienes permiso");
        }
    }

    private void marcarSolicitudComoRechazada(SolicitudContacto solicitud) {
        solicitud.setEstado(SolicitudContacto.EstadoSolicitud.RECHAZADA);
        solicitud.setFechaRespuesta(Instant.now());
        solicitudRepository.save(solicitud);
    }

    private void enviarPorWebSocket(Long receptorId, MensajeChatDTO mensaje) {
        try {
            messagingTemplate.convertAndSendToUser(
                    receptorId.toString(),
                    "/queue/mensajes",
                    mensaje
            );
        } catch (RuntimeException exception) {
            log.error(
                    "No se pudo notificar el mensaje por WebSocket al usuario {}",
                    receptorId,
                    exception
            );
        }
    }
}
