package com.kiert.backend.features.invitacion.service;

import com.kiert.backend.entity.InvitacionLink;
import com.kiert.backend.entity.GrupoChat; // <-- IMPORT CORREGIDO
import com.kiert.backend.entity.Usuario;
import com.kiert.backend.features.invitacion.repository.InvitacionLinkRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class InvitacionLinkService {

    private final InvitacionLinkRepository repository;
    private final SecureRandom random = new SecureRandom();

    // Lee la URL del application.yml. Si no existe, usa localhost por defecto.
    @Value("${kiert.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    public InvitacionLinkService(InvitacionLinkRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public InvitacionLink crearLink(GrupoChat grupo, Usuario creador, int usosMaximos, int horasExpiracion) { // <-- TIPO CORREGIDO
        InvitacionLink link = new InvitacionLink();
        link.setGrupo(grupo);
        link.setCreador(creador);
        link.setToken(generarTokenUnico());
        link.setUsosMaximos(usosMaximos);
        link.setUsosActuales(0);
        link.setActivo(true);
        link.setFechaCreacion(Instant.now());

        if (horasExpiracion > 0) {
            link.setExpiraEn(Instant.now().plus(horasExpiracion, ChronoUnit.HOURS));
        }

        return repository.save(link);
    }

    private String generarTokenUnico() {
        String token;
        do {
            // Genera un token alfanumérico (hex) de 12 caracteres
            byte[] bytes = new byte[6];
            random.nextBytes(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            token = sb.toString();
        } while (repository.existeTokenActivo(token));

        return token;
    }

    // Método helper para que el controlador devuelva la URL completa al frontend
    public String construirUrlInvitacion(String token) {
        return frontendUrl + "/invitacion/" + token;
    }
}