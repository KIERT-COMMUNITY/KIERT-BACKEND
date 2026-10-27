package com.kiert.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "historial_bienvenida")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistorialBienvenida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "titulo_anterior", length = 200)
    private String tituloAnterior;

    @Column(name = "contenido_anterior", columnDefinition = "TEXT")
    private String contenidoAnterior;

    @Column(name = "titulo_nuevo", nullable = false, length = 200)
    private String tituloNuevo;

    @Column(name = "contenido_nuevo", nullable = false, columnDefinition = "TEXT")
    private String contenidoNuevo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cambiado_por_id")
    private Usuario cambiadoPor;

    @Column(name = "fecha_cambio", nullable = false)
    private Instant fechaCambio;

    @Column(name = "ip_cambio", length = 45)
    private String ipCambio;
}