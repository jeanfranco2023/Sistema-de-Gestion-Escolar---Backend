package com.colegio.shuji.usuario.application.dto.in;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** DTO de entrada para la activación o desactivación de una cuenta de usuario. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CambiarEstadoRequestDto {

  @NotNull(message = "El estado activo es obligatorio (true para activar, false para desactivar)")
  private Boolean activo;
}
