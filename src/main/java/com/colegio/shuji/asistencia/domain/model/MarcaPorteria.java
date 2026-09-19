package com.colegio.shuji.asistencia.domain.model;

import com.colegio.shuji.asistencia.domain.enums.EstadoMarca;
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
public class MarcaPorteria {
  private Long id;
  private Long loteId;
  private String dniLeido;
  private OffsetDateTime fechaHora;
  private String dispositivoCodigo;
  private Long estudianteId;
  private EstadoMarca estadoProcesamiento;
  private OffsetDateTime createdAt;

  public void procesarValida(Long estudianteId) {
    this.estudianteId = estudianteId;
    this.estadoProcesamiento = EstadoMarca.CONCILIADO;
  }

  public void marcarDniNoIdentificado() {
    this.estadoProcesamiento = EstadoMarca.DNI_NO_IDENTIFICADO;
  }

  public void marcarDuplicado() {
    this.estadoProcesamiento = EstadoMarca.DUPLICADO;
  }

  public void identificar(Long estudianteId) {
    this.estudianteId = estudianteId;
    this.estadoProcesamiento =
        estudianteId == null ? EstadoMarca.DNI_NO_IDENTIFICADO : EstadoMarca.PENDIENTE;
  }

  public void conciliar() {
    if (estadoProcesamiento != EstadoMarca.PENDIENTE
        && estadoProcesamiento != EstadoMarca.CONCILIADO)
      throw new com.colegio.shuji.shared.domain.exception.BusinessException("Marca no conciliable");
    estadoProcesamiento = EstadoMarca.CONCILIADO;
  }
}
