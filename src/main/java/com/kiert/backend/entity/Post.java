package com.kiert.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    // ✅ CAMPO STRING - Acepta cualquier categoría
    @Column(nullable = false, length = 50)
    private String categoria;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Adjunto> adjuntos = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Comentario> comentarios = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Reaccion> reacciones = new ArrayList<>();

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    @Builder.Default
    private Instant fechaCreacion = Instant.now();

    @Column(name = "fecha_actualizacion")
    private Instant fechaActualizacion;

    @Column(name = "fecha_eliminacion")
    private Instant fechaEliminacion;

    @Column(nullable = false)
    @Builder.Default
    private boolean eliminado = false;
    // ✅ NUEVO CAMPO: TOTAL COMPARTIDOS
    @Column(name = "total_compartidos", nullable = false)
    @Builder.Default
    private Long totalCompartidos = 0L;

}