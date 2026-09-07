// src/main/java/com/kiert/backend/entity/Documento.java
package com.kiert.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "documentos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Column(nullable = false, length = 100)
    private String categoria;

    @Column(name = "categoria_personalizada")
    private String categoriaPersonalizada;

    @Column(nullable = false, length = 1000)
    private String urlArchivo;

    @Column(length = 100)
    private String nombreArchivo;

    @Column(length = 50)
    private String tipoArchivo; // pdf, doc, docx, xls, ppt, txt, etc

    @Column(name = "tamano_kb")
    private Long tamanoKb;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "descargas")
    private Long descargas = 0L;

    @Column(name = "visitas")
    private Long visitas = 0L;

    @Column(name = "activo")
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion")
    private Instant fechaActualizacion;
}