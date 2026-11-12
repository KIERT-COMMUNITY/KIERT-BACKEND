// src/main/java/com/kiert/backend/entity/MensajeGlobal.java
package com.kiert.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "mensaje_global", indexes = {
        @Index(name = "idx_mensaje_global_fecha", columnList = "fecha_envio"),
        @Index(name = "idx_mensaje_global_respuesta", columnList = "respuesta_a_id")
})
public class MensajeGlobal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario autor;

    @Column(nullable = false, length = 500)
    private String contenido;

    @Column(name = "fecha_envio", nullable = false)
    @Builder.Default
    private Instant fechaEnvio = Instant.now();

    @Column(name = "respuesta_a_id")
    private Long respuestaAId;
}