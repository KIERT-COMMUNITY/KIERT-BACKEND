// src/main/java/com/kiert/backend/entity/Sticker.java
package com.kiert.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "stickers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sticker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;



    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "url_imagen", nullable = false, length = 1000)
    private String urlImagen;

    @Column(name = "url_miniatura", length = 1000)
    private String urlMiniatura;

    @Column(name = "tipo_archivo", length = 20)
    private String tipoArchivo;  // image/webp, image/png, image/gif

    @Column(name = "peso_kb")
    private Integer pesoKb;

    @Column(name = "ancho")
    private Integer ancho;

    @Column(name = "alto")
    private Integer alto;

    @Column(name = "es_publico", nullable = false)
    private Boolean esPublico = false;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false)
    private Instant fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private Instant fechaActualizacion;
}