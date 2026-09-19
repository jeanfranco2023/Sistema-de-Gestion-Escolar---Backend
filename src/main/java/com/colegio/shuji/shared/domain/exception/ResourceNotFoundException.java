package com.colegio.shuji.shared.domain.exception;

import lombok.Getter;

/** Excepción lanzada cuando un recurso solicitado no existe en el sistema. */
@Getter
public class ResourceNotFoundException extends BusinessException {

  private final String resourceName;
  private final String fieldName;
  private final Object fieldValue;

  public ResourceNotFoundException(String message) {
    super(message, "RESOURCE_NOT_FOUND");
    this.resourceName = null;
    this.fieldName = null;
    this.fieldValue = null;
  }

  public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
    super(
        String.format("%s no encontrado(a) con %s: '%s'", resourceName, fieldName, fieldValue),
        "RESOURCE_NOT_FOUND");
    this.resourceName = resourceName;
    this.fieldName = fieldName;
    this.fieldValue = fieldValue;
  }
}
