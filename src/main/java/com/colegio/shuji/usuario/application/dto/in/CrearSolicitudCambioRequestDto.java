package com.colegio.shuji.usuario.application.dto.in;

import com.colegio.shuji.usuario.domain.enums.PrioridadSolicitudCambio;
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
public class CrearSolicitudCambioRequestDto {

  @NotBlank(message = "El título del cambio es obligatorio")
  @Size(min = 5, max = 200, message = "El título debe tener entre 5 y 200 caracteres")
  private String titulo;

  @NotBlank(message = "El módulo o proceso afectado es obligatorio")
  @Size(max = 100, message = "El módulo no puede superar los 100 caracteres")
  private String modulo;

  @NotBlank(message = "El problema o motivo es obligatorio")
  @Size(min = 10, max = 4000, message = "El problema o motivo debe tener entre 10 y 4000 caracteres")
  private String motivoProblema;

  @NotBlank(message = "El resultado esperado es obligatorio")
  @Size(min = 10, max = 4000, message = "El resultado esperado debe tener entre 10 y 4000 caracteres")
  private String resultadoEsperado;

  @Size(max = 4000, message = "El alcance o impacto estimado no puede superar los 4000 caracteres")
  private String alcanceImpacto;

  @Size(max = 2000, message = "La evidencia inicial no puede superar los 2000 caracteres")
  private String evidenciaInicial;

  private PrioridadSolicitudCambio prioridad;

  @Size(max = 120)
  private String idempotencyKey;
}
