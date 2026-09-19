package com.colegio.shuji.usuario.application.dto.out;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** DTO de salida con información pública del Rol. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolResponseDto {

  private Short id;
  private String codigo;
  private String nombre;
  private String descripcion;
}
