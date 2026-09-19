package com.colegio.shuji.academico.application.port.in;

import com.colegio.shuji.academico.application.dto.in.*;
import com.colegio.shuji.academico.application.dto.out.*;
import java.util.*;

public interface GestionarAnioLectivoUseCase {
  AnioLectivoResponseDto crearAnio(CrearAnioLectivoRequestDto r);

  List<AnioLectivoResponseDto> listarAnios();

  void cerrarAnio(Short id);

  PeriodoResponseDto crearPeriodo(CrearPeriodoRequestDto r);

  List<PeriodoResponseDto> listarPeriodos(Short anioId);

  void cerrarPeriodo(Short id);
}
