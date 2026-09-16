// src/main/java/com/kiert/backend/entity/RecursoBiblioteca.java
package com.kiert.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(
        name = "recursos_biblioteca",
        indexes = {
                @Index(name = "idx_categoria", columnList = "categoria"),
                @Index(name = "idx_destacado", columnList = "destacado"),
                @Index(name = "idx_activo", columnList = "activo"),
                @Index(name = "idx_usuario_id", columnList = "usuario_id"),
                @Index(name = "idx_es_usuario", columnList = "es_usuario"),
                @Index(name = "idx_usuario_fecha", columnList = "usuario_id, fecha_agregado")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecursoBiblioteca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔥 Relación con el usuario creador (NULL = recurso global del admin)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    @JsonIgnore // 👈 evita serializar el usuario completo (evita recursión)
    private Usuario usuario;

    @Column(name = "es_usuario", nullable = false)
    @Builder.Default
    private Boolean esUsuario = false;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(length = 1000) // 👈 ampliado (era 500)
    private String descripcion;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(nullable = false, length = 50)
    private String categoria; // certificacion, curso, video, articulo, herramienta, libro, idiomas, otro

    @Column(length = 100)
    private String subcategoria;

    @Column(length = 500)
    private String imagen;

    @Column(length = 150) // 👈 ampliado (era 100)
    private String autor;

    @Column(length = 150) // 👈 ampliado (era 100)
    private String plataforma;

    @Column(length = 80) // 👈 ampliado (era 50)
    private String duracion;

    @Column(length = 30) // 👈 ampliado (era 20)
    private String nivel; // principiante, intermedio, avanzado

    @Column(name = "destacado")
    @Builder.Default
    private Boolean destacado = false;

    @Column(name = "activo")
    @Builder.Default
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "fecha_agregado", nullable = false, updatable = false)
    private Instant fechaAgregado;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion")
    private Instant fechaActualizacion;

    @Column(length = 1000) // 👈 ampliado (era 500)
    private String tags; // Guardar como string separado por comas

    // ============================================================
    // MÉTODOS AUXILIARES
    // ============================================================

    /**
     * Comprueba si este recurso pertenece a un usuario concreto.
     */
    @Transient
    public boolean perteneceA(Long usuarioId) {
        return usuario != null && usuario.getId() != null && usuario.getId().equals(usuarioId);
    }

    /**
     * Indica si el recurso es editable/eliminable por un usuario.
     * Solo los recursos propios (esUsuario=true) son editables.
     */
    @Transient
    public boolean esEditablePor(Long usuarioId) {
        return Boolean.TRUE.equals(esUsuario) && perteneceA(usuarioId);
    }
}