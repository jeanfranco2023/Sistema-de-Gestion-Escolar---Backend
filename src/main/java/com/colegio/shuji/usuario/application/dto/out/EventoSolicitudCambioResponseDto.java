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
public class EventoSolicitudCambioResponseDto {

  private Long id;
  private Long solicitudId;
  private Long actorId;
  private String actorNombre;
  private String actorRol;
  private String estadoAnterior;
  private String estadoNuevo;
  private String tipoEvento;
  private String comentario;
  private Boolean visibleDireccion;
  private String evidenciaUrl;
  private OffsetDateTime fechaEvento;
}
