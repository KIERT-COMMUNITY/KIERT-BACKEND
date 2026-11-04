// src/main/java/com/kiert/backend/exception/TurnstileValidationException.java
package com.kiert.backend.exception;

public class TurnstileValidationException extends RuntimeException {
    public TurnstileValidationException(String message) {
        super(message);
    }
}