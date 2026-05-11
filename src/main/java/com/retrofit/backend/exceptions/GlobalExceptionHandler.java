package com.retrofit.backend.exceptions;

import jakarta.persistence.EntityExistsException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        Map<String, String> body = new HashMap<>();
        body.put("status", "404");
        body.put("error", "Not Found");
        body.put("message", ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentialsException(BadCredentialsException ex, WebRequest request) {
        Map<String, String> body = new HashMap<>();
        body.put("status", "401");
        body.put("error", "Unauthorized");
        body.put("message", "Usuario o contraseña incorrectos");
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

    // Único manejador para IllegalArgumentException
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, String> body = new HashMap<>();

        if (ex.getMessage().contains("Project code already exists")) {
            body.put("code", "Este código de proyecto ya está registrado");
        }
        else if (ex.getMessage().contains("Estado de proyecto inválido")) {
            body.put("status", "El estado seleccionado no es válido");
        }
        else if (ex.getMessage().contains("Prioridad de proyecto inválida")) {
            body.put("priority", "La prioridad seleccionada no es válida");
        }

        // Workers
        else if (ex.getMessage().contains("Worker DNI already exists")) {
            body.put("dni", "Este DNI ya se encuentra registrado.");
        }
        else if (ex.getMessage().contains("Worker phone already exists")) {
            body.put("phone", "Este número de teléfono ya está en uso.");
        }
        else if (ex.getMessage().contains("User email is required for account")) {
            body.put("email", "El correo es obligatorio para crear la cuenta.");
        }

        // Users
        else if (ex.getMessage().contains("User username already exists")) {
            body.put("username", "Este nombre de usuario ya está en uso.");
        }
        else if (ex.getMessage().contains("User email already exists")) {
            body.put("email", "Este correo electrónico ya está registrado.");
        }

        else {
            body.put("general", ex.getMessage());
        }

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }
}