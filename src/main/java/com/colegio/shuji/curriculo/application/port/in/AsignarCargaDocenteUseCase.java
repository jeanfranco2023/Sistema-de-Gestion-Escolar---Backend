package com.colegio.shuji.curriculo.application.port.in;

import com.colegio.shuji.curriculo.application.dto.in.AsignarDocenteRequestDto;
import com.colegio.shuji.curriculo.application.dto.out.AsignacionDocenteResponseDto;

public interface AsignarCargaDocenteUseCase {
  AsignacionDocenteResponseDto asignarDocente(AsignarDocenteRequestDto r);
}
