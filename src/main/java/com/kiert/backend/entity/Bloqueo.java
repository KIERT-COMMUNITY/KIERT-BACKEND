package com.kiert.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "bloqueos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bloqueo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_bloqueador_id", nullable = false)
    private Usuario usuarioBloqueador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_bloqueado_id", nullable = false)
    private Usuario usuarioBloqueado;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String tipo = "BLOQUEO"; // BLOQUEO, DENUNCIA

    @Column(nullable = false, length = 255)
    private String motivo;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    @Builder.Default
    private Instant fechaCreacion = Instant.now();

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;
}