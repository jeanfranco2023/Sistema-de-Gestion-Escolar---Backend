package com.colegio.shuji.usuario.application.dto.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponderInfoSolicitudCambioRequestDto {

  @NotBlank(message = "El comentario o respuesta de información es obligatorio")
  @Size(min = 5, max = 2000, message = "El comentario debe tener entre 5 y 2000 caracteres")
  private String comentario;

  @Size(max = 500, message = "El enlace o referencia de evidencia no puede superar los 500 caracteres")
  private String evidenciaUrl;
}
