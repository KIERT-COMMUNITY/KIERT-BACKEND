// src/main/java/com/kiert/backend/exception/AccesoDenegadoException.java
package com.kiert.backend.exception;

public class AccesoDenegadoException extends RuntimeException {
    public AccesoDenegadoException(String message) {
        super(message);
    }
}