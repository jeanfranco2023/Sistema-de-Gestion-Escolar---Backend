package com.colegio.shuji.shared.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** DTO para la estructura estandarizada de errores en la API REST. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

  private boolean success;
  private int status;
  private String errorCode;
  private String message;
  private String path;
  private Map<String, String> validationErrors;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
  @Builder.Default
  private LocalDateTime timestamp = LocalDateTime.now();

  public static ErrorResponse of(int status, String errorCode, String message, String path) {
    return ErrorResponse.builder()
        .success(false)
        .status(status)
        .errorCode(errorCode)
        .message(message)
        .path(path)
        .timestamp(LocalDateTime.now())
        .build();
  }

  public static ErrorResponse ofValidation(
      int status, String message, String path, Map<String, String> validationErrors) {
    return ErrorResponse.builder()
        .success(false)
        .status(status)
        .errorCode("VALIDATION_ERROR")
        .message(message)
        .path(path)
        .validationErrors(validationErrors)
        .timestamp(LocalDateTime.now())
        .build();
  }
}
