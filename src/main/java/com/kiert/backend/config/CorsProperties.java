// CorsProperties.java - ACTUALIZADO
package com.kiert.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@ConfigurationProperties(prefix = "kiert.cors")
public record CorsProperties(List<String> allowedOrigins) {
    // ✅ Cambiado de String a List<String>
}