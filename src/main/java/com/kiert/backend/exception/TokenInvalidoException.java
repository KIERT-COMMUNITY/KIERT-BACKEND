package com.kiert.backend.exception;

// Token de reseteo de contraseña vencido o inexistente
public class TokenInvalidoException extends RuntimeException {
    public TokenInvalidoException(String mensaje) {
        super(mensaje);
    }
}
