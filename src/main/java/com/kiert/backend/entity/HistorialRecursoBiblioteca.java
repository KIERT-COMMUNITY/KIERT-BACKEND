// src/main/java/com/kiert/backend/entity/HistorialRecursoBiblioteca.java
package com.kiert.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "historial_recursos_biblioteca",
        indexes = {
                @Index(name = "idx_historial_recurso", columnList = "recurso_id, fecha_accion"),
                @Index(name = "idx_historial_usuario", columnList = "usuario_id, fecha_accion")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistorialRecursoBiblioteca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurso_id", nullable = false)
    private RecursoBiblioteca recurso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 20)
    private String accion; // CREAR, EDITAR, ELIMINAR

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datos_anteriores", columnDefinition = "json")
    private String datosAnteriores;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datos_nuevos", columnDefinition = "json")
    private String datosNuevos;

    @Column(name = "ip_usuario", length = 45)
    private String ipUsuario;

    @Column(name = "fecha_accion", nullable = false)
    private LocalDateTime fechaAccion;

    @PrePersist
    public void prePersist() {
        if (fechaAccion == null) fechaAccion = LocalDateTime.now();
    }
}