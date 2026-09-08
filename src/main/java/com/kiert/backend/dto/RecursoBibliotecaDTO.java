// src/main/java/com/kiert/backend/dto/RecursoBibliotecaDTO.java
package com.kiert.backend.dto;

import java.time.Instant;
import java.util.List;

public record RecursoBibliotecaDTO(
        Long id,
        String titulo,
        String descripcion,
        String url,
        String categoria,
        String subcategoria,
        String imagen,
        String autor,
        String plataforma,
        String duracion,
        String nivel,
        Boolean destacado,
        Instant fechaAgregado,
        List<String> tags
) {}