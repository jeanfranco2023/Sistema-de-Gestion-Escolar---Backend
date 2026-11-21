package com.colegio.shuji.usuario.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "solicitudes_cambio_notificaciones")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionSolicitudCambioEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "solicitud_id", nullable = false)
  private Long solicitudId;

  @Column(name = "evento_id")
  private Long eventoId;

  @Column(name = "destinatario_email", nullable = false, length = 180)
  private String destinatarioEmail;

  @Column(name = "asunto", nullable = false, length = 255)
  private String asunto;

  @Column(name = "cuerpo_resumen", columnDefinition = "TEXT")
  private String cuerpoResumen;

  @Column(name = "estado_envio", nullable = false, length = 30)
  private String estadoEnvio;

  @Column(name = "intentos", nullable = false)
  private Integer intentos;

  @Column(name = "ultimo_error", columnDefinition = "TEXT")
  private String ultimoError;

  @Column(name = "clave_idempotencia", nullable = false, unique = true, length = 160)
  private String claveIdempotencia;

  @Column(name = "fecha_intento", nullable = false)
  private OffsetDateTime fechaIntento;
}
