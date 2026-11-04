package com.kiert.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kiert.turnstile")
public record TurnstileProperties(
        boolean enabled,
        String siteKey,
        String secretKey,
        String verifyUrl
) {}