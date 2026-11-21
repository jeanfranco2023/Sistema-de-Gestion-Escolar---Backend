package com.colegio.shuji.usuario.domain.enums;

public enum EstadoSolicitudCambio {
  RECIBIDA,
  EN_EVALUACION,
  REQUIERE_INFORMACION,
  APROBADA,
  RECHAZADA,
  EN_EJECUCION,
  RESUELTA,
  CERRADA;

  public boolean admiteGestionHacia(EstadoSolicitudCambio destino) {
    return switch (this) {
      case RECIBIDA -> destino == this || destino == EN_EVALUACION;
      case EN_EVALUACION ->
          destino == this || destino == REQUIERE_INFORMACION || destino == APROBADA
              || destino == RECHAZADA;
      case REQUIERE_INFORMACION -> destino == this || destino == RECHAZADA;
      case APROBADA -> destino == this || destino == EN_EJECUCION;
      case EN_EJECUCION -> destino == this || destino == RESUELTA;
      case RESUELTA -> destino == this || destino == CERRADA;
      case CERRADA, RECHAZADA -> false;
    };
  }
}
