package com.kiert.backend.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kiert.jwt")
public record JwtProperties(String secret, long expirationMs) {}
