package com.colegio.shuji.matricula.domain.model;

import com.colegio.shuji.matricula.domain.enums.EstadoMatricula;
import com.colegio.shuji.shared.domain.exception.BusinessException;
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
public class Matricula {
  private Long id;
  private UUID uuid;
  private Short anioLectivoId;
  private Long estudianteId;
  private Integer seccionId;
  private OffsetDateTime fechaMatricula;
  private EstadoMatricula estadoMatricula;
  private OffsetDateTime reservaExpiraAt;
  private String observaciones;

  public void reservar(OffsetDateTime vencimiento) {
    if (estadoMatricula != EstadoMatricula.SOLICITADA
        || !vencimiento.isAfter(OffsetDateTime.now())) {
      throw new BusinessException("Reserva inválida");
    }
    this.estadoMatricula = EstadoMatricula.RESERVADA_TEMPORAL;
    this.reservaExpiraAt = vencimiento;
  }

  public void confirmar(OffsetDateTime ahora) {
    if (estadoMatricula == EstadoMatricula.MATRICULADO) return;
    if (estadoMatricula == EstadoMatricula.RESERVADA_TEMPORAL && !reservaExpiraAt.isAfter(ahora)) {
      throw new com.colegio.shuji.matricula.domain.exception.ReservaExpiradaException(
          "La reserva expiró");
    }
    if (estadoMatricula != EstadoMatricula.SOLICITADA
        && estadoMatricula != EstadoMatricula.RESERVADA_TEMPORAL) {
      throw new BusinessException("No se puede confirmar este estado");
    }
    this.estadoMatricula = EstadoMatricula.MATRICULADO;
    this.reservaExpiraAt = null;
  }

  public boolean liberarSiVencida(OffsetDateTime ahora) {
    if (estadoMatricula == EstadoMatricula.RESERVADA_TEMPORAL && !reservaExpiraAt.isAfter(ahora)) {
      this.estadoMatricula = EstadoMatricula.CANCELADA;
      this.reservaExpiraAt = null;
      return true;
    }
    return false;
  }

  public void cancelar(String motivo) {
    this.estadoMatricula = EstadoMatricula.CANCELADA;
    this.observaciones = motivo;
  }

  public void trasladar(String motivo) {
    this.estadoMatricula = EstadoMatricula.TRASLADADO;
    this.observaciones = motivo;
  }

  public void retirar(String motivo) {
    this.estadoMatricula = EstadoMatricula.RETIRADO;
    this.observaciones = motivo;
  }

  public void cambiarSeccion(Integer nuevaSeccionId) {
    this.seccionId = nuevaSeccionId;
  }

  public boolean estaMatriculado() {
    return this.estadoMatricula == EstadoMatricula.MATRICULADO;
  }

  public void iniciarSolicitud() {
    if (id != null || estadoMatricula != null)
      throw new BusinessException("La matrícula ya tiene estado");
    estadoMatricula = EstadoMatricula.SOLICITADA;
    reservaExpiraAt = null;
  }
}
