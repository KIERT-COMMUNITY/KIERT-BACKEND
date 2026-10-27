package com.kiert.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "mensajes_global_usuarios", indexes = {
        @Index(name = "idx_mgu_fecha", columnList = "fecha_envio")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MensajeGlobalUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "emisor_id", nullable = false)
    private Usuario emisor;

    @Column(nullable = false, length = 2000)
    private String contenido;

    @Column(name = "tipo_mensaje", length = 20)
    @Builder.Default
    private String tipoMensaje = "TEXTO";

    @CreationTimestamp
    @Column(name = "fecha_envio", nullable = false, updatable = false)
    private Instant fechaEnvio;

    @Column(nullable = false)
    @Builder.Default
    private boolean eliminado = false;
}