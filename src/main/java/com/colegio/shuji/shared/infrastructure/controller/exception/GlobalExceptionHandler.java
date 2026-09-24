package com.colegio.shuji.shared.infrastructure.controller.exception;

import com.colegio.shuji.shared.application.dto.ErrorResponse;
import com.colegio.shuji.shared.domain.exception.BusinessException;
import com.colegio.shuji.shared.domain.exception.ResourceNotFoundException;
import com.colegio.shuji.shared.domain.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/** Manejador global centralizado de excepciones para la API REST. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
      ResourceNotFoundException ex, HttpServletRequest request) {
    log.warn("Recurso no encontrado en {}: {}", request.getRequestURI(), ex.getMessage());
    ErrorResponse errorResponse =
        ErrorResponse.of(
            HttpStatus.NOT_FOUND.value(),
            ex.getErrorCode(),
            ex.getMessage(),
            request.getRequestURI());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
  }

  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<ErrorResponse> handleUnauthorizedException(
      UnauthorizedException ex, HttpServletRequest request) {
    log.warn("No autorizado en {}: {}", request.getRequestURI(), ex.getMessage());
    ErrorResponse errorResponse =
        ErrorResponse.of(
            HttpStatus.UNAUTHORIZED.value(),
            ex.getErrorCode(),
            ex.getMessage(),
            request.getRequestURI());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ErrorResponse> handleBadCredentialsException(
      BadCredentialsException ex, HttpServletRequest request) {
    log.warn("Credenciales inválidas en {}: {}", request.getRequestURI(), ex.getMessage());
    ErrorResponse errorResponse =
        ErrorResponse.of(
            HttpStatus.UNAUTHORIZED.value(),
            "INVALID_CREDENTIALS",
            "Credenciales de acceso inválidas",
            request.getRequestURI());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDeniedException(
      AccessDeniedException ex, HttpServletRequest request) {
    log.warn("Acceso denegado en {}: {}", request.getRequestURI(), ex.getMessage());
    ErrorResponse errorResponse =
        ErrorResponse.of(
            HttpStatus.FORBIDDEN.value(),
            "ACCESS_DENIED",
            "No cuenta con los permisos necesarios para realizar esta acción",
            request.getRequestURI());
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationExceptions(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    log.warn("Error de validación en {}: {}", request.getRequestURI(), ex.getMessage());
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult()
        .getAllErrors()
        .forEach(
            error -> {
              String fieldName = ((FieldError) error).getField();
              String errorMessage = error.getDefaultMessage();
              errors.put(fieldName, errorMessage);
            });

    ErrorResponse errorResponse =
        ErrorResponse.ofValidation(
            HttpStatus.BAD_REQUEST.value(),
            "Error de validación en los campos enviados",
            request.getRequestURI(),
            errors);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
  }

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ErrorResponse> handleBusinessException(
      BusinessException ex, HttpServletRequest request) {
    log.warn("Error de negocio en {}: {}", request.getRequestURI(), ex.getMessage());
    ErrorResponse errorResponse =
        ErrorResponse.of(
            HttpStatus.BAD_REQUEST.value(),
            ex.getErrorCode(),
            ex.getMessage(),
            request.getRequestURI());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
      IllegalArgumentException ex, HttpServletRequest request) {
    log.warn("Argumento inválido en {}: {}", request.getRequestURI(), ex.getMessage());
    ErrorResponse errorResponse =
        ErrorResponse.of(
            HttpStatus.BAD_REQUEST.value(),
            "ILLEGAL_ARGUMENT",
            ex.getMessage(),
            request.getRequestURI());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ErrorResponse> handleResponseStatusException(
      ResponseStatusException ex, HttpServletRequest request) {
    int status = ex.getStatusCode().value();
    String message = ex.getReason() == null ? "La solicitud no pudo completarse" : ex.getReason();
    log.warn("Solicitud rechazada en {} con HTTP {}: {}", request.getRequestURI(), status, message);
    ErrorResponse errorResponse = ErrorResponse.of(status, "HTTP_" + status, message, request.getRequestURI());
    return ResponseEntity.status(ex.getStatusCode()).body(errorResponse);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGlobalException(
      Exception ex, HttpServletRequest request) {
    log.error("Error inesperado en servidor procesando {}", request.getRequestURI(), ex);
    ErrorResponse errorResponse =
        ErrorResponse.of(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "INTERNAL_SERVER_ERROR",
            "Ocurrió un error interno en el servidor. Por favor intente más tarde.",
            request.getRequestURI());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
  }
}
