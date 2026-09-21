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
@Table(name = "auditoria_usuario")
public class AuditoriaUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(length = 255)
    private String email;

    @Column(nullable = false, length = 60)
    private String evento;

    @Column(length = 500)
    private String descripcion;

    @Builder.Default
    @Column(nullable = false)
    private Boolean exito = true;

    @Column(length = 45)
    private String ip;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "datos_extra", columnDefinition = "JSON")
    private String datosExtra;

    @Column(name = "fecha_evento", nullable = false)
    @Builder.Default
    private Instant fechaEvento = Instant.now();
}