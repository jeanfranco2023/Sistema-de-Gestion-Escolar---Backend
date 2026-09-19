package com.colegio.shuji.asistencia.infrastructure.controller.exception;

import com.colegio.shuji.shared.application.dto.ErrorResponse;
import com.colegio.shuji.shared.domain.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(0)
@RestControllerAdvice(basePackages = "com.colegio.shuji.asistencia.infrastructure.controller.rest")
public class AsistenciaExceptionHandler {
  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ErrorResponse> negocio(BusinessException ex, HttpServletRequest request) {
    return ResponseEntity.badRequest()
        .body(ErrorResponse.of(400, ex.getErrorCode(), ex.getMessage(), request.getRequestURI()));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> conflicto(
      DataIntegrityViolationException ex, HttpServletRequest request) {
    return ResponseEntity.status(409)
        .body(
            ErrorResponse.of(
                409,
                "CONFLICTO_INTEGRIDAD",
                "La operación entra en conflicto con los datos existentes",
                request.getRequestURI()));
  }
}
