package com.colegio.shuji.usuario.infrastructure.entity;

import com.colegio.shuji.usuario.domain.enums.EstadoSolicitudCambio;
import com.colegio.shuji.usuario.domain.enums.PrioridadSolicitudCambio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "solicitudes_cambio")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudCambioEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "codigo", nullable = false, unique = true, length = 20)
  private String codigo;

  @Column(name = "titulo", nullable = false, length = 200)
  private String titulo;

  @Column(name = "modulo", nullable = false, length = 100)
  private String modulo;

  @Column(name = "motivo_problema", nullable = false, columnDefinition = "TEXT")
  private String motivoProblema;

  @Column(name = "resultado_esperado", nullable = false, columnDefinition = "TEXT")
  private String resultadoEsperado;

  @Column(name = "alcance_impacto", columnDefinition = "TEXT")
  private String alcanceImpacto;

  @Column(name = "evidencia_inicial", columnDefinition = "TEXT")
  private String evidenciaInicial;

  @Column(name = "solicitante_id", nullable = false)
  private Long solicitanteId;

  @Column(name = "solicitante_nombre", nullable = false, length = 180)
  private String solicitanteNombre;

  @Column(name = "solicitante_email", nullable = false, length = 180)
  private String solicitanteEmail;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado", nullable = false, length = 40)
  private EstadoSolicitudCambio estado;

  @Enumerated(EnumType.STRING)
  @Column(name = "prioridad", nullable = false, length = 30)
  private PrioridadSolicitudCambio prioridad;

  @Column(name = "impacto_evaluado", columnDefinition = "TEXT")
  private String impactoEvaluado;

  @Column(name = "responsable_id")
  private Long responsableId;

  @Column(name = "responsable_nombre", length = 180)
  private String responsableNombre;

  @Column(name = "aprobador_nombre", length = 180)
  private String aprobadorNombre;

  @Column(name = "registrado_decision_por", length = 180)
  private String registradoDecisionPor;

  @Column(name = "fecha_decision")
  private OffsetDateTime fechaDecision;

  @Column(name = "motivo_decision", columnDefinition = "TEXT")
  private String motivoDecision;

  @Column(name = "resumen_cambio", columnDefinition = "TEXT")
  private String resumenCambio;

  @Column(name = "enlace_evidencia_tecnica", length = 500)
  private String enlaceEvidenciaTecnica;

  @Column(name = "plan_reversion", columnDefinition = "TEXT")
  private String planReversion;

  @Column(name = "prueba_posterior", columnDefinition = "TEXT")
  private String pruebaPosterior;

  @Column(name = "resultado_prueba", length = 120)
  private String resultadoPrueba;

  @Column(name = "verificacion_cierre", columnDefinition = "TEXT")
  private String verificacionCierre;

  @Column(name = "cerrado_por_nombre", length = 180)
  private String cerradoPorNombre;

  @Column(name = "idempotency_key", unique = true, length = 120)
  private String idempotencyKey;

  @Column(name = "fecha_solicitud", nullable = false)
  private OffsetDateTime fechaSolicitud;

  @Column(name = "fecha_actualizacion", nullable = false)
  private OffsetDateTime fechaActualizacion;

  @Column(name = "fecha_cierre")
  private OffsetDateTime fechaCierre;
}
