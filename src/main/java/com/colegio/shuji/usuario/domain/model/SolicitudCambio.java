package com.colegio.shuji.usuario.domain.model;

import com.colegio.shuji.usuario.domain.enums.EstadoSolicitudCambio;
import com.colegio.shuji.usuario.domain.enums.PrioridadSolicitudCambio;
import java.time.OffsetDateTime;
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
public class SolicitudCambio {

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
  private String idempotencyKey;
  private OffsetDateTime fechaSolicitud;
  private OffsetDateTime fechaActualizacion;
  private OffsetDateTime fechaCierre;

  public void responderInformacion(String nuevaEvidencia, OffsetDateTime ahora) {
    if (nuevaEvidencia != null && !nuevaEvidencia.isBlank()) {
      this.evidenciaInicial =
          (this.evidenciaInicial == null || this.evidenciaInicial.isBlank())
              ? nuevaEvidencia.trim()
              : this.evidenciaInicial + "\n" + nuevaEvidencia.trim();
    }
    this.estado = EstadoSolicitudCambio.EN_EVALUACION;
    this.fechaActualizacion = ahora;
  }

  public void aplicarGestion(
      EstadoSolicitudCambio nuevoEstado,
      PrioridadSolicitudCambio nuevaPrioridad,
      String impactoEvaluado,
      Long responsableId,
      String responsableNombre,
      String aprobadorNombre,
      String registradoDecisionPor,
      OffsetDateTime fechaDecision,
      String motivoDecision,
      String resumenCambio,
      String enlaceEvidenciaTecnica,
      String planReversion,
      String pruebaPosterior,
      String resultadoPrueba,
      String verificacionCierre,
      String cerradoPorNombre,
      OffsetDateTime fechaCierre,
      OffsetDateTime ahora) {
    if (nuevoEstado != null) {
      this.estado = nuevoEstado;
    }
    if (nuevaPrioridad != null) {
      this.prioridad = nuevaPrioridad;
    }
    if (impactoEvaluado != null && !impactoEvaluado.isBlank()) {
      this.impactoEvaluado = impactoEvaluado.trim();
    }
    if (responsableId != null) {
      this.responsableId = responsableId;
    }
    if (responsableNombre != null && !responsableNombre.isBlank()) {
      this.responsableNombre = responsableNombre.trim();
    }
    if (aprobadorNombre != null && !aprobadorNombre.isBlank()) {
      this.aprobadorNombre = aprobadorNombre.trim();
    }
    if (registradoDecisionPor != null && !registradoDecisionPor.isBlank()) {
      this.registradoDecisionPor = registradoDecisionPor.trim();
    }
    if (fechaDecision != null) {
      this.fechaDecision = fechaDecision;
    }
    if (motivoDecision != null && !motivoDecision.isBlank()) {
      this.motivoDecision = motivoDecision.trim();
    }
    if (resumenCambio != null && !resumenCambio.isBlank()) {
      this.resumenCambio = resumenCambio.trim();
    }
    if (enlaceEvidenciaTecnica != null && !enlaceEvidenciaTecnica.isBlank()) {
      this.enlaceEvidenciaTecnica = enlaceEvidenciaTecnica.trim();
    }
    if (planReversion != null && !planReversion.isBlank()) {
      this.planReversion = planReversion.trim();
    }
    if (pruebaPosterior != null && !pruebaPosterior.isBlank()) {
      this.pruebaPosterior = pruebaPosterior.trim();
    }
    if (resultadoPrueba != null && !resultadoPrueba.isBlank()) {
      this.resultadoPrueba = resultadoPrueba.trim();
    }
    if (verificacionCierre != null && !verificacionCierre.isBlank()) {
      this.verificacionCierre = verificacionCierre.trim();
    }
    if (cerradoPorNombre != null && !cerradoPorNombre.isBlank()) {
      this.cerradoPorNombre = cerradoPorNombre.trim();
    }
    if (fechaCierre != null) {
      this.fechaCierre = fechaCierre;
    }
    this.fechaActualizacion = ahora;
  }
}
