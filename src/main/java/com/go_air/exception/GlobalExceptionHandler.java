package com.go_air.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

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
