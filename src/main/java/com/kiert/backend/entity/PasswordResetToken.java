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

    @Builder.Default
    private boolean usado = false;

    // ✅ MÉTODO PARA VERIFICAR SI EL TOKEN HA EXPIRADO
    public boolean isExpirado() {
        return Instant.now().isAfter(fechaExpiracion);
    }
}