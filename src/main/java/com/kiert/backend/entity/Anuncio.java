// src/main/java/com/kiert/backend/entity/Anuncio.java
package com.kiert.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "anuncios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Anuncio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String texto;

    // ============================================================
    // IMAGEN (Cloudinary) - Todos opcionales
    // ============================================================
    @Column(name = "imagen_url", length = 1000)
    private String imagenUrl;

    @Column(name = "imagen_public_id", length = 255)
    private String imagenPublicId;

    @Column(name = "imagen_formato", length = 20)
    private String imagenFormato;

    @Column(name = "imagen_peso_kb")
    private Integer imagenPesoKb;

    @Column(name = "imagen_ancho")
    private Integer imagenAncho;

    @Column(name = "imagen_alto")
    private Integer imagenAlto;

    // ============================================================
    // ENLACE Y VISIBILIDAD
    // ============================================================
    @Column(name = "enlace_url", length = 1000)
    private String enlaceUrl;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean destacado = false;

    // ✅ NUEVO CAMPO: Indica si este anuncio va en el banner flotante
    @Column(name = "es_flotante", nullable = false)
    @Builder.Default
    private Boolean esFlotante = false;

    // ============================================================
    // AUDITORÍA
    // ============================================================
    @Column(name = "creado_por_id")
    private Long creadoPorId;

    @Column(name = "actualizado_por_id")
    private Long actualizadoPorId;

    // ============================================================
    // FECHAS
    // ============================================================
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(name = "fecha_publicacion")
    private LocalDateTime fechaPublicacion;

    @Column(name = "fecha_expiracion")
    private LocalDateTime fechaExpiracion;

    // ============================================================
    // LIFECYCLE HOOKS
    // ============================================================
    @PrePersist
    public void prePersist() {
        if (this.fechaCreacion == null) this.fechaCreacion = LocalDateTime.now();
        if (this.activo == null) this.activo = true;
        if (this.destacado == null) this.destacado = false;
        if (this.esFlotante == null) this.esFlotante = false;
    }

    @PreUpdate
    public void preUpdate() {
        this.fechaActualizacion = LocalDateTime.now();
    }
}