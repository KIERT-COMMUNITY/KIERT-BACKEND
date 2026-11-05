package com.kiert.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FeatureFlagService {

    @Value("${kiert.features.chat-privado.enabled:true}")
    private boolean chatPrivadoEnabled;

    public boolean isChatPrivadoEnabled() {
        return chatPrivadoEnabled;
    }
}