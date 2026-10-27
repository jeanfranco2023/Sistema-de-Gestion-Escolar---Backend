package com.colegio.shuji.shared.infrastructure.controller.exception;

import com.colegio.shuji.shared.application.dto.ErrorResponse;
import com.colegio.shuji.shared.domain.exception.BusinessException;
import com.colegio.shuji.shared.domain.exception.ResourceNotFoundException;
import com.colegio.shuji.shared.domain.exception.UnauthorizedException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/** Manejador global centralizado de excepciones para la API REST. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(com.colegio.shuji.evaluacion.domain.exception.ImportacionCalificacionesException.class)
  public ResponseEntity<ErrorResponse> handleImportacionCalificaciones(
      com.colegio.shuji.evaluacion.domain.exception.ImportacionCalificacionesException ex, HttpServletRequest request) {
    return ResponseEntity.unprocessableEntity().body(ErrorResponse.ofValidation(422, ex.getMessage(),
        request.getRequestURI(), ex.errores()));
  }

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
    return ResponseEntity.status(ex.getStatusCode())
        .contentType(MediaType.APPLICATION_JSON)
        .body(errorResponse);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex, HttpServletRequest request) {
    InvalidFormatException invalidFormat = findInvalidFormat(ex);
    String message = "El cuerpo de la solicitud no tiene un formato válido.";
    if (invalidFormat != null && invalidFormat.getTargetType() != null
        && invalidFormat.getTargetType().isEnum()) {
      String field = invalidFormat.getPath().stream()
          .map(reference -> reference.getFieldName() != null
              ? reference.getFieldName()
              : reference.getIndex() >= 0 ? "[" + reference.getIndex() + "]" : "")
          .filter(part -> !part.isBlank())
          .collect(Collectors.joining("."))
          .replace(".[", "[");
      if (field.isBlank()) field = "campo indicado";
      String accepted = Arrays.stream(invalidFormat.getTargetType().getEnumConstants())
          .map(value -> ((Enum<?>) value).name())
          .collect(Collectors.joining(", "));
      message = "El campo '" + field + "' debe tener uno de estos valores: " + accepted + ".";
    }
    log.warn("Cuerpo de solicitud inválido en {}", request.getRequestURI());
    return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST_BODY", message, request);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
      MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
    String message = "El parámetro '" + ex.getName() + "' tiene un formato inválido.";
    log.warn("Tipo de parámetro inválido en {}", request.getRequestURI());
    return error(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER_TYPE", message, request);
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(
      MissingServletRequestParameterException ex, HttpServletRequest request) {
    String message = "Falta el parámetro requerido '" + ex.getParameterName() + "'.";
    log.warn("Falta parámetro requerido en {}", request.getRequestURI());
    return error(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER", message, request);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupported(
      HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
    log.warn("Método HTTP no permitido en {}", request.getRequestURI());
    return error(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
        "El método HTTP no está permitido para esta ruta.", request);
  }

  @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
  public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotAcceptable(
      HttpMediaTypeNotAcceptableException ex, HttpServletRequest request) {
    log.warn("Formato de respuesta no disponible en {}", request.getRequestURI());
    return error(HttpStatus.NOT_ACCEPTABLE, "NOT_ACCEPTABLE",
        "El formato de respuesta solicitado no está disponible.", request);
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupported(
      HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
    log.warn("Tipo de contenido no compatible en {}", request.getRequestURI());
    return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
        "El tipo de contenido enviado no es compatible.", request);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ErrorResponse> handleNoResourceFound(
      NoResourceFoundException ex, HttpServletRequest request) {
    log.warn("Recurso HTTP no encontrado en {}", request.getRequestURI());
    return error(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND",
        "No se encontró la ruta solicitada.", request);
  }

  private InvalidFormatException findInvalidFormat(Throwable throwable) {
    for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
      if (cause instanceof InvalidFormatException invalidFormat) return invalidFormat;
    }
    return null;
  }

  private ResponseEntity<ErrorResponse> error(
      HttpStatus status, String errorCode, String message, HttpServletRequest request) {
    ErrorResponse errorResponse =
        ErrorResponse.of(status.value(), errorCode, message, request.getRequestURI());
    return ResponseEntity.status(status)
        .contentType(MediaType.APPLICATION_JSON)
        .body(errorResponse);
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
