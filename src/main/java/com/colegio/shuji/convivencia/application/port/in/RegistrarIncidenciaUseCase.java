package com.colegio.shuji.convivencia.application.port.in;

import com.colegio.shuji.convivencia.application.dto.in.*;
import com.colegio.shuji.convivencia.application.dto.out.*;

public interface RegistrarIncidenciaUseCase {
  IncidenciaResponseDto registrar(RegistrarIncidenciaRequestDto r);
}
