// src/main/java/com/kiert/backend/features/grupo/service/GrupoChatService.java
package com.kiert.backend.features.grupo.service;

import com.kiert.backend.entity.*;
import com.kiert.backend.exception.BadRequestException;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.features.grupo.dto.*;
import com.kiert.backend.features.grupo.repository.GrupoChatRepository;
import com.kiert.backend.features.grupo.repository.GrupoHistorialRepository;
import com.kiert.backend.features.grupo.repository.MensajeGrupoRepository;
import com.kiert.backend.features.grupo.repository.MiembroGrupoRepository;
import com.kiert.backend.features.invitacion.dto.CrearInvitacionLinkDTO;
import com.kiert.backend.features.invitacion.dto.InfoInvitacionDTO;
import com.kiert.backend.features.invitacion.dto.InvitacionLinkDTO;
import com.kiert.backend.features.invitacion.repository.InvitacionLinkRepository;
import com.kiert.backend.features.notificaciones.repository.NotificacionRepository;
import com.kiert.backend.features.usuario.repository.UsuarioRepository;
import com.kiert.backend.features.notificaciones.service.NotificacionService;
import com.kiert.backend.features.personalizacion.repository.PersonalizacionRepository;
import com.kiert.backend.features.presencia.service.PresenciaService;
import com.kiert.backend.features.storage.StorageService;
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
    private final NotificacionRepository notificacionRepository;
    private final InvitacionLinkRepository invitacionLinkRepository;
    private final GrupoHistorialRepository historialRepository;
    private final StorageService storageService;
    private final PresenciaService presenciaService;
    private final PersonalizacionRepository personalizacionRepository;

    private static final String BASE_URL = "https://kiert.app/join/";
    private static final int MAX_NOMBRE_GRUPO = 100;
    private static final int MAX_DESCRIPCION = 500;
    private static final int MAX_MENSAJE = 5000;
    private static final long MAX_FOTO_SIZE = 5 * 1024 * 1024;
    private static final long MAX_ARCHIVO_SIZE = 20 * 1024 * 1024;
    private static final String CLOUDINARY_PREFIX = "https://res.cloudinary.com/";

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
    // HELPER: resolver marco del usuario
    // ============================================================
    private String resolverMarcoUsuario(Long usuarioId) {
        if (usuarioId == null) return "none";

        try {
            String marcoPersonalizacion = personalizacionRepository
                    .findByUsuarioId(usuarioId)
                    .map(PersonalizacionUsuario::getMarcoId)
                    .orElse(null);

            if (marcoPersonalizacion != null && !marcoPersonalizacion.isBlank()) {
                return marcoPersonalizacion;
            }
        } catch (Exception e) {
            log.debug("Error leyendo personalizacion de {}: {}", usuarioId, e.getMessage());
        }

        try {
            String marcoFallback = usuarioRepository.findById(usuarioId)
                    .map(Usuario::getMarcoId)
                    .orElse(null);

            if (marcoFallback != null && !marcoFallback.isBlank()) {
                return marcoFallback;
            }
        } catch (Exception e) {
            log.debug("Error leyendo marco fallback de {}: {}", usuarioId, e.getMessage());
        }

        return "none";
    }

    // ============================================================
    // AUDITORIA
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
            log.warn("Error al registrar historial: {}", e.getMessage());
        }
    }

    private void crearMensajeSistema(Long grupoId, Long emisorId, String contenido) {
        try {
            GrupoChat grupo = grupoRepository.findById(grupoId).orElse(null);
            if (grupo == null) return;

            Usuario emisor = null;
            if (emisorId != null) {
                emisor = usuarioRepository.findById(emisorId).orElse(null);
            }
            if (emisor == null) {
                emisor = grupo.getCreador();
            }
            if (emisor == null) return;

            MensajeGrupo mensaje = MensajeGrupo.builder()
                    .grupo(grupo)
                    .emisor(emisor)
                    .contenido(contenido)
                    .tipoMensaje("SISTEMA")
                    .fechaEnvio(Instant.now())
                    .eliminado(false)
                    .build();

            mensajeRepository.save(mensaje);
        } catch (Exception e) {
            log.warn("Error al crear mensaje de sistema: {}", e.getMessage());
        }
    }

    // ============================================================
    // HISTORIAL
    // ============================================================
    @Transactional(readOnly = true)
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
        log.info("Usuario {} creando grupo: {}", creadorId, dto != null ? dto.nombre() : "null");

        if (creadorId == null) throw new BadRequestException("Usuario requerido");
        if (dto == null) throw new BadRequestException("Datos requeridos");
        if (dto.nombre() == null || dto.nombre().trim().isEmpty()) {
            throw new BadRequestException("El nombre del grupo es obligatorio");
        }
        if (dto.nombre().trim().length() > MAX_NOMBRE_GRUPO) {
            throw new BadRequestException("El nombre es demasiado largo");
        }
        if (dto.descripcion() != null && dto.descripcion().length() > MAX_DESCRIPCION) {
            throw new BadRequestException("La descripcion es demasiado larga");
        }

        Usuario creador = usuarioRepository.findById(creadorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        String tipo = dto.tipo() != null ? dto.tipo().trim().toUpperCase() : "PRIVADO";
        if (!tipo.equals("PUBLICO") && !tipo.equals("PRIVADO")) {
            throw new BadRequestException("Tipo de grupo invalido");
        }

        GrupoChat grupo = GrupoChat.builder()
                .nombre(dto.nombre().trim())
                .descripcion(dto.descripcion())
                .creador(creador)
                .tipo(tipo)
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
                null, grupo.getNombre(), "Creo el grupo");

        if (dto.usuariosInvitados() != null && !dto.usuariosInvitados().isEmpty()) {
            for (Long usuarioId : dto.usuariosInvitados()) {
                if (usuarioId == null || usuarioId.equals(creadorId)) continue;

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
                                "<strong>" + creador.getNombreUsuario() + "</strong> te invito al grupo <strong>" + grupo.getNombre() + "</strong>",
                                grupo.getId(),
                                "/chat/grupo/" + grupo.getId()
                        );
                    } catch (Exception e) {
                        log.warn("Error notificacion: {}", e.getMessage());
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
        log.debug("[DB] Listando grupos del usuario: {}", usuarioId);
        return grupoRepository.findGruposDeUsuario(usuarioId)
                .stream()
                .map(g -> mapearADTO(g, usuarioId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_GRUPOS_PUBLICOS, key = "'usuario:' + #usuarioId")
    public List<GrupoDTO> listarGruposPublicos(Long usuarioId) {
        log.debug("[DB] Listando grupos publicos disponibles para: {}", usuarioId);
        return grupoRepository.findGruposPublicosDisponibles(usuarioId)
                .stream()
                .map(g -> mapearADTO(g, usuarioId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GrupoDTO obtenerGrupo(Long grupoId, Long usuarioId) {
        log.debug("[DB] Obteniendo grupo: {}", grupoId);
        GrupoChat grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Grupo no encontrado"));

        if (!grupo.isActivo()) {
            throw new RecursoNoEncontradoException("Grupo no encontrado");
        }

        boolean esMiembro = usuarioId != null && miembroRepository.esMiembroActivo(grupoId, usuarioId);
        boolean esPublico = "PUBLICO".equals(grupo.getTipo());

        if (!esMiembro && !esPublico) {
            throw new SecurityException("No tienes acceso a este grupo");
        }

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
        if (usuariosIds == null || usuariosIds.isEmpty()) return;

        GrupoChat grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Grupo no encontrado"));

        Usuario invitador = usuarioRepository.findById(invitadorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        if (!miembroRepository.esMiembroActivo(grupoId, invitadorId)) {
            throw new SecurityException("No eres miembro de este grupo");
        }

        String estadoInvitacion = grupo.getTipo().equals("PUBLICO") ? "ACTIVO" : "PENDIENTE";

        for (Long usuarioId : usuariosIds) {
            if (usuarioId == null || usuarioId.equals(invitadorId)) continue;

            Usuario invitado = usuarioRepository.findById(usuarioId).orElse(null);
            if (invitado == null) continue;

            Optional<MiembroGrupo> existenteOpt = miembroRepository
                    .findByGrupoIdAndUsuarioId(grupoId, usuarioId);

            if (existenteOpt.isPresent()) {
                MiembroGrupo m = existenteOpt.get();
                if ("ACTIVO".equals(m.getEstado())) continue;

                m.setEstado(estadoInvitacion);
                m.setFechaInvitacion(Instant.now());
                m.setInvitadoPor(invitador);
                if ("ACTIVO".equals(estadoInvitacion)) {
                    m.setFechaUnion(Instant.now());
                }
                miembroRepository.save(m);
            } else {
                MiembroGrupo miembro = MiembroGrupo.builder()
                        .grupo(grupo)
                        .usuario(invitado)
                        .rol("MIEMBRO")
                        .estado(estadoInvitacion)
                        .fechaUnion(Instant.now())
                        .fechaInvitacion(Instant.now())
                        .invitadoPor(invitador)
                        .build();
                miembroRepository.save(miembro);
            }

            registrarHistorial(grupoId, invitadorId, "INVITAR",
                    null, invitado.getNombreUsuario(),
                    "Invito a @" + invitado.getNombreUsuario());

            if ("PENDIENTE".equals(estadoInvitacion)) {
                try {
                    notificacionService.crearNotificacionGrupo(
                            usuarioId,
                            invitadorId,
                            "<strong>" + invitador.getNombreUsuario() + "</strong> te invito al grupo <strong>" + grupo.getNombre() + "</strong>",
                            grupoId,
                            "/chat/grupo/" + grupoId
                    );
                } catch (Exception e) {
                    log.warn("Error al crear notificacion: {}", e.getMessage());
                }
            }
        }
    }

    // ============================================================
    // ACEPTAR / RECHAZAR INVITACION
    // ============================================================
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_MIEMBROS_GRUPO, allEntries = true),
            @CacheEvict(value = CACHE_GRUPO, allEntries = true),
            @CacheEvict(value = CACHE_GRUPOS_USUARIO, allEntries = true),
            @CacheEvict(value = CACHE_INVITACIONES_PENDIENTES, allEntries = true),
            @CacheEvict(value = CACHE_HISTORIAL_GRUPO, allEntries = true),
            @CacheEvict(value = CACHE_MENSAJES_GRUPO, allEntries = true),
            @CacheEvict(value = CACHE_GRUPOS_PUBLICOS, allEntries = true)
    })
    public void aceptarInvitacion(Long grupoId, Long usuarioId) {
        log.info("Aceptando invitacion - grupo: {}, usuario: {}", grupoId, usuarioId);

        MiembroGrupo miembro = miembroRepository.findByGrupoIdAndUsuarioId(grupoId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Invitacion no encontrada"));

        String estado = miembro.getEstado();

        if ("ACTIVO".equals(estado)) {
            throw new BadRequestException("Ya eres miembro de este grupo");
        }

        if (!"PENDIENTE".equals(estado)
                && !"SALIO".equals(estado)
                && !"RECHAZADO".equals(estado)
                && !"EXPULSADO".equals(estado)) {
            throw new BadRequestException("Esta invitacion no se puede aceptar");
        }

        miembro.setEstado("ACTIVO");
        miembro.setFechaUnion(Instant.now());
        miembroRepository.save(miembro);
        miembroRepository.flush();

        try {
            notificacionRepository.eliminarNotificacionesInvitacionGrupo(usuarioId, grupoId);
        } catch (Exception e) {
            log.warn("Error al eliminar notificacion: {}", e.getMessage());
        }

        registrarHistorial(grupoId, usuarioId, "MIEMBRO_UNIDO",
                null, miembro.getUsuario().getNombreUsuario(),
                "Acepto la invitacion al grupo");

        crearMensajeSistema(grupoId, usuarioId,
                "@" + miembro.getUsuario().getNombreUsuario() + " se unio al grupo");
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_INVITACIONES_PENDIENTES, allEntries = true),
            @CacheEvict(value = CACHE_MIEMBROS_GRUPO, allEntries = true)
    })
    public void rechazarInvitacion(Long grupoId, Long usuarioId) {
        log.info("Rechazando invitacion - grupo: {}, usuario: {}", grupoId, usuarioId);

        MiembroGrupo miembro = miembroRepository.findByGrupoIdAndUsuarioId(grupoId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Invitacion no encontrada"));

        miembro.setEstado("RECHAZADO");
        miembroRepository.save(miembro);
        miembroRepository.flush();

        try {
            notificacionRepository.eliminarNotificacionesInvitacionGrupo(usuarioId, grupoId);
        } catch (Exception e) {
            log.warn("Error al eliminar notificacion: {}", e.getMessage());
        }
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_MIEMBROS_GRUPO, allEntries = true),
            @CacheEvict(value = CACHE_GRUPO, allEntries = true),
            @CacheEvict(value = CACHE_GRUPOS_USUARIO, allEntries = true),
            @CacheEvict(value = CACHE_GRUPOS_PUBLICOS, allEntries = true),
            @CacheEvict(value = CACHE_HISTORIAL_GRUPO, allEntries = true),
            @CacheEvict(value = CACHE_MENSAJES_GRUPO, allEntries = true)
    })
    public void unirseAGrupoPublico(Long grupoId, Long usuarioId) {
        GrupoChat grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Grupo no encontrado"));

        if (!grupo.isActivo()) {
            throw new BadRequestException("El grupo no esta activo");
        }

        if (!"PUBLICO".equals(grupo.getTipo())) {
            throw new BadRequestException("Este grupo no es publico");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        var existente = miembroRepository.findByGrupoIdAndUsuarioId(grupoId, usuarioId);
        if (existente.isPresent()) {
            MiembroGrupo m = existente.get();
            if ("ACTIVO".equals(m.getEstado())) {
                throw new BadRequestException("Ya eres miembro de este grupo");
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

        miembroRepository.flush();

        registrarHistorial(grupoId, usuarioId, "MIEMBRO_UNIDO",
                null, usuario.getNombreUsuario(),
                "Se unio al grupo publico");

        crearMensajeSistema(grupoId, usuarioId,
                "@" + usuario.getNombreUsuario() + " se unio al grupo");
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_MIEMBROS_GRUPO, allEntries = true),
            @CacheEvict(value = CACHE_GRUPO, allEntries = true),
            @CacheEvict(value = CACHE_GRUPOS_USUARIO, allEntries = true),
            @CacheEvict(value = CACHE_HISTORIAL_GRUPO, allEntries = true),
            @CacheEvict(value = CACHE_MENSAJES_GRUPO, allEntries = true)
    })
    public void salirDelGrupo(Long grupoId, Long usuarioId) {
        MiembroGrupo miembro = miembroRepository.findByGrupoIdAndUsuarioId(grupoId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No perteneces a este grupo"));

        GrupoChat grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Grupo no encontrado"));

        if (grupo.getCreador().getId().equals(usuarioId)) {
            throw new SecurityException("Como creador, debes eliminar el grupo en vez de salir");
        }

        String nombreUsuario = miembro.getUsuario().getNombreUsuario();

        miembro.setEstado("SALIO");
        miembroRepository.save(miembro);
        miembroRepository.flush();

        registrarHistorial(grupoId, usuarioId, "SALIR",
                null, null,
                "@" + nombreUsuario + " salio del grupo");

        crearMensajeSistema(grupoId, usuarioId,
                "@" + nombreUsuario + " salio del grupo");
    }

    // ============================================================
    // ELIMINAR / EXPULSAR
    // ============================================================
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_MIEMBROS_GRUPO, allEntries = true),
            @CacheEvict(value = CACHE_GRUPO, allEntries = true),
            @CacheEvict(value = CACHE_GRUPOS_USUARIO, allEntries = true),
            @CacheEvict(value = CACHE_GRUPOS_PUBLICOS, allEntries = true),
            @CacheEvict(value = CACHE_MENSAJES_GRUPO, allEntries = true),
            @CacheEvict(value = CACHE_HISTORIAL_GRUPO, allEntries = true),
            @CacheEvict(value = CACHE_LINKS_GRUPO, allEntries = true)
    })
    public void eliminarGrupo(Long grupoId, Long usuarioId) {
        log.info("Usuario {} intentando eliminar grupo {}", usuarioId, grupoId);

        GrupoChat grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Grupo no encontrado"));

        if (!grupo.isActivo()) {
            throw new BadRequestException("El grupo ya fue eliminado");
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

        try {
            notificacionRepository.eliminarPorGrupo(grupoId);
        } catch (Exception e) {
            log.warn("Error al eliminar notificaciones: {}", e.getMessage());
        }

        registrarHistorial(grupoId, usuarioId, "ELIMINAR",
                nombreAntes, null, "Elimino el grupo");
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_MIEMBROS_GRUPO, allEntries = true),
            @CacheEvict(value = CACHE_GRUPO, allEntries = true),
            @CacheEvict(value = CACHE_GRUPOS_USUARIO, allEntries = true),
            @CacheEvict(value = CACHE_HISTORIAL_GRUPO, allEntries = true),
            @CacheEvict(value = CACHE_MENSAJES_GRUPO, allEntries = true)
    })
    public void expulsarMiembro(Long grupoId, Long adminId, Long usuarioAExpulsarId) {
        log.info("Admin {} expulsa a {} del grupo {}", adminId, usuarioAExpulsarId, grupoId);

        if (adminId.equals(usuarioAExpulsarId)) {
            throw new BadRequestException("No puedes expulsarte a ti mismo. Usa 'Salir del grupo'.");
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
            throw new BadRequestException("El usuario no es miembro activo del grupo");
        }

        if (grupo.getCreador().getId().equals(usuarioAExpulsarId)) {
            throw new SecurityException("No puedes expulsar al creador del grupo");
        }

        String nombreExpulsado = miembro.getUsuario().getNombreUsuario();

        miembro.setEstado("EXPULSADO");
        miembroRepository.save(miembro);
        miembroRepository.flush();

        registrarHistorial(grupoId, adminId, "EXPULSAR",
                nombreExpulsado, null, "Expulso a @" + nombreExpulsado);

        crearMensajeSistema(grupoId, adminId,
                "@" + nombreExpulsado + " fue expulsado del grupo");
    }

    // ============================================================
    // INVITACIONES PENDIENTES
    // ============================================================
    @Transactional(readOnly = true)
    public List<InvitacionGrupoDTO> listarInvitacionesPendientes(Long usuarioId) {
        log.debug("[DB] Listando invitaciones pendientes de: {}", usuarioId);
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

    // ============================================================
    // LISTAR MIEMBROS - con marcoId
    // ============================================================
    @Transactional(readOnly = true)
    public List<MiembroGrupoDTO> listarMiembros(Long grupoId, Long usuarioId) {
        log.debug("[DB] Listando miembros del grupo: {}", grupoId);

        if (usuarioId == null || !miembroRepository.esMiembroActivo(grupoId, usuarioId)) {
            boolean esPublico = grupoRepository.findById(grupoId)
                    .map(g -> "PUBLICO".equals(g.getTipo()) && g.isActivo())
                    .orElse(false);
            if (!esPublico) {
                throw new SecurityException("No tienes acceso a los miembros de este grupo");
            }
        }

        return miembroRepository.findMiembrosActivos(grupoId)
                .stream()
                .map(m -> {
                    Usuario u = m.getUsuario();
                    Boolean online = presenciaService.estaEnLinea(u.getId());
                    Instant ultima = presenciaService.obtenerUltimaConexion(u.getId());
                    String marcoId = resolverMarcoUsuario(u.getId());

                    return new MiembroGrupoDTO(
                            m.getId(),
                            u.getId(),
                            u.getNombreUsuario(),
                            u.getEmail(),
                            u.getFotoPerfilUrl(),
                            marcoId,
                            m.getRol(),
                            m.getEstado(),
                            m.getFechaUnion(),
                            m.getInvitadoPor() != null ? m.getInvitadoPor().getNombreUsuario() : null,
                            online,
                            ultima
                    );
                })
                .collect(Collectors.toList());
    }

    private GrupoDTO mapearADTO(GrupoChat grupo, Long usuarioId) {
        List<MiembroGrupoDTO> miembros;
        try {
            miembros = listarMiembros(grupo.getId(), usuarioId);
        } catch (SecurityException e) {
            miembros = List.of();
        }

        String rolDelUsuario = miembros.stream()
                .filter(m -> usuarioId != null && m.usuarioId().equals(usuarioId))
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
    // MENSAJES - con marcoEmisor
    // ============================================================
    @Transactional(readOnly = true)
    public List<MensajeGrupoDTO> obtenerMensajes(Long grupoId) {
        return obtenerMensajes(grupoId, null);
    }

    @Transactional(readOnly = true)
    public List<MensajeGrupoDTO> obtenerMensajes(Long grupoId, Long usuarioActualId) {
        log.debug("[DB] Obteniendo mensajes del grupo: {}", grupoId);

        if (usuarioActualId == null || !miembroRepository.esMiembroActivo(grupoId, usuarioActualId)) {
            throw new SecurityException("No eres miembro activo del grupo");
        }

        List<MensajeGrupo> mensajes = mensajeRepository.findMensajesDeGrupo(grupoId);
        return mensajes.stream()
                .map(m -> {
                    boolean esSticker = "STICKER".equals(m.getTipoMensaje());
                    String marcoEmisor = resolverMarcoUsuario(m.getEmisor().getId());

                    return new MensajeGrupoDTO(
                            m.getId(),
                            m.getGrupo().getId(),
                            m.getEmisor().getId(),
                            m.getEmisor().getNombreUsuario(),
                            m.getEmisor().getFotoPerfilUrl(),
                            marcoEmisor,
                            m.getContenido(),
                            m.getTipoMensaje(),
                            m.getUrlArchivo(),
                            m.getNombreArchivo(),
                            m.getFechaEnvio(),
                            usuarioActualId != null && m.getEmisor().getId().equals(usuarioActualId),
                            null,
                            esSticker ? m.getUrlArchivo() : null,
                            esSticker ? m.getNombreArchivo() : null
                    );
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public MensajeGrupoDTO enviarMensaje(Long grupoId, Long emisorId, String contenido) {
        log.info("Enviando mensaje - grupo: {}, emisor: {}", grupoId, emisorId);

        if (grupoId == null || emisorId == null) {
            throw new BadRequestException("Parametros invalidos");
        }
        if (contenido == null || contenido.isBlank()) {
            throw new BadRequestException("El contenido no puede estar vacio");
        }
        if (contenido.length() > MAX_MENSAJE) {
            throw new BadRequestException("El mensaje es demasiado largo");
        }

        GrupoChat grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Grupo no encontrado"));

        if (!grupo.isActivo()) {
            throw new BadRequestException("El grupo ya no esta activo");
        }

        if (!miembroRepository.esMiembroActivo(grupoId, emisorId)) {
            throw new SecurityException("No eres miembro activo de este grupo");
        }

        Usuario emisor = usuarioRepository.findById(emisorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        MensajeGrupo mensaje = MensajeGrupo.builder()
                .grupo(grupo)
                .emisor(emisor)
                .contenido(contenido.trim())
                .tipoMensaje("TEXTO")
                .fechaEnvio(Instant.now())
                .eliminado(false)
                .build();

        mensaje = mensajeRepository.save(mensaje);
        String marcoEmisor = resolverMarcoUsuario(emisor.getId());

        return new MensajeGrupoDTO(
                mensaje.getId(),
                grupo.getId(),
                emisor.getId(),
                emisor.getNombreUsuario(),
                emisor.getFotoPerfilUrl(),
                marcoEmisor,
                mensaje.getContenido(),
                mensaje.getTipoMensaje(),
                mensaje.getUrlArchivo(),
                mensaje.getNombreArchivo(),
                mensaje.getFechaEnvio(),
                true,
                null,
                null,
                null
        );
    }

    @Transactional
    public MensajeGrupoDTO enviarMensajeConArchivo(
            Long grupoId, Long emisorId, String contenido, MultipartFile archivo) {

        log.info("Usuario {} envia archivo al grupo {}", emisorId, grupoId);

        GrupoChat grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Grupo no encontrado"));

        if (!grupo.isActivo()) {
            throw new BadRequestException("El grupo ya no esta activo");
        }

        if (!miembroRepository.esMiembroActivo(grupoId, emisorId)) {
            throw new SecurityException("No eres miembro activo de este grupo");
        }

        Usuario emisor = usuarioRepository.findById(emisorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        String urlArchivo = null;
        String nombreArchivo = null;
        String tipoMensaje = "TEXTO";

        if (archivo != null && !archivo.isEmpty()) {
            if (archivo.getSize() > MAX_ARCHIVO_SIZE) {
                throw new BadRequestException("El archivo no puede exceder los 20MB");
            }
            try {
                urlArchivo = storageService.subirArchivo(archivo);
                nombreArchivo = archivo.getOriginalFilename();
                tipoMensaje = determinarTipoArchivo(archivo);
            } catch (Exception e) {
                log.warn("Error al subir archivo: {}", e.getMessage());
                throw new BadRequestException("No se pudo subir el archivo");
            }
        }

        MensajeGrupo mensaje = MensajeGrupo.builder()
                .grupo(grupo)
                .emisor(emisor)
                .contenido(contenido != null ? contenido.trim() : "")
                .tipoMensaje(tipoMensaje)
                .urlArchivo(urlArchivo)
                .nombreArchivo(nombreArchivo)
                .fechaEnvio(Instant.now())
                .eliminado(false)
                .build();

        mensaje = mensajeRepository.save(mensaje);
        String marcoEmisor = resolverMarcoUsuario(emisor.getId());

        return new MensajeGrupoDTO(
                mensaje.getId(),
                grupo.getId(),
                emisor.getId(),
                emisor.getNombreUsuario(),
                emisor.getFotoPerfilUrl(),
                marcoEmisor,
                mensaje.getContenido(),
                mensaje.getTipoMensaje(),
                mensaje.getUrlArchivo(),
                mensaje.getNombreArchivo(),
                mensaje.getFechaEnvio(),
                true,
                null,
                null,
                null
        );
    }

    private String determinarTipoArchivo(MultipartFile archivo) {
        String nombre = archivo.getOriginalFilename();
        if (nombre == null) return "documento";
        int dotIdx = nombre.lastIndexOf(".");
        if (dotIdx == -1) return "documento";
        String ext = nombre.substring(dotIdx + 1).toLowerCase();
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
            @CacheEvict(value = CACHE_HISTORIAL_GRUPO, allEntries = true)
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
            throw new BadRequestException("La foto es obligatoria");
        }

        String contentType = foto.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("Solo se permiten imagenes");
        }

        if (foto.getSize() > MAX_FOTO_SIZE) {
            throw new BadRequestException("La imagen no debe superar 5MB");
        }

        try {
            String url = storageService.subirArchivo(foto);
            grupo.setFotoUrl(url);
            grupoRepository.save(grupo);

            registrarHistorial(grupoId, usuarioId, "CAMBIAR_FOTO",
                    null, url, "Cambio la foto del grupo");

            return mapearADTO(grupo, usuarioId);
        } catch (Exception e) {
            log.warn("Error al subir foto: {}", e.getMessage());
            throw new BadRequestException("No se pudo subir la foto");
        }
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_GRUPO, allEntries = true),
            @CacheEvict(value = CACHE_GRUPOS_USUARIO, allEntries = true),
            @CacheEvict(value = CACHE_GRUPOS_PUBLICOS, allEntries = true),
            @CacheEvict(value = CACHE_HISTORIAL_GRUPO, allEntries = true)
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
                fotoAntes, null, "Elimino la foto del grupo");

        return mapearADTO(grupo, usuarioId);
    }

    // ============================================================
    // EDITAR INFO
    // ============================================================
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_GRUPO, allEntries = true),
            @CacheEvict(value = CACHE_GRUPOS_USUARIO, allEntries = true),
            @CacheEvict(value = CACHE_GRUPOS_PUBLICOS, allEntries = true),
            @CacheEvict(value = CACHE_HISTORIAL_GRUPO, allEntries = true)
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

        if (nombre != null && !nombre.trim().isEmpty()) {
            String nombreNuevo = nombre.trim();
            if (nombreNuevo.length() > MAX_NOMBRE_GRUPO) {
                throw new BadRequestException("El nombre es demasiado largo");
            }
            String nombreAnterior = grupo.getNombre();

            if (!nombreNuevo.equals(nombreAnterior)) {
                grupo.setNombre(nombreNuevo);
                registrarHistorial(grupoId, usuarioId, "EDITAR_NOMBRE",
                        nombreAnterior, nombreNuevo, "Cambio el nombre del grupo");
                cambio = true;
            }
        }

        if (descripcion != null) {
            String descNueva = descripcion.trim();
            if (descNueva.length() > MAX_DESCRIPCION) {
                throw new BadRequestException("La descripcion es demasiado larga");
            }
            String descAnterior = grupo.getDescripcion() != null ? grupo.getDescripcion() : "";

            if (!descNueva.equals(descAnterior)) {
                grupo.setDescripcion(descNueva);
                registrarHistorial(grupoId, usuarioId, "EDITAR_DESC",
                        descAnterior.isEmpty() ? "(vacio)" : descAnterior,
                        descNueva.isEmpty() ? "(vacio)" : descNueva,
                        "Cambio la descripcion del grupo");
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
            @CacheEvict(value = CACHE_LINKS_GRUPO, allEntries = true),
            @CacheEvict(value = CACHE_HISTORIAL_GRUPO, allEntries = true)
    })
    public InvitacionLinkDTO generarLinkInvitacion(Long grupoId, Long usuarioId, CrearInvitacionLinkDTO dto) {
        log.info("Usuario {} genera link para grupo {}", usuarioId, grupoId);

        GrupoChat grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Grupo no encontrado"));

        if (!grupo.isActivo()) {
            throw new BadRequestException("El grupo no esta activo");
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
                "Genero un link de invitacion");

        return mapearLinkADTO(link);
    }

    @Transactional(readOnly = true)
    public List<InvitacionLinkDTO> listarLinksActivos(Long grupoId, Long usuarioId) {
        log.debug("[DB] Listando links activos del grupo: {}", grupoId);
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
                "Desactivo un link de invitacion");
    }

    @Transactional(readOnly = true)
    public InfoInvitacionDTO obtenerInfoInvitacion(String token) {
        log.debug("[DB] Obteniendo info de invitacion");

        if (token == null || token.isBlank()) {
            return new InfoInvitacionDTO(false, "Token invalido", null, null, null, null, null, null, null);
        }

        Optional<InvitacionLink> opt = invitacionLinkRepository.findByToken(token);

        if (opt.isEmpty()) {
            return new InfoInvitacionDTO(false, "Link invalido", null, null, null, null, null, null, null);
        }

        InvitacionLink link = opt.get();

        if (!link.esValido()) {
            String msg = "Este link ya expiro o alcanzo su limite de usos";
            if (!link.isActivo()) msg = "Este link fue desactivado";
            if (link.getExpiraEn() != null && Instant.now().isAfter(link.getExpiraEn())) {
                msg = "Este link ha expirado";
            }
            if (link.getUsosMaximos() > 0 && link.getUsosActuales() >= link.getUsosMaximos()) {
                msg = "Este link alcanzo su limite de usos";
            }
            return new InfoInvitacionDTO(false, msg, null, null, null, null, null, null, null);
        }

        GrupoChat grupo = link.getGrupo();
        long totalMiembros = miembroRepository.findMiembrosActivos(grupo.getId()).size();

        return new InfoInvitacionDTO(
                true,
                "Link valido",
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
            @CacheEvict(value = CACHE_HISTORIAL_GRUPO, allEntries = true),
            @CacheEvict(value = CACHE_MENSAJES_GRUPO, allEntries = true)
    })
    public GrupoDTO unirseConLink(String token, Long usuarioId) {
        log.info("Usuario {} intenta unirse con token", usuarioId);

        InvitacionLink link = invitacionLinkRepository.findByToken(token)
                .orElseThrow(() -> new RecursoNoEncontradoException("Link invalido"));

        if (!link.esValido()) {
            throw new BadRequestException("Este link ya no es valido");
        }

        GrupoChat grupo = link.getGrupo();

        if (!grupo.isActivo()) {
            throw new BadRequestException("El grupo ya no esta activo");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        Optional<MiembroGrupo> existente = miembroRepository.findByGrupoIdAndUsuarioId(grupo.getId(), usuarioId);

        if (existente.isPresent()) {
            MiembroGrupo m = existente.get();
            if ("ACTIVO".equals(m.getEstado())) {
                throw new BadRequestException("Ya eres miembro de este grupo");
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

        miembroRepository.flush();
        link.registrarUso();
        invitacionLinkRepository.save(link);

        registrarHistorial(grupo.getId(), usuarioId, "MIEMBRO_UNIDO",
                null, usuario.getNombreUsuario(),
                "Se unio mediante link");

        crearMensajeSistema(grupo.getId(), usuarioId,
                "@" + usuario.getNombreUsuario() + " se unio al grupo mediante link");

        try {
            notificacionService.crearNotificacionGrupo(
                    link.getCreador().getId(),
                    usuarioId,
                    "<strong>" + usuario.getNombreUsuario() + "</strong> se unio al grupo <strong>" + grupo.getNombre() + "</strong> mediante tu link",
                    grupo.getId(),
                    "/chat/grupo/" + grupo.getId()
            );
        } catch (Exception e) {
            log.warn("Error al notificar: {}", e.getMessage());
        }

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

    @Transactional
    @CacheEvict(value = CACHE_MENSAJES_GRUPO, key = "'grupo:' + #grupoId")
    public MensajeGrupoDTO enviarStickerGrupo(Long grupoId, Long emisorId,
                                              String stickerUrl, String stickerNombre) {
        log.info("Enviando sticker al grupo {} de {}", grupoId, emisorId);

        if (grupoId == null || emisorId == null || stickerUrl == null || stickerUrl.isBlank()) {
            throw new BadRequestException("Parametros invalidos");
        }

        if (!stickerUrl.startsWith(CLOUDINARY_PREFIX)) {
            throw new BadRequestException("URL de sticker no valida");
        }

        GrupoChat grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Grupo no encontrado"));

        if (!grupo.isActivo()) {
            throw new BadRequestException("El grupo ya no esta activo");
        }

        if (!miembroRepository.esMiembroActivo(grupoId, emisorId)) {
            throw new SecurityException("No eres miembro activo de este grupo");
        }

        Usuario emisor = usuarioRepository.findById(emisorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        MensajeGrupo mensaje = MensajeGrupo.builder()
                .grupo(grupo)
                .emisor(emisor)
                .contenido("")
                .tipoMensaje("STICKER")
                .urlArchivo(stickerUrl)
                .nombreArchivo(stickerNombre != null ? stickerNombre : "sticker")
                .fechaEnvio(Instant.now())
                .eliminado(false)
                .build();

        mensaje = mensajeRepository.save(mensaje);
        String marcoEmisor = resolverMarcoUsuario(emisor.getId());

        return new MensajeGrupoDTO(
                mensaje.getId(),
                grupo.getId(),
                emisor.getId(),
                emisor.getNombreUsuario(),
                emisor.getFotoPerfilUrl(),
                marcoEmisor,
                "",
                "STICKER",
                stickerUrl,
                stickerNombre,
                mensaje.getFechaEnvio(),
                true,
                null,
                stickerUrl,
                stickerNombre
        );
    }
}