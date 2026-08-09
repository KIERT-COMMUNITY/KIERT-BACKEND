package com.kiert.backend.entity;

// Espejo exacto de Post['categoria'] en post.model.ts
public enum CategoriaPost {
    CASO_HACKING("caso-hacking"),
    AYUDA("ayuda"),
    HISTORIA("historia"),
    OTRO("otro");

    private final String valor;

    CategoriaPost(String valor) {
        this.valor = valor;
    }

    public String getValor() {
        return valor;
    }

    public static CategoriaPost desdeValor(String valor) {
        for (CategoriaPost c : values()) {
            if (c.valor.equals(valor)) return c;
        }
        throw new IllegalArgumentException("Categoría inválida: " + valor);
    }
}
