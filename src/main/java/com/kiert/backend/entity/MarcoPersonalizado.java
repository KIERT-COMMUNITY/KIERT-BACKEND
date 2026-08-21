package com.kiert.backend.entity;

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

    @Column(name = "usuario_id")
    private Long usuarioId; // ID del usuario que lo subió
}