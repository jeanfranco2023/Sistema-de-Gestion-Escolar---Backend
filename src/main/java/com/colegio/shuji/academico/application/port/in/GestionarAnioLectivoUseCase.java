package com.colegio.shuji.academico.application.port.in;

import com.colegio.shuji.academico.application.dto.in.CrearAnioLectivoRequestDto;
import com.colegio.shuji.academico.application.dto.in.CrearPeriodoRequestDto;
import com.colegio.shuji.academico.application.dto.out.AnioLectivoResponseDto;
import com.colegio.shuji.academico.application.dto.out.PeriodoResponseDto;
import java.util.List;

public interface GestionarAnioLectivoUseCase {
  AnioLectivoResponseDto crearAnio(CrearAnioLectivoRequestDto r);

  List<AnioLectivoResponseDto> listarAnios();

  void cerrarAnio(Short id);

  PeriodoResponseDto crearPeriodo(CrearPeriodoRequestDto r);

  List<PeriodoResponseDto> listarPeriodos(Short anioId);

  void cerrarPeriodo(Short id);

  void cambiarEstadoPeriodo(Short id, Boolean activo);
}
