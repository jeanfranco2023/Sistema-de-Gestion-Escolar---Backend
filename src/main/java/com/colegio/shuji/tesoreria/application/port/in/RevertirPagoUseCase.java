package com.colegio.shuji.tesoreria.application.port.in;

import com.colegio.shuji.tesoreria.application.dto.in.RevertirPagoRequestDto;
import com.colegio.shuji.tesoreria.application.dto.out.TransaccionResponseDto;

public interface RevertirPagoUseCase {
  TransaccionResponseDto revertir(RevertirPagoRequestDto r);
}
