package com.kiert.backend.entity;

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
        for (CategoriaPost categoria : values()) {
            if (categoria.valor.equals(valor)) {
                return categoria;
            }
        }
        throw new IllegalArgumentException("Categoría no válida: " + valor);
    }
}