// src/main/java/com/kiert/backend/exception/GlobalExceptionHandler.java
package com.kiert.backend.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ============================================================
    // 1. ERRORES DE REGLA DE NEGOCIO (400 BAD REQUEST)
    // ============================================================
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, Object>> manejarBadRequest(BadRequestException ex) {
        log.warn("BadRequest: {}", ex.getMessage());
        return construirRespuesta(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> manejarIllegalArgument(IllegalArgumentException ex) {
        log.warn("IllegalArgument: {}", ex.getMessage());
        return construirRespuesta(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // ============================================================
    // 2. RATE LIMIT (429 TOO MANY REQUESTS)
    // ============================================================
    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<Map<String, Object>> manejarTooManyRequests(TooManyRequestsException ex) {
        log.warn("RateLimit alcanzado: {}", ex.getMessage());
        return construirRespuesta(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    }

    // ============================================================
    // 3. RECURSOS Y PERMISOS (404, 403)
    // ============================================================
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> manejarNoEncontrado(RecursoNoEncontradoException ex) {
        log.warn("No encontrado: {}", ex.getMessage());
        return construirRespuesta(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<Map<String, Object>> manejarAccesoDenegado(AccesoDenegadoException ex) {
        log.warn("Acceso denegado: {}", ex.getMessage());
        return construirRespuesta(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> manejarSecurity(SecurityException ex) {
        log.warn("Security: {}", ex.getMessage());
        return construirRespuesta(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    // ============================================================
    // 4. CAPTCHA (TURNSTILE - 403 FORBIDDEN)
    // ============================================================
    @ExceptionHandler(TurnstileValidationException.class)
    public ResponseEntity<Map<String, Object>> manejarTurnstile(TurnstileValidationException ex) {
        log.warn("Turnstile falló: {}", ex.getMessage());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.FORBIDDEN.value());
        body.put("error", "TURNSTILE_FAILED");
        body.put("mensaje", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    // ============================================================
    // 5. VALIDACIONES DE SPRING (400 BAD REQUEST)
    // ============================================================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> manejarValidacion(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new HashMap<>();
        for (FieldError err : ex.getBindingResult().getFieldErrors()) {
            errores.put(err.getField(), err.getDefaultMessage());
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("mensaje", "Validación fallida");
        body.put("detalles", errores);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> manejarJsonMalformado(HttpMessageNotReadableException ex) {
        return construirRespuesta(HttpStatus.BAD_REQUEST,
                "JSON malformado: " + ex.getMostSpecificCause().getMessage());
    }

    // ============================================================
    // 6. ERROR GENÉRICO (500 INTERNAL SERVER ERROR)
    // ============================================================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> manejarGenerico(Exception ex) {
        log.error("ERROR INESPERADO: {}", ex.getMessage(), ex);
        return construirRespuesta(HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getMessage() != null ? ex.getMessage() : "Error interno del servidor");
    }

    private ResponseEntity<Map<String, Object>> construirRespuesta(HttpStatus status, String mensaje) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("mensaje", mensaje);
        return ResponseEntity.status(status).body(body);
    }
}