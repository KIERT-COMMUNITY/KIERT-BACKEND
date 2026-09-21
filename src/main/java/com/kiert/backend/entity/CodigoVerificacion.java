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
@Table(name = "codigos_verificacion", indexes = {
        @Index(name = "idx_codigo_email", columnList = "email"),
        @Index(name = "idx_codigo_codigo", columnList = "codigo")
})
public class CodigoVerificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String email;

    @Column(nullable = false, length = 6)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoCodigo tipo;

    @Column(nullable = false)
    private Instant fechaCreacion;

    @Column(nullable = false)
    private Instant fechaExpiracion;

    @Column(nullable = false)
    @Builder.Default
    private Boolean usado = false;

    @PrePersist
    public void prePersist() {
        if (fechaCreacion == null) fechaCreacion = Instant.now();
        if (fechaExpiracion == null) fechaExpiracion = fechaCreacion.plusSeconds(15 * 60);
        if (usado == null) usado = false;
    }

    public boolean estaVigente() {
        return !Boolean.TRUE.equals(usado) && Instant.now().isBefore(fechaExpiracion);
    }
}