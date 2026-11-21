package com.colegio.shuji.usuario.application.dto.in;

import com.colegio.shuji.usuario.domain.enums.EstadoSolicitudCambio;
import com.colegio.shuji.usuario.domain.enums.PrioridadSolicitudCambio;
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
public class GestionarSolicitudCambioRequestDto {

  private EstadoSolicitudCambio nuevoEstado;
  private PrioridadSolicitudCambio prioridad;

  @Size(max = 4000)
  private String impactoEvaluado;

  private Long responsableId;

  @Size(max = 180)
  private String responsableNombre;

  @Size(max = 180)
  private String aprobadorNombre;

  @Size(max = 4000)
  private String motivoDecision;

  @Size(max = 4000)
  private String resumenCambio;

  @Size(max = 500)
  private String enlaceEvidenciaTecnica;

  @Size(max = 4000)
  private String planReversion;

  @Size(max = 4000)
  private String pruebaPosterior;

  @Size(max = 120)
  private String resultadoPrueba;

  @Size(max = 4000)
  private String verificacionCierre;

  @Size(max = 2000)
  private String comentario;

  private Boolean visibleDireccion;

  @Size(max = 500)
  private String evidenciaUrl;
}
