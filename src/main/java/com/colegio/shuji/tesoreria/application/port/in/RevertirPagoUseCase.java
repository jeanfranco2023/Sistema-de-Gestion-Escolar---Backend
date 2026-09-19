package com.colegio.shuji.tesoreria.application.port.in;

import com.colegio.shuji.tesoreria.application.dto.in.*;
import com.colegio.shuji.tesoreria.application.dto.out.*;

public interface RevertirPagoUseCase {
  TransaccionResponseDto revertir(RevertirPagoRequestDto r);
}
