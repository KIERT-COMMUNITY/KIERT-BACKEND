// src/main/java/com/kiert/backend/service/GrupoChatService.java
package com.kiert.backend.service;

import com.kiert.backend.dto.*;
import com.kiert.backend.entity.*;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
    private final InvitacionLinkRepository invitacionLinkRepository;
    private final GrupoHistorialRepository historialRepository;
    private final StorageService storageService;

    private static final String BASE_URL = "https://kiert.app/join/";

    // ============================================================
    // NOMBRES DE CACHÉ
    // ============================================================
    private static final String CACHE_GRUPOS_USUARIO = "gruposUsuario";
    private static final String CACHE_GRUPOS_PUBLICOS = "gruposPublicos";
    private static final String CACHE_GRUPO = "grupo";
    private static final String CACHE_MIEMBROS_GRUPO = "miembrosGrupo";
    private static final String CACHE_MENSAJES_GRUPO = "mensajesGrupo";
    private static final String CACHE_HISTORIAL_GRUPO = "historialGrupo";
    private static final String CACHE_INVITACIONES_PENDIENTES = "invitacionesPendientes";
    private static final String CACHE_LINKS_GRUPO = "linksGrupo";
    private static final String CACHE_INFO_INVITACION = "infoInvitacion";

    // ============================================================
    // AUDITORÍA (solo escritura, sin caché)
    // ============================================================
    private void registrarHistorial(Long grupoId, Long usuarioId, String accion,
                                    String valorAnterior, String valorNuevo, String detalle) {
        try {
            GrupoChat grupo = grupoRepository.findById(grupoId).orElse(null);
            if (grupo == null) return;

            Usuario usuario = usuarioId != null
                    ? usuarioRepository.findById(usuarioId).orElse(null)
                    : null;

            GrupoHistorial h = GrupoHistorial.builder()
                    .grupo(grupo)
                    .usuario(usuario)
                    .accion(accion)
                    .valorAnterior(valorAnterior)
                    .valorNuevo(valorNuevo)
                    .detalle(detalle)
                    .fecha(Instant.now())
                    .build();

            historialRepository.save(h);
        } catch (Exception e) {
            log.error("Error al registrar historial: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_HISTORIAL_GRUPO, key = "'grupo:' + #grupoId")
    public List<GrupoHistorialDTO> listarHistorial(Long grupoId, Long usuarioId) {
        if (!miembroRepository.esMiembroActivo(grupoId, usuarioId)) {
            throw new SecurityException("No eres miembro activo del grupo");
        }
        return historialRepository.findByGrupoIdOrderByFechaDesc(grupoId)
                .stream()
                .map(h -> new GrupoHistorialDTO(
                        h.getId(),
                        h.getGrupo().getId(),
                        h.getUsuario() != null ? h.getUsuario().getId() : null,
                        h.getUsuario() != null ? h.getUsuario().getNombreUsuario() : "Sistema",
                        h.getUsuario() != null ? h.getUsuario().getFotoPerfilUrl() : null,
                        h.getAccion(),
                        h.getDetalle(),
                        h.getValorAnterior(),
                        h.getValorNuevo(),
                        h.getFecha()
                ))
                .collect(Collectors.toList());
    }

    // ============================================================
    // CREAR GRUPO
    // ============================================================
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_GRUPOS_USUARIO, key = "'usuario:' + #creadorId"),
            @CacheEvict(value = CACHE_GRUPOS_PUBLICOS, allEntries = true)
    })
    public GrupoDTO crearGrupo(Long creadorId, CrearGrupoDTO dto) {
        log.info("Usuario {} creando grupo: {}", creadorId, dto.nombre());

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

        MiembroGrupo admin = MiembroGrupo.builder()
                .grupo(grupo)
                .usuario(creador)
                .rol("ADMIN")
                .estado("ACTIVO")
                .fechaUnion(Instant.now())
                .build();
        miembroRepository.save(admin);

        registrarHistorial(grupo.getId(), creadorId, "CREAR",
                null, grupo.getNombre(), "Creó el grupo");

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
                        log.error("Error notificación: {}", e.getMessage());
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
    @Cacheable(value = CACHE_GRUPOS_USUARIO, key = "'usuario:' + #usuarioId")
    public List<GrupoDTO> listarMisGrupos(Long usuarioId) {
        log.info("[DB] Listando grupos del usuario: {}", usuarioId);
        return grupoRepository.findGruposDeUsuario(usuarioId)
                .stream()
                .map(g -> mapearADTO(g, usuarioId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_GRUPOS_PUBLICOS, key = "'usuario:' + #usuarioId")
    public List<GrupoDTO> listarGruposPublicos(Long usuarioId) {
        log.info("[DB] Listando grupos públicos disponibles para: {}", usuarioId);
        return grupoRepository.findGruposPublicosDisponibles(usuarioId)
                .stream()
                .map(g -> mapearADTO(g, usuarioId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_GRUPO, key = "#grupoId + ':' + #usuarioId")
    public GrupoDTO obtenerGrupo(Long grupoId, Long usuarioId) {
        log.info("[DB] Obteniendo grupo: {}", grupoId);
        GrupoChat grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Grupo no encontrado"));
        return mapearADTO(grupo, usuarioId);
    }

    // ============================================================
    // INVITAR
    // ============================================================
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_MIEMBROS_GRUPO, key = "'grupo:' + #grupoId"),
            @CacheEvict(value = CACHE_GRUPO, allEntries = true),
            @CacheEvict(value = CACHE_GRUPOS_USUARIO, allEntries = true),
            @CacheEvict(value = CACHE_INVITACIONES_PENDIENTES, allEntries = true),
            @CacheEvict(value = CACHE_HISTORIAL_GRUPO, key = "'grupo:' + #grupoId")
    })
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

            registrarHistorial(grupoId, invitadorId, "INVITAR",
                    null, invitado.getNombreUsuario(),
                    "Invitó a @" + invitado.getNombreUsuario());

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
    @Caching(evict = {
            @CacheEvict(value = CACHE_MIEMBROS_GRUPO, key = "'grupo:' + #grupoId"),
            @CacheEvict(value = CACHE_GRUPO, allEntries = true),
            @CacheEvict(value = CACHE_GRUPOS_USUARIO, key = "'usuario:' + #usuarioId"),
            @CacheEvict(value = CACHE_INVITACIONES_PENDIENTES, key = "'usuario:' + #usuarioId"),
            @CacheEvict(value = CACHE_HISTORIAL_GRUPO, key = "'grupo:' + #grupoId")
    })
    public void aceptarInvitacion(Long grupoId, Long usuarioId) {
        MiembroGrupo miembro = miembroRepository.findByGrupoIdAndUsuarioId(grupoId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Invitación no encontrada"));

        if (!miembro.getEstado().equals("PENDIENTE")) {
            throw new IllegalArgumentException("Esta invitación ya fue procesada");
        }

        miembro.setEstado("ACTIVO");
        miembro.setFechaUnion(Instant.now());
        miembroRepository.save(miembro);

        registrarHistorial(grupoId, usuarioId, "MIEMBRO_UNIDO",
                null, miembro.getUsuario().getNombreUsuario(),
                "Aceptó la invitación al grupo");
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_INVITACIONES_PENDIENTES, key = "'usuario:' + #usuarioId"),
            @CacheEvict(value = CACHE_MIEMBROS_GRUPO, key = "'grupo:' + #grupoId")
    })
    public void rechazarInvitacion(Long grupoId, Long usuarioId) {
        MiembroGrupo miembro = miembroRepository.findByGrupoIdAndUsuarioId(grupoId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Invitación no encontrada"));

        miembro.setEstado("RECHAZADO");
        miembroRepository.save(miembro);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_MIEMBROS_GRUPO, key = "'grupo:' + #grupoId"),
            @CacheEvict(value = CACHE_GRUPO, allEntries = true),
            @CacheEvict(value = CACHE_GRUPOS_USUARIO, key = "'usuario:' + #usuarioId"),
            @CacheEvict(value = CACHE_GRUPOS_PUBLICOS, allEntries = true),
            @CacheEvict(value = CACHE_HISTORIAL_GRUPO, key = "'grupo:' + #grupoId")
    })
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

        registrarHistorial(grupoId, usuarioId, "MIEMBRO_UNIDO",
                null, usuario.getNombreUsuario(),
                "Se unió al grupo público");
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_MIEMBROS_GRUPO, key = "'grupo:' + #grupoId"),
            @CacheEvict(value = CACHE_GRUPO, allEntries = true),
            @CacheEvict(value = CACHE_GRUPOS_USUARIO, key = "'usuario:' + #usuarioId"),
            @CacheEvict(value = CACHE_HISTORIAL_GRUPO, key = "'grupo:' + #grupoId")
    })
    public void salirDelGrupo(Long grupoId, Long usuarioId) {
        MiembroGrupo miembro = miembroRepository.findByGrupoIdAndUsuarioId(grupoId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No perteneces a este grupo"));

        GrupoChat grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Grupo no encontrado"));

        if (grupo.getCreador().getId().equals(usuarioId)) {
            throw new SecurityException("Como creador, debes eliminar el grupo en vez de salir");
        }

        miembro.setEstado("SALIO");
        miembroRepository.save(miembro);

        registrarHistorial(grupoId, usuarioId, "SALIR",
                null, null,
                "@" + miembro.getUsuario().getNombreUsuario() + " salió del grupo");

        log.info("Usuario {} salió del grupo {}", usuarioId, grupoId);
    }

    // ============================================================
    // ELIMINAR GRUPO
    // ============================================================
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_MIEMBROS_GRUPO, key = "'grupo:' + #grupoId"),
            @CacheEvict(value = CACHE_GRUPO, allEntries = true),
            @CacheEvict(value = CACHE_GRUPOS_USUARIO, allEntries = true),
            @CacheEvict(value = CACHE_GRUPOS_PUBLICOS, allEntries = true),
            @CacheEvict(value = CACHE_MENSAJES_GRUPO, key = "'grupo:' + #grupoId"),
            @CacheEvict(value = CACHE_HISTORIAL_GRUPO, key = "'grupo:' + #grupoId"),
            @CacheEvict(value = CACHE_LINKS_GRUPO, allEntries = true)
    })
    public void eliminarGrupo(Long grupoId, Long usuarioId) {
        log.info("Usuario {} intentando eliminar grupo {}", usuarioId, grupoId);

        GrupoChat grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Grupo no encontrado"));

        if (!grupo.isActivo()) {
            throw new IllegalArgumentException("El grupo ya fue eliminado");
        }

        MiembroGrupo miembro = miembroRepository.findByGrupoIdAndUsuarioId(grupoId, usuarioId)
                .orElseThrow(() -> new SecurityException("No eres miembro de este grupo"));

        boolean esCreador = grupo.getCreador().getId().equals(usuarioId);
        boolean esAdmin = "ADMIN".equals(miembro.getRol());

        if (!esCreador && !esAdmin) {
            throw new SecurityException("Solo el creador o un administrador puede eliminar el grupo");
        }

        String nombreAntes = grupo.getNombre();
        grupo.setActivo(false);
        grupoRepository.save(grupo);

        registrarHistorial(grupoId, usuarioId, "ELIMINAR",
                nombreAntes, null,
                "Eliminó el grupo");

        log.info("Grupo {} eliminado por usuario {}", grupoId, usuarioId);
    }

    // ============================================================
    // EXPULSAR MIEMBRO
    // ============================================================
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_MIEMBROS_GRUPO, key = "'grupo:' + #grupoId"),
            @CacheEvict(value = CACHE_GRUPO, allEntries = true),
            @CacheEvict(value = CACHE_GRUPOS_USUARIO, key = "'usuario:' + #usuarioAExpulsarId"),
            @CacheEvict(value = CACHE_HISTORIAL_GRUPO, key = "'grupo:' + #grupoId")
    })
    public void expulsarMiembro(Long grupoId, Long adminId, Long usuarioAExpulsarId) {
        log.info("Admin {} expulsa a {} del grupo {}", adminId, usuarioAExpulsarId, grupoId);

        if (adminId.equals(usuarioAExpulsarId)) {
            throw new IllegalArgumentException("No puedes expulsarte a ti mismo. Usa 'Salir del grupo'.");
        }

        GrupoChat grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Grupo no encontrado"));

        MiembroGrupo admin = miembroRepository.findByGrupoIdAndUsuarioId(grupoId, adminId)
                .orElseThrow(() -> new SecurityException("No eres miembro de este grupo"));

        if (!"ADMIN".equals(admin.getRol())) {
            throw new SecurityException("Solo los administradores pueden expulsar miembros");
        }

        MiembroGrupo miembro = miembroRepository.findByGrupoIdAndUsuarioId(grupoId, usuarioAExpulsarId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no pertenece al grupo"));

        if (!"ACTIVO".equals(miembro.getEstado())) {
            throw new IllegalArgumentException("El usuario no es miembro activo del grupo");
        }

        if (grupo.getCreador().getId().equals(usuarioAExpulsarId)) {
            throw new SecurityException("No puedes expulsar al creador del grupo");
        }

        miembro.setEstado("EXPULSADO");
        miembroRepository.save(miembro);

        registrarHistorial(grupoId, adminId, "EXPULSAR",
                miembro.getUsuario().getNombreUsuario(), null,
                "Expulsó a @" + miembro.getUsuario().getNombreUsuario());

        log.info("Usuario {} expulsado del grupo {}", usuarioAExpulsarId, grupoId);
    }

    // ============================================================
    // LISTADOS AUXILIARES
    // ============================================================
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_INVITACIONES_PENDIENTES, key = "'usuario:' + #usuarioId")
    public List<InvitacionGrupoDTO> listarInvitacionesPendientes(Long usuarioId) {
        log.info("[DB] Listando invitaciones pendientes de: {}", usuarioId);
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
    @Cacheable(value = CACHE_MIEMBROS_GRUPO, key = "'grupo:' + #grupoId")
    public List<MiembroGrupoDTO> listarMiembros(Long grupoId) {
        log.info("[DB] Listando miembros del grupo: {}", grupoId);
        return miembroRepository.findMiembrosActivos(grupoId)
                .stream()
                .map(m -> {
                    Usuario u = m.getUsuario();
                    Boolean online = u.getEnLinea() != null && u.getEnLinea();

                    return new MiembroGrupoDTO(
                            m.getId(),
                            u.getId(),
                            u.getNombreUsuario(),
                            u.getEmail(),
                            u.getFotoPerfilUrl(),
                            m.getRol(),
                            m.getEstado(),
                            m.getFechaUnion(),
                            m.getInvitadoPor() != null ? m.getInvitadoPor().getNombreUsuario() : null,
                            online,
                            u.getUltimaConexion()
                    );
                })
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
    @Cacheable(
            value = CACHE_MENSAJES_GRUPO,
            key = "'grupo:' + #grupoId"
    )
    public List<MensajeGrupoDTO> obtenerMensajes(Long grupoId, Long usuarioActualId) {
        log.info("[DB] Obteniendo mensajes del grupo: {}", grupoId);
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
    @CacheEvict(value = CACHE_MENSAJES_GRUPO, key = "'grupo:' + #grupoId")
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

    // ============================================================
    // MENSAJE CON ARCHIVO
    // ============================================================
    @Transactional
    @CacheEvict(value = CACHE_MENSAJES_GRUPO, key = "'grupo:' + #grupoId")
    public MensajeGrupoDTO enviarMensajeConArchivo(
            Long grupoId, Long emisorId, String contenido, MultipartFile archivo) {

        log.info("Usuario {} envía archivo al grupo {}", emisorId, grupoId);

        GrupoChat grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Grupo no encontrado"));

        if (!miembroRepository.esMiembroActivo(grupoId, emisorId)) {
            throw new SecurityException("No eres miembro de este grupo");
        }

        Usuario emisor = usuarioRepository.findById(emisorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        String urlArchivo = null;
        String nombreArchivo = null;
        String tipoMensaje = "TEXTO";

        if (archivo != null && !archivo.isEmpty()) {
            try {
                urlArchivo = storageService.subirArchivo(archivo);
                nombreArchivo = archivo.getOriginalFilename();
                tipoMensaje = determinarTipoArchivo(archivo);
            } catch (Exception e) {
                log.error("Error al subir archivo: {}", e.getMessage());
                throw new RuntimeException("Error al subir archivo: " + e.getMessage());
            }
        }

        MensajeGrupo mensaje = MensajeGrupo.builder()
                .grupo(grupo)
                .emisor(emisor)
                .contenido(contenido != null ? contenido : "")
                .tipoMensaje(tipoMensaje)
                .urlArchivo(urlArchivo)
                .nombreArchivo(nombreArchivo)
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

    private String determinarTipoArchivo(MultipartFile archivo) {
        String nombre = archivo.getOriginalFilename();
        if (nombre == null) return "documento";
        String ext = nombre.substring(nombre.lastIndexOf(".") + 1).toLowerCase();
        return switch (ext) {
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

    // ============================================================
    // FOTO DEL GRUPO
    // ============================================================
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_GRUPO, allEntries = true),
            @CacheEvict(value = CACHE_GRUPOS_USUARIO, allEntries = true),
            @CacheEvict(value = CACHE_GRUPOS_PUBLICOS, allEntries = true),
            @CacheEvict(value = CACHE_HISTORIAL_GRUPO, key = "'grupo:' + #grupoId")
    })
    public GrupoDTO actualizarFotoGrupo(Long grupoId, Long usuarioId, MultipartFile foto) {
        log.info("Usuario {} actualiza foto del grupo {}", usuarioId, grupoId);

        GrupoChat grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Grupo no encontrado"));

        MiembroGrupo miembro = miembroRepository.findByGrupoIdAndUsuarioId(grupoId, usuarioId)
                .orElseThrow(() -> new SecurityException("No eres miembro de este grupo"));

        if (!"ADMIN".equals(miembro.getRol())) {
            throw new SecurityException("Solo los administradores pueden cambiar la foto");
        }

        if (foto == null || foto.isEmpty()) {
            throw new IllegalArgumentException("La foto es obligatoria");
        }

        String contentType = foto.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Solo se permiten imágenes");
        }

        if (foto.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("La imagen no debe superar 5MB");
        }

        try {
            String url = storageService.subirArchivo(foto);
            grupo.setFotoUrl(url);
            grupoRepository.save(grupo);

            registrarHistorial(grupoId, usuarioId, "CAMBIAR_FOTO",
                    null, url,
                    "Cambió la foto del grupo");

            log.info("Foto del grupo {} actualizada: {}", grupoId, url);
            return mapearADTO(grupo, usuarioId);
        } catch (Exception e) {
            log.error("Error al subir foto: {}", e.getMessage());
            throw new RuntimeException("Error al subir la foto: " + e.getMessage());
        }
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_GRUPO, allEntries = true),
            @CacheEvict(value = CACHE_GRUPOS_USUARIO, allEntries = true),
            @CacheEvict(value = CACHE_GRUPOS_PUBLICOS, allEntries = true),
            @CacheEvict(value = CACHE_HISTORIAL_GRUPO, key = "'grupo:' + #grupoId")
    })
    public GrupoDTO eliminarFotoGrupo(Long grupoId, Long usuarioId) {
        GrupoChat grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Grupo no encontrado"));

        MiembroGrupo miembro = miembroRepository.findByGrupoIdAndUsuarioId(grupoId, usuarioId)
                .orElseThrow(() -> new SecurityException("No eres miembro de este grupo"));

        if (!"ADMIN".equals(miembro.getRol())) {
            throw new SecurityException("Solo los administradores pueden eliminar la foto");
        }

        String fotoAntes = grupo.getFotoUrl();
        grupo.setFotoUrl(null);
        grupoRepository.save(grupo);

        registrarHistorial(grupoId, usuarioId, "ELIMINAR_FOTO",
                fotoAntes, null,
                "Eliminó la foto del grupo");

        return mapearADTO(grupo, usuarioId);
    }

    // ============================================================
    // EDITAR INFO DEL GRUPO
    // ============================================================
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_GRUPO, allEntries = true),
            @CacheEvict(value = CACHE_GRUPOS_USUARIO, allEntries = true),
            @CacheEvict(value = CACHE_GRUPOS_PUBLICOS, allEntries = true),
            @CacheEvict(value = CACHE_HISTORIAL_GRUPO, key = "'grupo:' + #grupoId")
    })
    public GrupoDTO actualizarInfoGrupo(Long grupoId, Long usuarioId, String nombre, String descripcion) {
        log.info("Usuario {} editando grupo {}", usuarioId, grupoId);

        GrupoChat grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Grupo no encontrado"));

        MiembroGrupo miembro = miembroRepository.findByGrupoIdAndUsuarioId(grupoId, usuarioId)
                .orElseThrow(() -> new SecurityException("No eres miembro"));

        if (!"ADMIN".equals(miembro.getRol())) {
            throw new SecurityException("Solo los administradores pueden editar");
        }

        boolean cambio = false;

        // ===== NOMBRE =====
        if (nombre != null && !nombre.trim().isEmpty()) {
            String nombreNuevo = nombre.trim();
            String nombreAnterior = grupo.getNombre();

            if (!nombreNuevo.equals(nombreAnterior)) {
                grupo.setNombre(nombreNuevo);
                registrarHistorial(grupoId, usuarioId, "EDITAR_NOMBRE",
                        nombreAnterior, nombreNuevo,
                        "Cambió el nombre del grupo");
                cambio = true;
            }
        }

        // ===== DESCRIPCIÓN =====
        if (descripcion != null) {
            String descNueva = descripcion.trim();
            String descAnterior = grupo.getDescripcion() != null ? grupo.getDescripcion() : "";

            if (!descNueva.equals(descAnterior)) {
                grupo.setDescripcion(descNueva);
                registrarHistorial(grupoId, usuarioId, "EDITAR_DESC",
                        descAnterior.isEmpty() ? "(vacío)" : descAnterior,
                        descNueva.isEmpty() ? "(vacío)" : descNueva,
                        "Cambió la descripción del grupo");
                cambio = true;
            }
        }

        if (cambio) {
            grupoRepository.save(grupo);
            log.info("Grupo {} editado por usuario {}", grupoId, usuarioId);
        }

        return mapearADTO(grupo, usuarioId);
    }

    // ============================================================
    // INVITACIONES POR LINK
    // ============================================================
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_LINKS_GRUPO, key = "'grupo:' + #grupoId"),
            @CacheEvict(value = CACHE_HISTORIAL_GRUPO, key = "'grupo:' + #grupoId")
    })
    public InvitacionLinkDTO generarLinkInvitacion(Long grupoId, Long usuarioId, CrearInvitacionLinkDTO dto) {
        log.info("Usuario {} genera link para grupo {}", usuarioId, grupoId);

        GrupoChat grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Grupo no encontrado"));

        if (!grupo.isActivo()) {
            throw new IllegalArgumentException("El grupo no está activo");
        }

        MiembroGrupo miembro = miembroRepository.findByGrupoIdAndUsuarioId(grupoId, usuarioId)
                .orElseThrow(() -> new SecurityException("No eres miembro del grupo"));

        if (!"ACTIVO".equals(miembro.getEstado())) {
            throw new SecurityException("No eres miembro activo del grupo");
        }

        Usuario creador = miembro.getUsuario();
        String token = UUID.randomUUID().toString();

        Instant expiraEn = null;
        if (dto != null && dto.horasExpiracion() != null && dto.horasExpiracion() > 0) {
            expiraEn = Instant.now().plus(dto.horasExpiracion(), ChronoUnit.HOURS);
        }

        Integer usosMaximos = (dto != null && dto.usosMaximos() != null) ? dto.usosMaximos() : 0;

        InvitacionLink link = InvitacionLink.builder()
                .grupo(grupo)
                .token(token)
                .creador(creador)
                .usosMaximos(usosMaximos)
                .usosActuales(0)
                .expiraEn(expiraEn)
                .activo(true)
                .fechaCreacion(Instant.now())
                .build();

        link = invitacionLinkRepository.save(link);

        registrarHistorial(grupoId, usuarioId, "LINK_CREADO",
                null, token.substring(0, 8) + "...",
                "Generó un link de invitación");

        return mapearLinkADTO(link);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_LINKS_GRUPO, key = "'grupo:' + #grupoId")
    public List<InvitacionLinkDTO> listarLinksActivos(Long grupoId, Long usuarioId) {
        log.info("[DB] Listando links activos del grupo: {}", grupoId);
        if (!miembroRepository.esMiembroActivo(grupoId, usuarioId)) {
            throw new SecurityException("No eres miembro activo del grupo");
        }
        return invitacionLinkRepository.findActivosByGrupo(grupoId)
                .stream()
                .map(this::mapearLinkADTO)
                .collect(Collectors.toList());
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_LINKS_GRUPO, allEntries = true),
            @CacheEvict(value = CACHE_HISTORIAL_GRUPO, allEntries = true)
    })
    public void desactivarLink(Long linkId, Long usuarioId) {
        InvitacionLink link = invitacionLinkRepository.findById(linkId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Link no encontrado"));

        MiembroGrupo miembro = miembroRepository.findByGrupoIdAndUsuarioId(link.getGrupo().getId(), usuarioId)
                .orElseThrow(() -> new SecurityException("No eres miembro del grupo"));

        boolean esCreadorLink = link.getCreador().getId().equals(usuarioId);
        boolean esAdmin = "ADMIN".equals(miembro.getRol());

        if (!esCreadorLink && !esAdmin) {
            throw new SecurityException("Solo el creador del link o un admin puede desactivarlo");
        }

        link.setActivo(false);
        invitacionLinkRepository.save(link);

        registrarHistorial(link.getGrupo().getId(), usuarioId, "LINK_DESACTIVADO",
                link.getToken().substring(0, 8) + "...", null,
                "Desactivó un link de invitación");

        log.info("Link {} desactivado por usuario {}", linkId, usuarioId);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_INFO_INVITACION, key = "#token")
    public InfoInvitacionDTO obtenerInfoInvitacion(String token) {
        log.info("[DB] Obteniendo info de invitación: {}", token);
        Optional<InvitacionLink> opt = invitacionLinkRepository.findByToken(token);

        if (opt.isEmpty()) {
            return new InfoInvitacionDTO(false, "Link inválido", null, null, null, null, null, null, null);
        }

        InvitacionLink link = opt.get();

        if (!link.esValido()) {
            String msg = "Este link ya expiró o alcanzó su límite de usos";
            if (!link.isActivo()) msg = "Este link fue desactivado";
            if (link.getExpiraEn() != null && Instant.now().isAfter(link.getExpiraEn())) {
                msg = "Este link ha expirado";
            }
            if (link.getUsosMaximos() > 0 && link.getUsosActuales() >= link.getUsosMaximos()) {
                msg = "Este link alcanzó su límite de usos";
            }
            return new InfoInvitacionDTO(false, msg, null, null, null, null, null, null, null);
        }

        GrupoChat grupo = link.getGrupo();
        long totalMiembros = miembroRepository.findMiembrosActivos(grupo.getId()).size();

        return new InfoInvitacionDTO(
                true,
                "Link válido",
                grupo.getId(),
                grupo.getNombre(),
                grupo.getDescripcion(),
                grupo.getFotoUrl(),
                link.getCreador().getNombreUsuario(),
                (int) totalMiembros,
                link.getExpiraEn()
        );
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_MIEMBROS_GRUPO, allEntries = true),
            @CacheEvict(value = CACHE_GRUPO, allEntries = true),
            @CacheEvict(value = CACHE_GRUPOS_USUARIO, allEntries = true),
            @CacheEvict(value = CACHE_GRUPOS_PUBLICOS, allEntries = true),
            @CacheEvict(value = CACHE_LINKS_GRUPO, allEntries = true),
            @CacheEvict(value = CACHE_INFO_INVITACION, key = "#token"),
            @CacheEvict(value = CACHE_HISTORIAL_GRUPO, allEntries = true)
    })
    public GrupoDTO unirseConLink(String token, Long usuarioId) {
        log.info("Usuario {} intenta unirse con token {}", usuarioId, token);

        InvitacionLink link = invitacionLinkRepository.findByToken(token)
                .orElseThrow(() -> new RecursoNoEncontradoException("Link inválido"));

        if (!link.esValido()) {
            throw new IllegalArgumentException("Este link ya no es válido");
        }

        GrupoChat grupo = link.getGrupo();

        if (!grupo.isActivo()) {
            throw new IllegalArgumentException("El grupo ya no está activo");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        Optional<MiembroGrupo> existente = miembroRepository.findByGrupoIdAndUsuarioId(grupo.getId(), usuarioId);

        if (existente.isPresent()) {
            MiembroGrupo m = existente.get();
            if ("ACTIVO".equals(m.getEstado())) {
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
                    .invitadoPor(link.getCreador())
                    .build();
            miembroRepository.save(miembro);
        }

        link.registrarUso();
        invitacionLinkRepository.save(link);

        registrarHistorial(grupo.getId(), usuarioId, "MIEMBRO_UNIDO",
                null, usuario.getNombreUsuario(),
                "Se unió mediante link");

        try {
            notificacionService.crearNotificacionGrupo(
                    link.getCreador().getId(),
                    usuarioId,
                    "<strong>" + usuario.getNombreUsuario() + "</strong> se unió al grupo <strong>" + grupo.getNombre() + "</strong> mediante tu link",
                    grupo.getId(),
                    "/chat/grupo/" + grupo.getId()
            );
        } catch (Exception e) {
            log.error("Error al notificar: {}", e.getMessage());
        }

        log.info("Usuario {} unido al grupo {} con link {}", usuarioId, grupo.getId(), token);

        return mapearADTO(grupo, usuarioId);
    }

    private InvitacionLinkDTO mapearLinkADTO(InvitacionLink link) {
        return new InvitacionLinkDTO(
                link.getId(),
                link.getGrupo().getId(),
                link.getGrupo().getNombre(),
                link.getToken(),
                BASE_URL + link.getToken(),
                link.getCreador().getId(),
                link.getCreador().getNombreUsuario(),
                link.getUsosMaximos(),
                link.getUsosActuales(),
                link.getExpiraEn(),
                link.isActivo(),
                link.getFechaCreacion(),
                link.getFechaUltimoUso()
        );
    }
}