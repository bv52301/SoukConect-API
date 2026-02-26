package com.souk.order.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class OrderGlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderGlobalExceptionHandler.class);

    // @Valid failures — returns each field's constraint message
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(Map.of(
                "error", "Validation failed",
                "fields", fieldErrors));
    }

    // Bad enum value or malformed JSON body
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleNotReadable(HttpMessageNotReadableException ex) {
        log.debug("Unreadable request body: {}", ex.getMessage());
        String msg = ex.getMostSpecificCause().getMessage();
        return ResponseEntity.badRequest().body(Map.of("error", "Invalid request body: " + msg));
    }

    // Unhandled exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Unexpected error in order-service", ex);
        return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error: " + ex.getMessage()));
    }
}
