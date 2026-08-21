package com.kiert.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableCaching
@EnableConfigurationProperties  // ✅ Habilita el uso de @ConfigurationProperties
public class KiertBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(KiertBackendApplication.class, args);
        System.out.println(" Backend Kiert corriendo");
        System.out.println(" Redis caché habilitado");
    }
}