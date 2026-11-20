package com.unifor.br.distrischool.alunoservice.security;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
@RestControllerAdvice
public class ApiErrors {
 @ExceptionHandler(IllegalArgumentException.class)
 ResponseEntity<?> invalid(IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("message", e.getMessage())); }
 @ExceptionHandler(ResponseStatusException.class)
 ResponseEntity<?> status(ResponseStatusException e) { return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", e.getReason() == null ? "Solicita??o n?o permitida" : e.getReason())); }
}