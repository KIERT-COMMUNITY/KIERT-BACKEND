package com.kiert.backend.exception;

// Se usa para el 409 que espera register.component.ts cuando el correo/usuario ya existe
public class RecursoDuplicadoException extends RuntimeException {
    public RecursoDuplicadoException(String mensaje) {
        super(mensaje);
    }
}
