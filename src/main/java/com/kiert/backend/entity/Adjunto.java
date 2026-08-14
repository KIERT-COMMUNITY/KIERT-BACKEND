package com.kiert.backend.entity;

import jakarta.persistence.*;
import lombok.*;

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
    private TipoAdjunto tipo;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(name = "peso_kb")
    private Integer pesoKb;

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

        public static TipoAdjunto desdeValor(String valor) {
            for (TipoAdjunto tipo : values()) {
                if (tipo.valor.equals(valor)) {
                    return tipo;
                }
            }
            throw new IllegalArgumentException("Tipo de adjunto no válido: " + valor);
        }
    }
}