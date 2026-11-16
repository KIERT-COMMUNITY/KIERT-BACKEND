// src/main/java/com/kiert/backend/exception/TooManyRequestsException.java
package com.kiert.backend.exception;

public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException(String mensaje) {
        super(mensaje);
    }
}