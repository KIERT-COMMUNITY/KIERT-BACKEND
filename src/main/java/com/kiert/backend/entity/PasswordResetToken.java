package com.kiert.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_expiracion", nullable = false)
    private Instant fechaExpiracion;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    @Builder.Default
    private Instant fechaCreacion = Instant.now();

    @Column(nullable = false)
    @Builder.Default
    private boolean usado = false;

    // ✅ NUEVOS: campos que tenía la tabla SQL
    @Column(name = "fecha_uso")
    private Instant fechaUso;

    @Column(name = "ip_solicitante", length = 45)
    private String ipSolicitante;

    public boolean isExpirado() {
        return Instant.now().isAfter(fechaExpiracion);
    }

    @PrePersist
    public void prePersist() {
        if (fechaCreacion == null) fechaCreacion = Instant.now();
    }
}