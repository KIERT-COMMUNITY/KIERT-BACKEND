package com.kiert.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class KiertBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(KiertBackendApplication.class, args);
        System.out.println("backend corriendo");
    }
}
