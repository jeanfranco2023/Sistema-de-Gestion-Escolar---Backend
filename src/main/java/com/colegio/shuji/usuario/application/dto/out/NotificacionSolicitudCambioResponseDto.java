package com.colegio.shuji.usuario.application.dto.out;

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
public class NotificacionSolicitudCambioResponseDto {

  private Long id;
  private Long solicitudId;
  private Long eventoId;
  private String destinatarioEmail;
  private String asunto;
  private String cuerpoResumen;
  private String estadoEnvio;
  private Integer intentos;
  private String ultimoError;
  private String claveIdempotencia;
  private OffsetDateTime fechaIntento;
}
