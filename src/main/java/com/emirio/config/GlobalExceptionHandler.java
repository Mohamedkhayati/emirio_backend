package com.emirio.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
