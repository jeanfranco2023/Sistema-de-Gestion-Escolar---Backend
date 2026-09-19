package com.colegio.shuji.shared.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Envoltorio estándar genérico para todas las respuestas de la API REST.
 *
 * @param <T> Tipo del payload de datos.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

  private boolean success;
  private int status;
  private String message;
  private T data;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
  @Builder.Default
  private LocalDateTime timestamp = LocalDateTime.now();

  public static <T> ApiResponse<T> ok(T data) {
    return ApiResponse.<T>builder()
        .success(true)
        .status(200)
        .message("Operación realizada con éxito")
        .data(data)
        .timestamp(LocalDateTime.now())
        .build();
  }

  public static <T> ApiResponse<T> ok(T data, String message) {
    return ApiResponse.<T>builder()
        .success(true)
        .status(200)
        .message(message)
        .data(data)
        .timestamp(LocalDateTime.now())
        .build();
  }

  public static <T> ApiResponse<T> created(T data, String message) {
    return ApiResponse.<T>builder()
        .success(true)
        .status(201)
        .message(message)
        .data(data)
        .timestamp(LocalDateTime.now())
        .build();
  }

  public static <T> ApiResponse<T> error(int status, String message) {
    return ApiResponse.<T>builder()
        .success(false)
        .status(status)
        .message(message)
        .timestamp(LocalDateTime.now())
        .build();
  }
}
