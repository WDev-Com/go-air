package com.go_air.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {
	
	 // Handle the invalid enum or type mismatches (e.g., wrong enum value in JSON)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        Map<String, Object> response = new HashMap<>();

        Throwable cause = ex.getCause();
        if (cause instanceof InvalidFormatException invalidFormat) {

            // Extract JSON field name
            String fieldName = invalidFormat.getPath().stream()
                    .map(ref -> ref.getFieldName())
                    .collect(Collectors.joining("."));

            // If the target type is an Enum, show allowed values
            String allowedValues = "";
            if (invalidFormat.getTargetType() != null && invalidFormat.getTargetType().isEnum()) {
                Object[] constants = invalidFormat.getTargetType().getEnumConstants();
                allowedValues = Arrays.stream(constants)
                        .map(Object::toString)
                        .collect(Collectors.joining(", "));
            }

            // Build user-friendly message
            String message = String.format(
                    "Invalid value '%s' for field '%s'. Allowed values: %s",
                    invalidFormat.getValue(), fieldName,
                    allowedValues.isEmpty() ? "N/A" : allowedValues
            );

            response.put("message", message);
            response.put("status", "FAILED");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // If not caused by InvalidFormatException → fallback
        response.put("message", "Invalid request format: " + ex.getMessage());
        response.put("status", "FAILED");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }


    // Handle RuntimeExceptions thrown from service
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", ex.getMessage());
        response.put("status", "FAILED");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // Handle database unique constraint violations
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        Map<String, Object> response = new HashMap<>();
        String message = ex.getRootCause() != null ? ex.getRootCause().getMessage() : ex.getMessage();

        // Simplify PostgreSQL unique constraint message
        if (message != null && message.contains("duplicate key value violates unique constraint")) {

            if (message.contains("(email)")) {
                message = "User with this email already exists!";
            } else if (message.contains("(contact)")) {
                message = "User with this contact number already exists!";
            } else if (message.contains("passport_number")) {
                message = "Passenger with this passport number already exists!";
            } else if (message.contains("flightNumber")) {
                message = "This flight has already been booked!";
            } else {
                message = "Duplicate value found!";
            }
        }


        response.put("message", message);
        response.put("status", "FAILED");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // Handle all other exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Something went wrong: " + ex.getMessage());
        response.put("status", "FAILED");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
