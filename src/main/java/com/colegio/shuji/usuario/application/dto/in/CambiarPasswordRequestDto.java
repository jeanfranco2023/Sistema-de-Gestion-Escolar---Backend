package com.colegio.shuji.usuario.application.dto.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** DTO de entrada para la solicitud de cambio de contraseña de usuario. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CambiarPasswordRequestDto {

  @NotBlank(message = "La contraseña actual es obligatoria")
  private String passwordActual;

  @NotBlank(message = "La nueva contraseña es obligatoria")
  @Size(min = 6, max = 100, message = "La nueva contraseña debe tener entre 6 y 100 caracteres")
  private String passwordNuevo;
}
