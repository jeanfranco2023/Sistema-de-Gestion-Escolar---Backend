package com.colegio.shuji.convivencia.domain.model;

import com.colegio.shuji.convivencia.domain.enums.EstadoIncidencia;
import com.colegio.shuji.convivencia.domain.enums.TipoFalta;
import com.colegio.shuji.shared.domain.exception.BusinessException;
import java.time.LocalDate;
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
public class IncidenciaConductual {
  private Long id;
  private Long matriculaId;
  private LocalDate fechaIncidencia;
  private TipoFalta tipoFalta;
  private String descripcion;
  private Long reportadoPorUsuarioId;
  private Boolean requiereCitacion;
  private EstadoIncidencia estado;
  private OffsetDateTime fechaCreacion;

  public void cambiarEstado(EstadoIncidencia nuevoEstado) {
    if (estado == EstadoIncidencia.ABIERTA && nuevoEstado == EstadoIncidencia.ATENDIDA) {
      atender();
      return;
    }
    if (estado == EstadoIncidencia.ATENDIDA && nuevoEstado == EstadoIncidencia.CERRADA) {
      cerrar();
      return;
    }
    throw new BusinessException("La incidencia solo puede avanzar de ABIERTA a ATENDIDA y de ATENDIDA a CERRADA");
  }

  public void atender() {
    if (estado != EstadoIncidencia.ABIERTA) {
      throw new BusinessException("Solo una incidencia abierta puede pasar a atención");
    }
    this.estado = EstadoIncidencia.ATENDIDA;
  }

  public void cerrar() {
    if (estado != EstadoIncidencia.ATENDIDA) {
      throw new BusinessException("Solo una incidencia atendida puede cerrarse");
    }
    this.estado = EstadoIncidencia.CERRADA;
  }

  public void actualizarDetalles(String nuevaDescripcion, Boolean requiereCitacion) {
    if (estado == EstadoIncidencia.CERRADA) {
      throw new BusinessException("No se puede editar una incidencia cerrada");
    }
    this.descripcion = nuevaDescripcion;
    this.requiereCitacion = requiereCitacion;
  }

  public void abrir(Long reportanteId) {
    if (id != null || reportanteId == null)
      throw new com.colegio.shuji.shared.domain.exception.BusinessException(
          "No se puede abrir esta incidencia");
    reportadoPorUsuarioId = reportanteId;
    estado = EstadoIncidencia.ABIERTA;
  }
}
