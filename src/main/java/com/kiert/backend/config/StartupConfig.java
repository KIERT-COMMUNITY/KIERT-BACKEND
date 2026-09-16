// src/main/java/com/kiert/backend/config/StartupConfig.java
package com.kiert.backend.config;

import com.kiert.backend.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartupConfig {

    private final UsuarioService usuarioService;

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        log.info("🚀 Servidor iniciado - marcando todos los usuarios como desconectados");
        try {
            usuarioService.marcarTodosDesconectados();
        } catch (Exception e) {
            log.warn("Error marcando usuarios offline: {}", e.getMessage());
        }
    }
}