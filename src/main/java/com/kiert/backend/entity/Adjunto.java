package com.kiert.backend.entity;

import jakarta.persistence.*;
import lombok.*;

// Espejo de Adjunto en post.model.ts: un post puede llevar archivos (Supabase) o links.
@Entity
@Table(name = "adjuntos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Adjunto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoAdjunto tipo; // ARCHIVO | LINK

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(name = "peso_kb")
    private Integer pesoKb; // solo aplica si tipo == ARCHIVO

    public enum TipoAdjunto {
        ARCHIVO("archivo"),
        LINK("link");

        private final String valor;

        TipoAdjunto(String valor) {
            this.valor = valor;
        }

        public String getValor() {
            return valor;
        }
    }
}
