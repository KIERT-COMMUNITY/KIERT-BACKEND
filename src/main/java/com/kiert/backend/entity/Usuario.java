// src/main/java/com/kiert/backend/entity/Usuario.java
package com.kiert.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_usuario", unique = true, nullable = false, length = 20)
    private String nombreUsuario;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "foto_perfil_url")
    private String fotoPerfilUrl;

    @Column(name = "bio", length = 255)
    private String bio;

    @Column(name = "marco_id", length = 50)
    @Builder.Default
    private String marcoId = "none";

    // ============================================================
    // ESTADO DE LA CUENTA
    // ============================================================
    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = false;

    @Column(name = "email_verificado", nullable = false)
    @Builder.Default
    private Boolean emailVerificado = false;

    @Column(name = "fecha_verificacion_email")
    private Instant fechaVerificacionEmail;

    @Column(name = "en_linea", nullable = false)
    @Builder.Default
    private Boolean enLinea = false;

    @Column(name = "ultima_conexion")
    private Instant ultimaConexion;

    @Column(nullable = false)
    @Builder.Default
    private Boolean eliminado = false;

    @Column(name = "fecha_eliminacion")
    private Instant fechaEliminacion;

    // ============================================================
    // AUDITORIA DE PASSWORD
    // ============================================================
    @Column(name = "fecha_ultimo_cambio_password")
    private Instant fechaUltimoCambioPassword;

    @Column(name = "intentos_login_fallidos", nullable = false)
    @Builder.Default
    private Integer intentosLoginFallidos = 0;

    @Column(name = "bloqueado_hasta")
    private Instant bloqueadoHasta;

    // ============================================================
    // FECHAS
    // ============================================================
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    @Builder.Default
    private Instant fechaCreacion = Instant.now();

    @Column(name = "fecha_actualizacion")
    private Instant fechaActualizacion;

    // ============================================================
    // RELACIONES
    // ============================================================
    @OneToOne(mappedBy = "usuario", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private PersonalizacionUsuario personalizacion;

    @OneToMany(mappedBy = "autor", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Post> posts = new ArrayList<>();

    @OneToMany(mappedBy = "autor", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Comentario> comentarios = new ArrayList<>();

    @OneToMany(mappedBy = "emisor", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Mensaje> mensajesEnviados = new ArrayList<>();

    @OneToMany(mappedBy = "receptor", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Mensaje> mensajesRecibidos = new ArrayList<>();

    @OneToMany(mappedBy = "emisor", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SolicitudContacto> solicitudesEnviadas = new ArrayList<>();

    @OneToMany(mappedBy = "receptor", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SolicitudContacto> solicitudesRecibidas = new ArrayList<>();

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PasswordResetToken> resetTokens = new ArrayList<>();

    // ============================================================
    // METODOS AUXILIARES
    // ============================================================
    public String getMarcoId() {
        if (this.personalizacion != null && this.personalizacion.getMarcoId() != null) {
            return this.personalizacion.getMarcoId();
        }
        return "none";
    }

    @PreUpdate
    public void preUpdate() {
        this.fechaActualizacion = Instant.now();
    }
}