package com.colegio.shuji.matricula.domain.enums;

import java.util.List;

public enum TipoDocumentoSolicitud {
  PARTIDA_NACIMIENTO,
  DNI_C4,
  RECIBO_SERVICIO,
  CERTIFICADO_ESTUDIOS,
  CONSTANCIA_PAGO;

  public static final List<TipoDocumentoSolicitud> REQUERIDOS =
      List.of(PARTIDA_NACIMIENTO, DNI_C4, RECIBO_SERVICIO);

  public static final List<TipoDocumentoSolicitud> REQUERIDOS_TRASLADO =
      List.of(PARTIDA_NACIMIENTO, DNI_C4, RECIBO_SERVICIO, CERTIFICADO_ESTUDIOS);

  public static List<TipoDocumentoSolicitud> requeridosPara(ModalidadIngreso modalidad) {
    return modalidad == ModalidadIngreso.TRASLADO ? REQUERIDOS_TRASLADO : REQUERIDOS;
  }
}
