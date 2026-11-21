package com.colegio.shuji.usuario.domain.model;

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
public class NotificacionSolicitudCambio {

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

  public void registrarResultadoEnvio(boolean enviado, String errorDetalle, OffsetDateTime ahora) {
    this.intentos = (this.intentos == null ? 0 : this.intentos) + 1;
    this.estadoEnvio = enviado ? "ENVIADO" : "FALLIDO";
    this.ultimoError = enviado ? null : errorDetalle;
    this.fechaIntento = ahora;
  }
}
