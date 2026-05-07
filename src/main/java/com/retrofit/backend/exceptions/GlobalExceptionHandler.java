package com.retrofit.backend.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
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

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {

        Map<String, String> body = new HashMap<>();
        body.put("status", "400");
        body.put("error", "Bad Request");
        body.put("message", ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }


    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentialsException(BadCredentialsException ex, WebRequest request) {
        Map<String, String> body = new HashMap<>();
        body.put("status", "401");
        body.put("error", "Unauthorized");
        body.put("message", "Usuario o contraseña incorrectos");
        return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
    }

}
