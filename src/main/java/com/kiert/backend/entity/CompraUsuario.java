package com.kiert.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "compras_usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompraUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "tipo", length = 20, nullable = false)
    private String tipo; // marco, fondo, tema

    @Column(name = "item_id", length = 50, nullable = false)
    private String itemId;

    @Column(name = "precio", nullable = false)
    private Double precio;

    @Column(name = "fecha_compra")
    @Builder.Default
    private Instant fechaCompra = Instant.now();
}