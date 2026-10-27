package com.kiert.backend.features.chatYArchivo.service;

import com.kiert.backend.entity.MensajeGlobalUsuario;
import com.kiert.backend.entity.PersonalizacionUsuario;
import com.kiert.backend.entity.Usuario;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.features.chatYArchivo.dto.MensajeGlobalUsuarioDTO;
import com.kiert.backend.features.chatYArchivo.repository.MensajeGlobalUsuarioRepository;
import com.kiert.backend.features.personalizacion.repository.PersonalizacionRepository;
import com.kiert.backend.features.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatGlobalUsuariosService {

    private final MensajeGlobalUsuarioRepository mensajeRepository;
    private final UsuarioRepository usuarioRepository;
    private final PersonalizacionRepository personalizacionRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private static final int LIMITE_INICIAL = 200;
    private static final String TOPIC = "/topic/chat-global-usuarios";

    // ============================================================
    // LISTAR
    // ============================================================
    @Transactional(readOnly = true)
    public List<MensajeGlobalUsuarioDTO> listar(Long usuarioActualId, Long desdeId) {
        List<MensajeGlobalUsuario> mensajes = (desdeId != null && desdeId > 0)
                ? mensajeRepository.findDesdeId(desdeId)
                : mensajeRepository.findUltimos(PageRequest.of(0, LIMITE_INICIAL));

        return mensajes.stream()
                .map(m -> mapear(m, usuarioActualId))
                .toList();
    }

    // ============================================================
    // ENVIAR
    // ============================================================
    @Transactional
    public MensajeGlobalUsuarioDTO enviar(Long emisorId, String contenido) {
        if (emisorId == null) {
            throw new IllegalArgumentException("Usuario requerido");
        }
        if (contenido == null || contenido.isBlank()) {
            throw new IllegalArgumentException("El contenido no puede estar vacío");
        }
        if (contenido.length() > 2000) {
            throw new IllegalArgumentException("El mensaje es demasiado largo");
        }

        Usuario emisor = usuarioRepository.findById(emisorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Emisor no encontrado"));

        MensajeGlobalUsuario mensaje = MensajeGlobalUsuario.builder()
                .emisor(emisor)
                .contenido(contenido.trim())
                .tipoMensaje("TEXTO")
                .build();

        mensaje = mensajeRepository.save(mensaje);

        MensajeGlobalUsuarioDTO dto = mapear(mensaje, null);

        try {
            messagingTemplate.convertAndSend(TOPIC, dto);
        } catch (Exception e) {
            log.error("Error broadcast chat global usuarios: {}", e.getMessage());
        }

        return mapear(mensaje, emisorId);
    }

    // ============================================================
    // MAPEO
    // ============================================================
    private MensajeGlobalUsuarioDTO mapear(MensajeGlobalUsuario m, Long usuarioActualId) {
        Long emisorId = m.getEmisor().getId();
        String foto = m.getEmisor().getFotoPerfilUrl();

        String marco = personalizacionRepository.findByUsuarioId(emisorId)
                .map(PersonalizacionUsuario::getMarcoId)
                .orElse("none");

        String fecha = m.getFechaEnvio() != null
                ? m.getFechaEnvio().truncatedTo(ChronoUnit.MILLIS).toString()
                : null;

        boolean propio = usuarioActualId != null && emisorId.equals(usuarioActualId);

        return new MensajeGlobalUsuarioDTO(
                m.getId(),
                emisorId,
                m.getEmisor().getNombreUsuario(),
                foto,
                marco,
                m.getContenido(),
                m.getTipoMensaje(),
                fecha,
                propio
        );
    }
}