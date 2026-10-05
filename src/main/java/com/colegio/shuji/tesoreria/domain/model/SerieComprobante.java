package com.colegio.shuji.tesoreria.domain.model;

import com.colegio.shuji.shared.domain.exception.BusinessException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SerieComprobante {
  private String serie;
  private Integer ultimoCorrelativo;
  private OffsetDateTime fechaActualizacion;

  public int siguienteCorrelativo() {
    if (ultimoCorrelativo == null || ultimoCorrelativo < 0) {
      throw new BusinessException("Contador de comprobante inválido");
    }
    ultimoCorrelativo++;
    fechaActualizacion = OffsetDateTime.now(ZoneOffset.UTC);
    return ultimoCorrelativo;
  }
}
