package com.colegio.shuji.convivencia.domain.model;

import com.colegio.shuji.convivencia.domain.enums.EstadoIncidencia;
import com.colegio.shuji.convivencia.domain.enums.TipoFalta;
import com.colegio.shuji.shared.domain.exception.BusinessException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
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
  private UUID uuid;
  private Long matriculaId;
  private LocalDate fechaIncidencia;
  private TipoFalta tipoFalta;
  private String descripcion;
  private Long reportadoPorUsuarioId;
  private Boolean requiereCitacion;
  private EstadoIncidencia estado;
  private OffsetDateTime createdAt;

  public void cambiarEstado(EstadoIncidencia nuevoEstado) {
    if (estado == EstadoIncidencia.CERRADA) {
      throw new BusinessException("No se puede cambiar el estado de una incidencia cerrada");
    }
    if (estado == EstadoIncidencia.ABIERTA && nuevoEstado == EstadoIncidencia.CERRADA) {
      throw new BusinessException(
          "Una incidencia abierta debe pasar por atención antes de cerrarse");
    }
    this.estado = nuevoEstado;
  }

  public void atender() {
    cambiarEstado(EstadoIncidencia.ATENDIDA);
  }

  public void cerrar() {
    cambiarEstado(EstadoIncidencia.CERRADA);
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
