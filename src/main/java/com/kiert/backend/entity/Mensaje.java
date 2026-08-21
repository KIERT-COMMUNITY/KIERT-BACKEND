package com.kiert.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "mensajes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "emisor_id", nullable = false)
    private Usuario emisor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receptor_id", nullable = false)
    private Usuario receptor;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenido;

    @Column(name = "fecha_envio", nullable = false, updatable = false)
    @Builder.Default
    private Instant fechaEnvio = Instant.now();

    @Column(nullable = false)
    @Builder.Default
    private boolean leido = false;

    @Column(name = "tipo_mensaje", length = 20)
    @Builder.Default
    private String tipoMensaje = "TEXTO";

    @Column(name = "url_archivo", length = 1000)
    private String urlArchivo;

    @Column(name = "nombre_archivo", length = 255)
    private String nombreArchivo;
}