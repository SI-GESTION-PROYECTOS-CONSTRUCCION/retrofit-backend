package com.retrofit.backend.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateResourceException(DuplicateResourceException ex) {
        Map<String, String> body = new HashMap<>();
        body.put(ex.getField(), ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidEnumException.class)
    public ResponseEntity<Map<String, String>> handleInvalidEnumException(InvalidEnumException ex) {
        Map<String, String> body = new HashMap<>();
        body.put(ex.getField(), ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleResourceNotFoundException(ResourceNotFoundException ex,
            WebRequest request) {
        Map<String, String> body = new HashMap<>();
        body.put("status", "404");
        body.put("error", "Not Found");
        body.put("message", ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<Map<String, String>> handleLockedException(LockedException ex) {
        log.warn("Intento de acceso a cuenta bloqueada: {}", ex.getMessage());
        Map<String, String> body = new HashMap<>();
        body.put("status", "423");
        body.put("error", "Locked");
        body.put("message", ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.LOCKED);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {
        log.warn("Fallo de autenticación bloqueado: {}", ex.getMessage());
        Map<String, String> body = new HashMap<>();
        body.put("status", "401");
        body.put("error", "Unauthorized");
        body.put("message", ex.getMessage() != null && !ex.getMessage().isBlank() ? ex.getMessage()
                : "Usuario o contraseña incorrectos");
        return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, String> body = new HashMap<>();

        // Workers
        if (ex.getMessage().contains("Worker DNI already exists")) {
            body.put("dni", "Este DNI ya se encuentra registrado.");
        } else if (ex.getMessage().contains("Worker phone already exists")) {
            body.put("phone", "Este número de teléfono ya está en uso.");
        } else if (ex.getMessage().contains("User email is required for account")) {
            body.put("email", "El correo es obligatorio para crear la cuenta.");
        } else {
            body.put("general", ex.getMessage());
        }

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalStateException(IllegalStateException ex) {
        Map<String, String> body = new HashMap<>();
        body.put("general", ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAllExceptions(Exception ex) {
        log.error("Excepción no controlada capturada en GlobalExceptionHandler: ", ex);
        Map<String, Object> body = new HashMap<>();
        body.put("status", 500);
        body.put("error", "Internal Server Error");
        body.put("message", "Ha ocurrido un error inesperado en el servidor.");
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}