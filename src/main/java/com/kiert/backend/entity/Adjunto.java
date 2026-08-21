package com.kiert.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "adjuntos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Adjunto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false, length = 255)
    private String nombre;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(name = "peso_kb")
    private Integer pesoKb;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    @Builder.Default
    private Instant fechaCreacion = Instant.now();

    @Column(name = "duracion_segundos")
    private Integer duracionSegundos;

    @Column(name = "ancho")
    private Integer ancho;

    @Column(name = "alto")
    private Integer alto;

    @Column(name = "formato", length = 20)
    private String formato;

    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) {
            fechaCreacion = Instant.now();
        }
    }
}