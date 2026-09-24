package com.colegio.shuji.curriculo.application.port.in;

import com.colegio.shuji.curriculo.application.dto.in.AsignarDocenteRequestDto;
import com.colegio.shuji.curriculo.application.dto.out.AsignacionDocenteResponseDto;
import java.util.List;

public interface AsignarCargaDocenteUseCase {
  AsignacionDocenteResponseDto asignarDocente(AsignarDocenteRequestDto r);
  List<AsignacionDocenteResponseDto> listarAsignaciones();
}
