// src/main/java/com/kiert/backend/entity/MensajeGrupo.java
package com.kiert.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "mensajes_grupo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MensajeGrupo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ============================================================
    // RELACIONES
    // ============================================================

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grupo_id", nullable = false)
    private GrupoChat grupo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "emisor_id", nullable = false)
    private Usuario emisor;

    // ============================================================
    // CONTENIDO
    // ============================================================

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenido;

    @Column(name = "tipo_mensaje", length = 20)
    @Builder.Default
    private String tipoMensaje = "TEXTO";

    @Column(name = "url_archivo", length = 1000)
    private String urlArchivo;

    @Column(name = "nombre_archivo", length = 255)
    private String nombreArchivo;

    // ============================================================
    // FECHAS
    // ============================================================

    @Column(name = "fecha_envio", nullable = false, updatable = false)
    @Builder.Default
    private Instant fechaEnvio = Instant.now();

    // ============================================================
    // SOFT DELETE
    // ============================================================

    @Column(nullable = false)
    @Builder.Default
    private boolean eliminado = false;            // ✅ Ya lo tenías

    // ============================================================
    // MÉTODOS AUXILIARES
    // ============================================================

    public void marcarComoEliminado() {
        this.eliminado = true;
    }
}