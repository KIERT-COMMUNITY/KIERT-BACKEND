package com.kiert.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

// Espejo de "User" en user.model.ts del frontend.
@Entity
@Table(name = "usuarios", uniqueConstraints = {
        @UniqueConstraint(columnNames = "email"),
        @UniqueConstraint(columnNames = "nombre_usuario")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_usuario", nullable = false, length = 20)
    private String nombreUsuario;

    @Column(nullable = false)
    private String email;

    // Hash BCrypt, nunca se expone al frontend
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "foto_perfil_url")
    private String fotoPerfilUrl;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    @Builder.Default
    private Instant fechaCreacion = Instant.now();
}
