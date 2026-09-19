package com.colegio.shuji.asistencia.domain.model;

import com.colegio.shuji.asistencia.domain.enums.TipoDiscrepancia;
import java.time.LocalDate;
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
public class ConciliacionAsistencia {
  private Long id;
  private LocalDate fecha;
  private Long estudianteId;
  private Boolean marcoPorteria;
  private Boolean presenteAula;
  private TipoDiscrepancia tipoDiscrepancia;
  private Boolean alertaNotificada;

  public void resolver() {
    this.alertaNotificada = true;
  }

  public boolean estaNotificada() {
    return Boolean.TRUE.equals(this.alertaNotificada);
  }

  public void conciliar(boolean porteria, boolean presente, boolean turnoCerrado) {
    if (!turnoCerrado
        && tipoDiscrepancia != null
        && tipoDiscrepancia != TipoDiscrepancia.PENDIENTE_CIERRE_TURNO) return;
    var nuevoTipo =
        !turnoCerrado
            ? TipoDiscrepancia.PENDIENTE_CIERRE_TURNO
            : porteria && !presente
                ? TipoDiscrepancia.DISCREPANCIA_FUGA
                : !porteria && presente
                    ? TipoDiscrepancia.DISCREPANCIA_OMISION_PORTERIA
                    : TipoDiscrepancia.ASISTENCIA_CONCILIADA;
    if (tipoDiscrepancia != nuevoTipo) alertaNotificada = false;
    marcoPorteria = porteria;
    presenteAula = presente;
    tipoDiscrepancia = nuevoTipo;
  }
}
