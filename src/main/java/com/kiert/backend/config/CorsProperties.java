package com.kiert.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kiert.cors")
public record CorsProperties(String allowedOrigins) {}
