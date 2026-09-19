package com.colegio.shuji.curriculo.application.port.in;

import com.colegio.shuji.curriculo.application.dto.in.*;
import com.colegio.shuji.curriculo.application.dto.out.*;

public interface AsignarCargaDocenteUseCase {
  AsignacionDocenteResponseDto asignarDocente(AsignarDocenteRequestDto r);
}
