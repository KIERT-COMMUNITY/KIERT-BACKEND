// src/main/java/com/kiert/backend/entity/MarcoPersonalizado.java
package com.kiert.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "marcos_personalizados")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarcoPersonalizado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", length = 50, nullable = false)
    private String nombre;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @Column(name = "url_imagen", length = 500, nullable = false)
    private String urlImagen;

    @Column(name = "tipo", length = 20)
    @Builder.Default
    private String tipo = "circulo";

    @Column(name = "precio")
    @Builder.Default
    private Double precio = 0.0;

    @Column(name = "gratis")
    @Builder.Default
    private boolean gratis = true;

    @Column(name = "activo")
    @Builder.Default
    private boolean activo = true;

    @Column(name = "fecha_creacion")
    @Builder.Default
    private Instant fechaCreacion = Instant.now();

    // ✅ CORREGIDO: era Long usuarioId, ahora es relación
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    @JsonIgnore
    private Usuario usuario;
}