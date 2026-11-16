// src/main/java/com/kiert/backend/features/email/service/EmailAsyncService.java
package com.kiert.backend.features.email.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailAsyncService {

    private final EmailService emailService;

    @Async("emailTaskExecutor")
    public void enviarCodigoVerificacion(String email, String nombreUsuario, String codigo) {
        try {
            emailService.enviarCodigoVerificacion(email, nombreUsuario, codigo);
        } catch (Exception e) {
            log.error("Error enviando correo: {}", e.getMessage());
        }
    }
}