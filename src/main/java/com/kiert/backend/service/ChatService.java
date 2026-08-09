package com.kiert.backend.service;

import com.kiert.backend.dto.ConversacionDTO;
import com.kiert.backend.dto.MensajeChatDTO;
import com.kiert.backend.entity.Mensaje;
import com.kiert.backend.entity.Usuario;
import com.kiert.backend.exception.RecursoNoEncontradoException;
import com.kiert.backend.repository.MensajeRepository;
import com.kiert.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Espejo del backend que necesita chat.service.ts: historial vía HTTP +
// tiempo real vía WebSocket/STOMP (ver comentario del propio archivo del frontend).
@Service
@RequiredArgsConstructor
public class ChatService {

    private final MensajeRepository mensajeRepository;
    private final UsuarioRepository usuarioRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional(readOnly = true)
    public List<ConversacionDTO> listarConversaciones(Long usuarioId) {
        // Nos quedamos con el mensaje más reciente por interlocutor (el repo ya
        // devuelve todo ordenado desc, así que el primero que veamos de cada uno gana).
        Map<Long, Mensaje> ultimoPorInterlocutor = new LinkedHashMap<>();
        for (Mensaje m : mensajeRepository.findTodosLosMensajesDeUsuario(usuarioId)) {
            Usuario interlocutor = m.getEmisor().getId().equals(usuarioId) ? m.getReceptor() : m.getEmisor();
            ultimoPorInterlocutor.putIfAbsent(interlocutor.getId(), m);
        }

        return ultimoPorInterlocutor.values().stream()
                .map(m -> {
                    Usuario interlocutor = m.getEmisor().getId().equals(usuarioId) ? m.getReceptor() : m.getEmisor();
                    long noLeidos = mensajeRepository.countByEmisorIdAndReceptorIdAndLeidoFalse(interlocutor.getId(), usuarioId);
                    return new ConversacionDTO(
                            interlocutor.getId(),
                            interlocutor.getNombreUsuario(),
                            interlocutor.getFotoPerfilUrl(),
                            m.getContenido(),
                            null, // "ultimaConexion": requeriría presencia en tiempo real, se deja para el WebSocket
                            noLeidos
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MensajeChatDTO> listarMensajes(Long usuarioActualId, Long otroUsuarioId) {
        return mensajeRepository.findConversacion(usuarioActualId, otroUsuarioId).stream()
                .map(m -> aDTO(m, usuarioActualId))
                .toList();
    }

    @Transactional
    public MensajeChatDTO enviarMensaje(Long emisorId, Long receptorId, String contenido) {
        Usuario emisor = usuarioRepository.findById(emisorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado."));
        Usuario receptor = usuarioRepository.findById(receptorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El destinatario no existe."));

        Mensaje mensaje = Mensaje.builder()
                .emisor(emisor)
                .receptor(receptor)
                .contenido(contenido)
                .build();

        mensaje = mensajeRepository.save(mensaje);

        // Empuja el mensaje en tiempo real al destinatario si está suscrito a su tópico
        MensajeChatDTO paraReceptor = aDTO(mensaje, receptorId);
        messagingTemplate.convertAndSend("/topic/chat/" + receptorId, paraReceptor);

        return aDTO(mensaje, emisorId);
    }

    private MensajeChatDTO aDTO(Mensaje mensaje, Long usuarioQueConsulta) {
        return new MensajeChatDTO(
                mensaje.getId(),
                mensaje.getEmisor().getId(),
                mensaje.getContenido(),
                mensaje.getFechaEnvio(),
                mensaje.getEmisor().getId().equals(usuarioQueConsulta)
        );
    }
}
