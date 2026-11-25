package com.colegio.shuji.curriculo.application.port.in;

import com.colegio.shuji.curriculo.application.dto.in.AsignarDocenteRequestDto;
import com.colegio.shuji.curriculo.application.dto.out.AsignacionDocenteResponseDto;
import com.colegio.shuji.curriculo.application.dto.out.DocenteOpcionResponseDto;
import java.util.List;

public interface AsignarCargaDocenteUseCase {
  AsignacionDocenteResponseDto asignarDocente(AsignarDocenteRequestDto r);

  AsignacionDocenteResponseDto reasignarDocente(Long asignacionId, Long nuevoDocenteUsuarioId);

  List<AsignacionDocenteResponseDto> listarAsignaciones();

  default List<AsignacionDocenteResponseDto> listarAsignaciones(Short anioId) {
    return listarAsignaciones();
  }

  List<DocenteOpcionResponseDto> listarDocentesPorArea(Short areaId);
}
