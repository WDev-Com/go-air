package com.go_air.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;

import java.security.SignatureException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {
	private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	
	// Handles Spring-wrapped database integrity violations
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        Map<String, Object> response = new HashMap<>();
        String message = ex.getRootCause() != null ? ex.getRootCause().getMessage() : ex.getMessage();

        /* The duplicate contact, email, passport_number, flightNumber, error message should be throw for 
           the particular method a by checking the proper case happening */
           
        response.put("status", "FAILED");
        response.put("message", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<Map<String, Object>> handleExpiredJwt(ExpiredJwtException ex) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "FAILED");
        resp.put("message", "JWT token has expired. Please login again.");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(resp);
    }

    @ExceptionHandler({MalformedJwtException.class, SignatureException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, Object>> handleInvalidJwt(Exception ex) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "FAILED");
        resp.put("exception", ex.getClass().getSimpleName()); 
        String message;
        if (ex instanceof ExpiredJwtException) {
            message = "JWT token has expired. Please login again. (" + ex.getMessage() + ")";
        } else if (ex instanceof MalformedJwtException) {
            message = "JWT token is malformed or invalid. (" + ex.getMessage() + ")";
        } else if (ex instanceof SignatureException) {
            message = "JWT signature validation failed. (" + ex.getMessage() + ")";
        } else if (ex instanceof IllegalArgumentException) {
            message = "JWT token is missing or invalid. (" + ex.getMessage() + ")";
        } else {
            message = "Invalid JWT token. (" + ex.getMessage() + ")";
        }

        resp.put("message", message);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(resp);
    }

	
    // Handle invalid enum or type mismatches in JSON
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        Map<String, Object> response = new HashMap<>();

        Throwable cause = ex.getCause();
        if (cause instanceof InvalidFormatException invalidFormat) {

            String fieldName = invalidFormat.getPath().stream()
                    .map(ref -> ref.getFieldName())
                    .collect(Collectors.joining("."));

            String allowedValues = "";
            if (invalidFormat.getTargetType() != null && invalidFormat.getTargetType().isEnum()) {
                Object[] constants = invalidFormat.getTargetType().getEnumConstants();
                allowedValues = Arrays.stream(constants)
                        .map(Object::toString)
                        .collect(Collectors.joining(", "));
            }

            String message = String.format(
                    "Invalid value '%s' for field '%s'. Allowed values: %s",
                    invalidFormat.getValue(), fieldName,
                    allowedValues.isEmpty() ? "N/A" : allowedValues
            );

            response.put("status", "FAILED");
            response.put("message", message);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        response.put("status", "FAILED");
        response.put("message", "Invalid request format: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // Handle runtime exceptions from services
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
    	logger.warn("Line No 113 GEH : RuntimeException caught: {}", ex.getMessage());
    	
    	Map<String, Object> response = new HashMap<>();
        response.put("status", "FAILED");
        response.put("message", "Runtime Exception : "+ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

   /*
    * The runtime exception is working properly because it is thrown from the methods itself 
    * when  the condition occurs but the PL SQL exception is not catch by the global exception 
    * handler so some work should be done 
    * how the the spring security and jwt filter chain causing the problem or the logical  problem 
    * it present in the code we should work on it 
    * 
    * A common issue arises when attempting to handle exceptions thrown within Spring Security's JWT 
    * filters using a global exception handler annotated with @ControllerAdvice and @ExceptionHandler.
    *  This is because @ControllerAdvice primarily catches exceptions that occur within the Spring MVC 
    *  dispatch process, specifically when requests reach your controllers. Spring Security filters, 
    *  including those handling JWT authentication and authorization, execute before the request reaches
    *   your controllers.
      
      The Problem:
         Filter Chain Execution: Spring Security filters operate early in the servlet filter chain.
          Exceptions like MalformedJwtException, ExpiredJwtException, or BadCredentialsException 
          (if thrown during JWT validation) occur within these filters.
          
         ControllerAdvice Scope: @ControllerAdvice and its @ExceptionHandler methods are part 
         of the Spring MVC framework and are designed to handle exceptions that originate from 
         within the controller layer. They are not in a position to intercept exceptions thrown 
         by filters earlier in the request processing pipeline.
    * */
    
    // Handle all other exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "FAILED");
        response.put("message", "Something went wrong: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
