package com.emirio.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult().getFieldErrors()
      .forEach(err -> errors.put(err.getField(), err.getDefaultMessage()));
    return ResponseEntity.badRequest().body(Map.of(
      "message", "Validation failed",
      "errors", errors
    ));
  }

  // THIS IS THE CRITICAL FIX - Handle ResponseStatusException first
  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<?> handleResponseStatusException(ResponseStatusException ex) {
    // Return the original status code from the exception (BAD_REQUEST, NOT_FOUND, etc.)
    return ResponseEntity
      .status(ex.getStatusCode())
      .body(Map.of(
        "message", ex.getReason(),
        "error", ex.getReason(),
        "status", ex.getStatusCode().value()
      ));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<?> handleAny(Exception ex) {
    // log it in console
    ex.printStackTrace();
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
      "message", "Server error",
      "error", ex.getMessage()
    ));
  }
}