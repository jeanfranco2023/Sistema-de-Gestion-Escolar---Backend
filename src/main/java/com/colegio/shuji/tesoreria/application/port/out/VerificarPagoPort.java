package com.colegio.shuji.tesoreria.application.port.out;

import com.colegio.shuji.tesoreria.domain.enums.*;
import java.math.BigDecimal;

public interface VerificarPagoPort {
  record PagoVerificado(
      String transaccionId,
      Long obligacionPagoId,
      BigDecimal monto,
      String moneda,
      MetodoPago metodo,
      boolean aprobado) {}

  PagoVerificado verificar(PasarelaProveedor proveedor, String transaccionId, String credencial);
}
