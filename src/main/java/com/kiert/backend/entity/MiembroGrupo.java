package com.kiert.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "miembros_grupo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MiembroGrupo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grupo_id", nullable = false)
    private GrupoChat grupo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String rol = "MIEMBRO"; // ADMIN, MODERADOR, MIEMBRO

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = "ACTIVO"; // ACTIVO, PENDIENTE, RECHAZADO, EXPULSADO, SALIO

    @Column(name = "fecha_union", nullable = false)
    @Builder.Default
    private Instant fechaUnion = Instant.now();

    @Column(name = "fecha_invitacion")
    private Instant fechaInvitacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitado_por_id")
    private Usuario invitadoPor;
}