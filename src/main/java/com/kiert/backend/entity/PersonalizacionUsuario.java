package com.kiert.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "personalizacion_usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonalizacionUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(name = "tema_id", length = 50)
    @Builder.Default
    private String temaId = "default";

    @Column(name = "marco_id", length = 50)
    @Builder.Default
    private String marcoId = "none";

    @Column(name = "fondo_id", length = 50)
    @Builder.Default
    private String fondoId = "default";

    @Column(name = "foto_perfil_url", length = 500)
    private String fotoPerfilUrl;

    @Column(name = "foto_portada_url", length = 500)
    private String fotoPortadaUrl;

    @Column(name = "marco_personalizado_url", length = 500)
    private String marcoPersonalizadoUrl;
}