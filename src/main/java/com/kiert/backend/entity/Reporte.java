package com.kiert.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "reportes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_reportante_id", nullable = false)
    private Usuario usuarioReportante;

    @Column(name = "tipo_reporte", nullable = false, length = 20)
    private String tipoReporte;

    @Column(nullable = false, length = 50)
    private String motivo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comentario_id")
    private Comentario comentario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "respuesta_id")
    private RespuestaComentario respuesta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_reportado_id")
    private Usuario usuarioReportado;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = "PENDIENTE";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revisado_por_id")
    private Usuario revisadoPor;

    @Column(name = "fecha_revision")
    private Instant fechaRevision;

    @Column(name = "nota_moderador", columnDefinition = "TEXT")
    private String notaModerador;

    @Column(name = "accion_tomada", length = 50)
    private String accionTomada;

    @Column(name = "fecha_creacion", nullable = false)
    @Builder.Default
    private Instant fechaCreacion = Instant.now();

    @Column(name = "ip_usuario", length = 45)
    private String ipUsuario;
}