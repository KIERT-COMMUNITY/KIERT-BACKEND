// src/main/java/com/kiert/backend/config/PropertiesConfig.java
package com.kiert.backend.config;

import com.kiert.backend.security.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
public class PropertiesConfig {
}