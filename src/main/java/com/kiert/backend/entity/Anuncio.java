package com.kiert.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "anuncios")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Anuncio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String texto;

    // Imagen: TODOS opcionales (sin nullable = false)
    @Column(name = "imagen_url", length = 1000)
    private String imagenUrl;

    @Column(name = "imagen_public_id", length = 255)
    private String imagenPublicId;

    @Column(name = "imagen_formato", length = 20)
    private String imagenFormato;

    @Column(name = "imagen_peso_kb")
    private Integer imagenPesoKb;

    @Column(name = "imagen_ancho")
    private Integer imagenAncho;

    @Column(name = "imagen_alto")
    private Integer imagenAlto;

    @Column(name = "enlace_url", length = 1000)
    private String enlaceUrl;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(nullable = false)
    private Boolean destacado = false;

    @Column(name = "creado_por_id")
    private Long creadoPorId;

    @Column(name = "actualizado_por_id")
    private Long actualizadoPorId;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(name = "fecha_publicacion")
    private LocalDateTime fechaPublicacion;

    @Column(name = "fecha_expiracion")
    private LocalDateTime fechaExpiracion;
}