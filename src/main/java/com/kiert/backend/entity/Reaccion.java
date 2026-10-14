// src/main/java/com/kiert/backend/entity/Reaccion.java
package com.kiert.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "reacciones",
        indexes = {
                @Index(name = "idx_reaccion_usuario_post", columnList = "usuario_id, post_id"),
                @Index(name = "idx_reaccion_usuario_comentario", columnList = "usuario_id, comentario_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comentario_id")
    private Comentario comentario;

    @Column(nullable = false, length = 10)
    private String tipo;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    @Builder.Default
    private Instant fechaCreacion = Instant.now();

    @Column(name = "fecha_actualizacion")
    private Instant fechaActualizacion;

    @PreUpdate
    public void preUpdate() {
        this.fechaActualizacion = Instant.now();
    }
}