package com.colegio.shuji.tesoreria.application.port.in;

import com.colegio.shuji.tesoreria.application.port.out.VerificarPagoPort.PagoVerificado;
import java.math.BigDecimal;

public interface RegistrarCobroMatriculaUseCase {
  String registrar(Long matriculaId, BigDecimal montoEsperado, PagoVerificado pago);
}
