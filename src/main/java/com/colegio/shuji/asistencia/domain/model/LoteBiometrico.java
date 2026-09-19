package com.colegio.shuji.asistencia.domain.model;

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
public class LoteBiometrico {
  private Long id;
  private String nombreArchivo;
  private Integer totalFilas;
  private Integer marcasValidas;
  private Integer marcasErroneas;
  private Long importadoPorUsuarioId;
  private OffsetDateTime createdAt;

  public void registrarResultado(int validas, int erroneas) {
    if (validas < 0
        || erroneas < 0
        || totalFilas == null
        || (long) validas + erroneas != totalFilas)
      throw new com.colegio.shuji.asistencia.domain.exception.LoteCorruptoException(
          "El resultado no coincide con el total del lote");
    this.marcasValidas = validas;
    this.marcasErroneas = erroneas;
  }
}
