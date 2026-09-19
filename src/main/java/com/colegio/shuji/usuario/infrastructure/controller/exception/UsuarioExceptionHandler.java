package com.colegio.shuji.usuario.infrastructure.controller.exception;

import com.colegio.shuji.shared.application.dto.ErrorResponse;
import com.colegio.shuji.usuario.domain.exception.InvalidCredentialsException;
import com.colegio.shuji.usuario.domain.exception.UserAlreadyExistsException;
import com.colegio.shuji.usuario.domain.exception.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Manejador local de excepciones para el módulo integral de Usuarios y Autenticación. Priorizado
 * con @Order(Ordered.HIGHEST_PRECEDENCE).
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.colegio.shuji.usuario")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UsuarioExceptionHandler {

  @ExceptionHandler(InvalidCredentialsException.class)
  public ResponseEntity<ErrorResponse> handleInvalidCredentialsException(
      InvalidCredentialsException ex, HttpServletRequest request) {
    log.warn("Fallo de credenciales en {}: {}", request.getRequestURI(), ex.getMessage());
    ErrorResponse errorResponse =
        ErrorResponse.of(
            HttpStatus.UNAUTHORIZED.value(),
            ex.getErrorCode(),
            ex.getMessage(),
            request.getRequestURI());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
  }

  @ExceptionHandler(UserAlreadyExistsException.class)
  public ResponseEntity<ErrorResponse> handleUserAlreadyExistsException(
      UserAlreadyExistsException ex, HttpServletRequest request) {
    log.warn("Conflicto de usuario existente en {}: {}", request.getRequestURI(), ex.getMessage());
    ErrorResponse errorResponse =
        ErrorResponse.of(
            HttpStatus.CONFLICT.value(),
            ex.getErrorCode(),
            ex.getMessage(),
            request.getRequestURI());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
  }

  @ExceptionHandler(UserNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleUserNotFoundException(
      UserNotFoundException ex, HttpServletRequest request) {
    log.warn("Usuario no encontrado en {}: {}", request.getRequestURI(), ex.getMessage());
    ErrorResponse errorResponse =
        ErrorResponse.of(
            HttpStatus.NOT_FOUND.value(),
            ex.getErrorCode(),
            ex.getMessage(),
            request.getRequestURI());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
      IllegalArgumentException ex, HttpServletRequest request) {
    log.warn("Argumento inválido en {}: {}", request.getRequestURI(), ex.getMessage());
    ErrorResponse errorResponse =
        ErrorResponse.of(
            HttpStatus.BAD_REQUEST.value(),
            "BAD_REQUEST",
            ex.getMessage(),
            request.getRequestURI());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
  }
}
