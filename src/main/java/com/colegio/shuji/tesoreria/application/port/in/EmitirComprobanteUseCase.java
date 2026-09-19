package com.colegio.shuji.tesoreria.application.port.in;

import com.colegio.shuji.tesoreria.application.dto.in.EmitirComprobanteRequestDto;
import com.colegio.shuji.tesoreria.application.dto.out.ComprobanteResponseDto;
import com.colegio.shuji.tesoreria.domain.enums.TipoComprobante;

public interface EmitirComprobanteUseCase {
  ComprobanteResponseDto emitir(EmitirComprobanteRequestDto r);

  ComprobanteResponseDto consultar(Long id);

  ComprobanteResponseDto emitirAutomaticoParaPago(Long pagoId, TipoComprobante tipo, String serie);
}
