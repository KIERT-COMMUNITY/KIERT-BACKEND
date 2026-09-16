// src/main/java/com/kiert/backend/dto/RecursoUsuarioRequest.java
package com.kiert.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecursoUsuarioRequest {

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 200)
    private String titulo;

    @Size(max = 1000)
    private String descripcion;

    @NotBlank(message = "La URL es obligatoria")
    @Size(max = 1000)
    @Pattern(regexp = "^https?://.+", message = "La URL debe comenzar con http:// o https://")
    private String url;

    @NotBlank(message = "La categoría es obligatoria")
    @Size(max = 50)
    private String categoria;

    @Size(max = 100)
    private String subcategoria;

    @Size(max = 150)
    private String autor;

    @Size(max = 150)
    private String plataforma;

    @Size(max = 80)
    private String duracion;

    @Size(max = 30)
    private String nivel;

    private Boolean destacado;

    private List<String> tags;
}