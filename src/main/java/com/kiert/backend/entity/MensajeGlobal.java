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
        @Index(name = "idx_mensaje_global_fecha", columnList = "fecha_envio")
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
}