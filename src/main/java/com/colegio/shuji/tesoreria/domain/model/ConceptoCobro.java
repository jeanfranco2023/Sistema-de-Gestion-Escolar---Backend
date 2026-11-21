package com.colegio.shuji.tesoreria.domain.model;

import com.colegio.shuji.tesoreria.domain.enums.TipoConcepto;
import java.math.BigDecimal;
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
public class ConceptoCobro {
  private Short id;
  private String codigo;
  private String nombre;
  private TipoConcepto tipoConcepto;
  private BigDecimal montoSugerido;
  @Builder.Default private boolean activo = true;
  @Builder.Default private short diaVencimiento = 10;

  public void actualizar(String nuevoNombre, BigDecimal nuevoMonto) {
    this.nombre = nuevoNombre;
    this.montoSugerido = nuevoMonto;
  }

  public String getDescripcion() {
    return nombre;
  }
}
