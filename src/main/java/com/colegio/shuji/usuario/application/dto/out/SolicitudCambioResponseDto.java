package com.colegio.shuji.usuario.application.dto.out;

import com.colegio.shuji.usuario.domain.enums.EstadoSolicitudCambio;
import com.colegio.shuji.usuario.domain.enums.PrioridadSolicitudCambio;
import java.time.OffsetDateTime;
import java.util.List;
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
public class SolicitudCambioResponseDto {

  private Long id;
  private String codigo;
  private String titulo;
  private String modulo;
  private String motivoProblema;
  private String resultadoEsperado;
  private String alcanceImpacto;
  private String evidenciaInicial;
  private Long solicitanteId;
  private String solicitanteNombre;
  private String solicitanteEmail;
  private EstadoSolicitudCambio estado;
  private PrioridadSolicitudCambio prioridad;
  private String impactoEvaluado;
  private Long responsableId;
  private String responsableNombre;
  private String aprobadorNombre;
  private String registradoDecisionPor;
  private OffsetDateTime fechaDecision;
  private String motivoDecision;
  private String resumenCambio;
  private String enlaceEvidenciaTecnica;
  private String planReversion;
  private String pruebaPosterior;
  private String resultadoPrueba;
  private String verificacionCierre;
  private String cerradoPorNombre;
  private OffsetDateTime fechaSolicitud;
  private OffsetDateTime fechaActualizacion;
  private OffsetDateTime fechaCierre;
  private List<EventoSolicitudCambioResponseDto> eventos;
  private List<NotificacionSolicitudCambioResponseDto> notificaciones;
}
