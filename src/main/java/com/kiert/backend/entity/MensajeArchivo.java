package com.kiert.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "archivos_mensaje")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MensajeArchivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mensaje_id", nullable = false)
    private Mensaje mensaje;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    @Column(name = "url_archivo", nullable = false, length = 1000)
    private String urlArchivo;

    @Column(name = "public_id", nullable = false, length = 500)
    private String publicId;

    @Column(name = "tipo_mime", nullable = false, length = 150)
    private String tipoMime;

    @Column(name = "tipo_archivo", nullable = false, length = 30)
    private String tipoArchivo;

    @Column(name = "formato", length = 30)
    private String formato;

    @Column(name = "resource_type", nullable = false, length = 20)
    private String resourceType;

    @Column(name = "tamano_bytes", nullable = false)
    private Long tamanoBytes;

    @Column(name = "duracion_segundos")
    private Double duracionSegundos;

    @Column(name = "ancho")
    private Integer ancho;

    @Column(name = "alto")
    private Integer alto;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    @Builder.Default
    private Instant fechaCreacion = Instant.now();

    @PrePersist
    private void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = Instant.now();
        }
    }
}
