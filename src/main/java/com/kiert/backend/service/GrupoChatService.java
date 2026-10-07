package com.kiert.backend.service;

import com.kiert.backend.dto.ArchivoSubidoDTO;
import com.kiert.backend.dto.CrearGrupoDTO;
import com.kiert.backend.dto.GrupoDTO;
import com.kiert.backend.dto.InvitacionGrupoDTO;
import com.kiert.backend.dto.MensajeArchivoDTO;
import com.kiert.backend.dto.MensajeGrupoDTO;
import com.kiert.backend.dto.MiembroGrupoDTO;
import com.kiert.backend.entity.GrupoChat;
import com.kiert.backend.entity.MensajeArchivoGrupo;
import com.kiert.backend.entity.MensajeGrupo;
import com.kiert.backend.entity.MiembroGrupo;
import com.kiert.backend.entity.Usuario;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.repository.GrupoChatRepository;
import com.kiert.backend.repository.MensajeArchivoGrupoRepository;
import com.kiert.backend.repository.MensajeGrupoRepository;
import com.kiert.backend.repository.MiembroGrupoRepository;
import com.kiert.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class GrupoChatService {

    private final GrupoChatRepository grupoRepository;
    private final MiembroGrupoRepository miembroRepository;
    private final MensajeGrupoRepository mensajeRepository;
    private final MensajeArchivoGrupoRepository mensajeArchivoGrupoRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;
    private final ArchivoChatService archivoChatService;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public GrupoDTO crearGrupo(Long creadorId, CrearGrupoDTO dto) {
        if (dto.nombre() == null || dto.nombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del grupo es obligatorio");
        }

        Usuario creador = obtenerUsuario(creadorId);
        GrupoChat grupo = GrupoChat.builder()
                .nombre(dto.nombre().trim())
                .descripcion(dto.descripcion())
                .creador(creador)
                .tipo(dto.tipo() != null ? dto.tipo() : "PRIVADO")
                .fechaCreacion(Instant.now())
                .activo(true)
                .build();
        grupo = grupoRepository.save(grupo);

        miembroRepository.save(MiembroGrupo.builder()
                .grupo(grupo)
                .usuario(creador)
                .rol("ADMIN")
                .estado("ACTIVO")
                .fechaUnion(Instant.now())
                .build());

        if (dto.usuariosInvitados() != null) {
            for (Long usuarioId : dto.usuariosInvitados()) {
                if (usuarioId == null || usuarioId.equals(creadorId)) {
                    continue;
                }
                invitarUsuario(grupo, creador, usuarioId);
            }
        }

        return mapearADTO(grupo, creadorId);
    }

    @Transactional(readOnly = true)
    public List<GrupoDTO> listarMisGrupos(Long usuarioId) {
        return grupoRepository.findGruposDeUsuario(usuarioId)
                .stream()
                .map(grupo -> mapearADTO(grupo, usuarioId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GrupoDTO> listarGruposPublicos(Long usuarioId) {
        return grupoRepository.findGruposPublicosDisponibles(usuarioId)
                .stream()
                .map(grupo -> mapearADTO(grupo, usuarioId))
                .toList();
    }

    @Transactional(readOnly = true)
    public GrupoDTO obtenerGrupo(Long grupoId, Long usuarioId) {
        return mapearADTO(obtenerGrupoActivo(grupoId), usuarioId);
    }

    @Transactional
    public void invitarUsuarios(Long grupoId, Long invitadorId, List<Long> usuariosIds) {
        GrupoChat grupo = obtenerGrupoActivo(grupoId);
        Usuario invitador = obtenerUsuario(invitadorId);
        validarMiembroActivo(grupoId, invitadorId);

        if (usuariosIds == null) {
            return;
        }

        for (Long usuarioId : usuariosIds) {
            if (usuarioId == null
                    || usuarioId.equals(invitadorId)
                    || miembroRepository.findByGrupoIdAndUsuarioId(grupoId, usuarioId).isPresent()) {
                continue;
            }
            invitarUsuario(grupo, invitador, usuarioId);
        }
    }

    @Transactional
    public void aceptarInvitacion(Long grupoId, Long usuarioId) {
        MiembroGrupo miembro = obtenerMiembro(grupoId, usuarioId, "Invitacion no encontrada");
        if (!"PENDIENTE".equals(miembro.getEstado())) {
            throw new IllegalArgumentException("Esta invitacion ya fue procesada");
        }
        miembro.setEstado("ACTIVO");
        miembro.setFechaUnion(Instant.now());
        miembroRepository.save(miembro);
    }

    @Transactional
    public void rechazarInvitacion(Long grupoId, Long usuarioId) {
        MiembroGrupo miembro = obtenerMiembro(grupoId, usuarioId, "Invitacion no encontrada");
        miembro.setEstado("RECHAZADO");
        miembroRepository.save(miembro);
    }

    @Transactional
    public void unirseAGrupoPublico(Long grupoId, Long usuarioId) {
        GrupoChat grupo = obtenerGrupoActivo(grupoId);
        if (!"PUBLICO".equals(grupo.getTipo())) {
            throw new IllegalArgumentException("Este grupo no es publico");
        }

        Usuario usuario = obtenerUsuario(usuarioId);
        MiembroGrupo miembro = miembroRepository.findByGrupoIdAndUsuarioId(grupoId, usuarioId)
                .orElseGet(() -> MiembroGrupo.builder()
                        .grupo(grupo)
                        .usuario(usuario)
                        .rol("MIEMBRO")
                        .build());

        if ("ACTIVO".equals(miembro.getEstado())) {
            throw new IllegalArgumentException("Ya eres miembro de este grupo");
        }

        miembro.setEstado("ACTIVO");
        miembro.setFechaUnion(Instant.now());
        miembroRepository.save(miembro);
    }

    @Transactional
    public void salirDelGrupo(Long grupoId, Long usuarioId) {
        GrupoChat grupo = obtenerGrupoActivo(grupoId);
        MiembroGrupo miembro = obtenerMiembro(grupoId, usuarioId, "No perteneces a este grupo");

        if (grupo.getCreador().getId().equals(usuarioId)) {
            throw new SecurityException("Como creador, debes eliminar el grupo en vez de salir");
        }

        miembro.setEstado("SALIO");
        miembroRepository.save(miembro);
    }

    @Transactional
    public void eliminarGrupo(Long grupoId, Long usuarioId) {
        GrupoChat grupo = obtenerGrupoActivo(grupoId);
        MiembroGrupo miembro = obtenerMiembro(grupoId, usuarioId, "No eres miembro de este grupo");
        boolean esCreador = grupo.getCreador().getId().equals(usuarioId);
        boolean esAdmin = "ADMIN".equals(miembro.getRol());

        if (!esCreador && !esAdmin) {
            throw new SecurityException("Solo el creador o un administrador puede eliminar el grupo");
        }

        grupo.setActivo(false);
        grupoRepository.save(grupo);
    }

    @Transactional
    public void expulsarMiembro(Long grupoId, Long adminId, Long usuarioAExpulsarId) {
        if (adminId.equals(usuarioAExpulsarId)) {
            throw new IllegalArgumentException("No puedes expulsarte a ti mismo");
        }

        GrupoChat grupo = obtenerGrupoActivo(grupoId);
        MiembroGrupo admin = obtenerMiembro(grupoId, adminId, "No eres miembro de este grupo");
        if (!"ADMIN".equals(admin.getRol()) || !"ACTIVO".equals(admin.getEstado())) {
            throw new SecurityException("Solo los administradores activos pueden expulsar miembros");
        }

        MiembroGrupo miembro = obtenerMiembro(
                grupoId,
                usuarioAExpulsarId,
                "Usuario no pertenece al grupo"
        );
        if (!"ACTIVO".equals(miembro.getEstado())) {
            throw new IllegalArgumentException("El usuario no es miembro activo del grupo");
        }
        if (grupo.getCreador().getId().equals(usuarioAExpulsarId)) {
            throw new SecurityException("No puedes expulsar al creador del grupo");
        }

        miembro.setEstado("EXPULSADO");
        miembroRepository.save(miembro);
    }

    @Transactional(readOnly = true)
    public List<InvitacionGrupoDTO> listarInvitacionesPendientes(Long usuarioId) {
        return miembroRepository.findInvitacionesPendientes(usuarioId)
                .stream()
                .map(miembro -> new InvitacionGrupoDTO(
                        miembro.getId(),
                        miembro.getGrupo().getId(),
                        miembro.getGrupo().getNombre(),
                        miembro.getGrupo().getFotoUrl(),
                        miembro.getInvitadoPor() != null ? miembro.getInvitadoPor().getId() : null,
                        miembro.getInvitadoPor() != null
                                ? miembro.getInvitadoPor().getNombreUsuario()
                                : "Sistema",
                        miembro.getFechaInvitacion()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MiembroGrupoDTO> listarMiembros(Long grupoId) {
        return miembroRepository.findMiembrosActivos(grupoId)
                .stream()
                .map(this::convertirMiembroDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MensajeGrupoDTO> obtenerMensajes(Long grupoId) {
        return obtenerMensajes(grupoId, null);
    }

    @Transactional(readOnly = true)
    public List<MensajeGrupoDTO> obtenerMensajes(Long grupoId, Long usuarioActualId) {
        if (usuarioActualId != null) {
            validarMiembroActivo(grupoId, usuarioActualId);
        }
        return mensajeRepository.findMensajesDeGrupo(grupoId)
                .stream()
                .map(mensaje -> convertirMensajeDTO(mensaje, usuarioActualId))
                .toList();
    }

    @Transactional
    public MensajeGrupoDTO enviarMensaje(Long grupoId, Long emisorId, String contenido) {
        return enviarMensajeConArchivos(grupoId, emisorId, contenido, List.of());
    }

    @Transactional
    public MensajeGrupoDTO enviarMensajeConArchivos(
            Long grupoId,
            Long emisorId,
            String contenido,
            List<MultipartFile> archivos
    ) {
        GrupoChat grupo = obtenerGrupoActivo(grupoId);
        validarMiembroActivo(grupoId, emisorId);
        Usuario emisor = obtenerUsuario(emisorId);
        String contenidoNormalizado = normalizarContenido(contenido);
        List<MultipartFile> archivosValidos = normalizarArchivos(archivos);

        if (contenidoNormalizado == null && archivosValidos.isEmpty()) {
            throw new IllegalArgumentException("El mensaje debe contener texto o al menos un archivo");
        }

        List<ArchivoSubidoDTO> archivosSubidos = new ArrayList<>();
        try {
            if (!archivosValidos.isEmpty()) {
                archivosSubidos = archivoChatService.validarYSubir(
                        archivosValidos,
                        "chat/grupos/" + grupoId
                );
            }

            MensajeGrupo mensaje = MensajeGrupo.builder()
                    .grupo(grupo)
                    .emisor(emisor)
                    .contenido(contenidoNormalizado)
                    .tipoMensaje(determinarTipoMensaje(contenidoNormalizado, archivosSubidos))
                    .fechaEnvio(Instant.now())
                    .eliminado(false)
                    .build();

            if (!archivosSubidos.isEmpty()) {
                ArchivoSubidoDTO primero = archivosSubidos.get(0);
                mensaje.setUrlArchivo(primero.secureUrl());
                mensaje.setNombreArchivo(primero.nombreOriginal());
            }

            mensaje = mensajeRepository.save(mensaje);
            for (ArchivoSubidoDTO archivo : archivosSubidos) {
                mensaje.agregarArchivo(crearEntidadArchivo(archivo));
            }
            if (!archivosSubidos.isEmpty()) {
                mensaje = mensajeRepository.save(mensaje);
            }

            MensajeGrupoDTO dto = convertirMensajeDTO(mensaje, emisorId);
            notificarMensajeGrupo(grupoId, dto);
            return dto;
        } catch (RuntimeException exception) {
            if (!archivosSubidos.isEmpty()) {
                archivoChatService.eliminarArchivos(archivosSubidos);
            }
            throw exception;
        }
    }

    private GrupoDTO mapearADTO(GrupoChat grupo, Long usuarioId) {
        List<MiembroGrupoDTO> miembros = listarMiembros(grupo.getId());
        String rolDelUsuario = miembros.stream()
                .filter(miembro -> miembro.usuarioId().equals(usuarioId))
                .findFirst()
                .map(MiembroGrupoDTO::rol)
                .orElse(null);
        int miembrosEnLinea = Math.toIntExact(
                miembroRepository.countMiembrosActivosEnLinea(grupo.getId())
        );

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
                rolDelUsuario,
                miembrosEnLinea
        );
    }

    private MiembroGrupoDTO convertirMiembroDTO(MiembroGrupo miembro) {
        Usuario usuario = miembro.getUsuario();
        return new MiembroGrupoDTO(
                miembro.getId(),
                usuario.getId(),
                usuario.getNombreUsuario(),
                usuario.getEmail(),
                usuario.getFotoPerfilUrl(),
                miembro.getRol(),
                miembro.getEstado(),
                miembro.getFechaUnion(),
                miembro.getInvitadoPor() != null
                        ? miembro.getInvitadoPor().getNombreUsuario()
                        : null,
                Boolean.TRUE.equals(usuario.getEnLinea()),
                usuario.getUltimaConexion()
        );
    }

    private MensajeGrupoDTO convertirMensajeDTO(
            MensajeGrupo mensaje,
            Long usuarioActualId
    ) {
        List<MensajeArchivoDTO> archivos = mensaje.getArchivos() == null
                ? new ArrayList<>()
                : mensaje.getArchivos().stream()
                .map(this::convertirArchivoDTO)
                .toList();

        if (archivos.isEmpty() && mensaje.getUrlArchivo() != null) {
            archivos = List.of(new MensajeArchivoDTO(
                    null,
                    mensaje.getNombreArchivo(),
                    mensaje.getUrlArchivo(),
                    normalizarTipoFrontend(mensaje.getTipoMensaje()),
                    null,
                    false
            ));
        }

        return new MensajeGrupoDTO(
                mensaje.getId(),
                mensaje.getGrupo().getId(),
                mensaje.getEmisor().getId(),
                mensaje.getEmisor().getNombreUsuario(),
                mensaje.getEmisor().getFotoPerfilUrl(),
                mensaje.getContenido(),
                mensaje.getTipoMensaje(),
                mensaje.getUrlArchivo(),
                mensaje.getNombreArchivo(),
                mensaje.getFechaEnvio(),
                usuarioActualId != null
                        && mensaje.getEmisor().getId().equals(usuarioActualId),
                archivos.isEmpty() ? null : archivos
        );
    }

    private MensajeArchivoDTO convertirArchivoDTO(MensajeArchivoGrupo archivo) {
        Integer pesoKb = archivo.getTamanoBytes() == null
                ? null
                : Math.toIntExact((archivo.getTamanoBytes() + 1023L) / 1024L);
        return new MensajeArchivoDTO(
                archivo.getId(),
                archivo.getNombreArchivo(),
                archivo.getUrlArchivo(),
                normalizarTipoFrontend(archivo.getTipoArchivo()),
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

    private MensajeArchivoGrupo crearEntidadArchivo(ArchivoSubidoDTO archivo) {
        return MensajeArchivoGrupo.builder()
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

    private void invitarUsuario(GrupoChat grupo, Usuario invitador, Long usuarioId) {
        Usuario invitado = usuarioRepository.findById(usuarioId).orElse(null);
        if (invitado == null) {
            return;
        }

        String estado = "PUBLICO".equals(grupo.getTipo()) ? "ACTIVO" : "PENDIENTE";
        miembroRepository.save(MiembroGrupo.builder()
                .grupo(grupo)
                .usuario(invitado)
                .rol("MIEMBRO")
                .estado(estado)
                .fechaUnion(Instant.now())
                .fechaInvitacion(Instant.now())
                .invitadoPor(invitador)
                .build());

        if ("PRIVADO".equals(grupo.getTipo())) {
            try {
                notificacionService.crearNotificacionGrupo(
                        usuarioId,
                        invitador.getId(),
                        "<strong>" + invitador.getNombreUsuario()
                                + "</strong> te invito al grupo <strong>"
                                + grupo.getNombre() + "</strong>",
                        grupo.getId(),
                        "/chat/grupo/" + grupo.getId()
                );
            } catch (RuntimeException exception) {
                log.error("No se pudo crear la notificacion del grupo", exception);
            }
        }
    }

    private GrupoChat obtenerGrupoActivo(Long grupoId) {
        GrupoChat grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Grupo no encontrado"));
        if (!grupo.isActivo()) {
            throw new IllegalArgumentException("El grupo fue eliminado");
        }
        return grupo;
    }

    private Usuario obtenerUsuario(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }

    private MiembroGrupo obtenerMiembro(
            Long grupoId,
            Long usuarioId,
            String mensajeError
    ) {
        return miembroRepository.findByGrupoIdAndUsuarioId(grupoId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(mensajeError));
    }

    private void validarMiembroActivo(Long grupoId, Long usuarioId) {
        if (!miembroRepository.esMiembroActivo(grupoId, usuarioId)) {
            throw new SecurityException("No eres miembro activo de este grupo");
        }
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

    private String determinarTipoMensaje(
            String contenido,
            List<ArchivoSubidoDTO> archivos
    ) {
        if (archivos.isEmpty()) {
            return "TEXTO";
        }
        boolean mismoTipo = archivos.stream()
                .map(ArchivoSubidoDTO::tipoArchivo)
                .distinct()
                .count() == 1;
        if (contenido == null && mismoTipo) {
            return archivos.get(0).tipoArchivo();
        }
        return "MULTIMEDIA";
    }

    private String normalizarTipoFrontend(String tipo) {
        return tipo == null ? "documento" : tipo.toLowerCase(Locale.ROOT);
    }

    private void notificarMensajeGrupo(Long grupoId, MensajeGrupoDTO mensaje) {
        try {
            messagingTemplate.convertAndSend(
                    "/topic/grupos/" + grupoId + "/mensajes",
                    mensaje
            );
        } catch (RuntimeException exception) {
            log.error(
                    "No se pudo notificar el mensaje del grupo {} por WebSocket",
                    grupoId,
                    exception
            );
        }
    }
}
