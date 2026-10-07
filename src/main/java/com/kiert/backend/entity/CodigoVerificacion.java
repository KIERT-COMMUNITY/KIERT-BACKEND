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
        @Index(name = "idx_codigo_codigo", columnList = "codigo"),
        @Index(name = "idx_codigo_tipo", columnList = "tipo"),
        @Index(name = "idx_codigo_usado", columnList = "usado"),
        @Index(name = "idx_codigo_expira", columnList = "fecha_expiracion")
})
public class CodigoVerificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ============================================================
    // CAMPOS PRINCIPALES
    // ============================================================

    @Column(nullable = false, length = 120)
    private String email;

    @Column(nullable = false, length = 6)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoCodigo tipo;

    // ============================================================
    // FECHAS
    // ============================================================

    @Column(name = "fecha_creacion", nullable = false)
    private Instant fechaCreacion;

    @Column(name = "fecha_expiracion", nullable = false)
    private Instant fechaExpiracion;

    @Column(name = "fecha_uso")
    private Instant fechaUso;                    //NUEVO: cuándo se consumió

    // ============================================================
    // ESTADO
    // ============================================================

    @Column(nullable = false)
    @Builder.Default
    private Boolean usado = false;

    // ============================================================
    // AUDITORÍA (metadata de la solicitud)
    // ============================================================

    @Column(name = "ip_solicitante", length = 45)
    private String ipSolicitante;                //NUEVO: IPv4 o IPv6

    @Column(name = "user_agent", length = 500)
    private String userAgent;                    //NUEVO: navegador/cliente

    // ============================================================
    // CICLO DE VIDA
    // ============================================================

    @PrePersist
    public void prePersist() {
        if (fechaCreacion == null) fechaCreacion = Instant.now();
        if (fechaExpiracion == null) fechaExpiracion = fechaCreacion.plusSeconds(15 * 60);
        if (usado == null) usado = false;
    }

    // ============================================================
    // MÉTODOS AUXILIARES
    // ============================================================

    /**
     * Verifica si el código está vigente (no usado y no expirado).
     */
    public boolean estaVigente() {
        return !Boolean.TRUE.equals(usado)
                && fechaExpiracion != null
                && Instant.now().isBefore(fechaExpiracion);
    }

    /**
     * Marca el código como usado y registra la fecha de uso.
     */
    public void consumir() {
        this.usado = true;
        this.fechaUso = Instant.now();
    }
}