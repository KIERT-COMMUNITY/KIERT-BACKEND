package com.kiert.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "compartidos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Compartido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "tipo_compartido", nullable = false, length = 20)
    @Builder.Default
    private String tipoCompartido = "INTERNO";

    @Column(length = 500)
    private String comentario;

    @Column(name = "fecha_creacion", nullable = false)
    @Builder.Default
    private Instant fechaCreacion = Instant.now();
}