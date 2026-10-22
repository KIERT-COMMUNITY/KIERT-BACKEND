package com.kiert.backend.features.post.dto;

public record CompartirPostDTO(
        String tipoCompartido,  // "INTERNO" o "EXTERNO"
        String comentario
) {}