package com.colegio.shuji.tesoreria.application.port.out;

import com.colegio.shuji.tesoreria.application.dto.out.PreferenciaMercadoPagoResponseDto;
import com.colegio.shuji.tesoreria.domain.model.ObligacionPago;
import java.math.BigDecimal;

public interface MercadoPagoPort {
  PreferenciaMercadoPagoResponseDto crearPreferencia(
      ObligacionPago obligacion, String descripcion, String backUrlSuccess, String backUrlFailure);

  default PreferenciaMercadoPagoResponseDto crearPreferenciaResumen(
      String referencia, BigDecimal monto, String descripcion, String backUrlSuccess, String backUrlFailure) {
    return crearPreferencia(
        ObligacionPago.builder()
            .id(0L)
            .montoBase(monto)
            .montoMora(BigDecimal.ZERO)
            .montoDescuento(BigDecimal.ZERO)
            .totalPagado(BigDecimal.ZERO)
            .build(),
        descripcion,
        backUrlSuccess,
        backUrlFailure);
  }

  PreferenciaMercadoPagoResponseDto crearPreferenciaMatriculaPublica(
      String referencia, BigDecimal monto, String descripcion);

  VerificarPagoPort.PagoVerificado consultarPago(String paymentId);

  java.util.Optional<VerificarPagoPort.PagoVerificado> buscarPagoAprobado(String referencia);
}
