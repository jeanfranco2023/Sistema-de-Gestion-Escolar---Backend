package com.colegio.shuji.tesoreria.application.port.out;

import com.colegio.shuji.tesoreria.application.dto.out.PreferenciaMercadoPagoResponseDto;
import com.colegio.shuji.tesoreria.domain.model.ObligacionPago;
import java.math.BigDecimal;

public interface MercadoPagoPort {
  PreferenciaMercadoPagoResponseDto crearPreferencia(
      ObligacionPago obligacion, String descripcion, String backUrlSuccess, String backUrlFailure);

  PreferenciaMercadoPagoResponseDto crearPreferenciaMatriculaPublica(
      String referencia, BigDecimal monto, String descripcion);

  VerificarPagoPort.PagoVerificado consultarPago(String paymentId);
}
