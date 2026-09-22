// src/main/java/com/kiert/backend/entity/InvitacionLink.java
package com.kiert.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "invitaciones_grupo_link")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvitacionLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grupo_id", nullable = false)
    private GrupoChat grupo;

    @Column(nullable = false, unique = true, length = 36)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creador_id", nullable = false)
    private Usuario creador;

    @Column(name = "usos_maximos", nullable = false)
    @Builder.Default
    private Integer usosMaximos = 0; // 0 = ilimitado

    @Column(name = "usos_actuales", nullable = false)
    @Builder.Default
    private Integer usosActuales = 0;

    @Column(name = "expira_en")
    private Instant expiraEn;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    @Builder.Default
    private Instant fechaCreacion = Instant.now();

    @Column(name = "fecha_ultimo_uso")
    private Instant fechaUltimoUso;

    // ============================================================
    // Helpers
    // ============================================================
    public boolean esValido() {
        if (!activo) return false;
        if (expiraEn != null && Instant.now().isAfter(expiraEn)) return false;
        if (usosMaximos > 0 && usosActuales >= usosMaximos) return false;
        return true;
    }

    public void registrarUso() {
        this.usosActuales++;
        this.fechaUltimoUso = Instant.now();
        if (usosMaximos > 0 && usosActuales >= usosMaximos) {
            this.activo = false;
        }
    }
}