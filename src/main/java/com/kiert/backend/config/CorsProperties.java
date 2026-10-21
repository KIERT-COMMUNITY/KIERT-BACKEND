// src/main/java/com/kiert/backend/config/CorsProperties.java
package com.kiert.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@ConfigurationProperties(prefix = "kiert.cors")
public record CorsProperties(List<String> allowedOrigins) {
    // Lista de orígenes permitidos desde application.yml
}