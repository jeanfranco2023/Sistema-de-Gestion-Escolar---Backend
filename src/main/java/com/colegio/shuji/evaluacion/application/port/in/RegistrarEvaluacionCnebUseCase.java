package com.colegio.shuji.evaluacion.application.port.in;

import com.colegio.shuji.evaluacion.application.dto.in.RegistrarCalificacionesMasivasRequestDto;
import com.colegio.shuji.evaluacion.application.dto.out.CalificacionResponseDto;
import java.util.List;

public interface RegistrarEvaluacionCnebUseCase {
  List<CalificacionResponseDto> registrar(RegistrarCalificacionesMasivasRequestDto r);
}
