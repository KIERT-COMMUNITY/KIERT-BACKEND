package com.kiert.backend.dto;

public record CompartirPostDTO(
        String tipoCompartido,  // "INTERNO" o "EXTERNO"
        String comentario
) {}