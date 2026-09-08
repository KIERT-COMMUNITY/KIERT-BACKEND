// src/main/java/com/kiert/backend/entity/RecursoBiblioteca.java
package com.kiert.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "recursos_biblioteca")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecursoBiblioteca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(nullable = false, length = 50)
    private String categoria; // certificacion, curso, video, articulo, herramienta, libro

    @Column(length = 100)
    private String subcategoria;

    @Column(length = 500)
    private String imagen;

    @Column(length = 100)
    private String autor;

    @Column(length = 100)
    private String plataforma;

    @Column(length = 50)
    private String duracion;

    @Column(length = 20)
    private String nivel; // principiante, intermedio, avanzado

    @Column(name = "destacado")
    private Boolean destacado = false;

    @Column(name = "activo")
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "fecha_agregado", nullable = false, updatable = false)
    private Instant fechaAgregado;

    @Column(length = 500)
    private String tags; // Guardar como string separado por comas
}