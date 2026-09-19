package com.colegio.shuji.evaluacion.application.port.in;

import com.colegio.shuji.evaluacion.application.dto.out.EstudiantesEnRiesgoResponseDto;
import com.colegio.shuji.evaluacion.application.dto.out.LibretaNotasResponseDto;

public interface ConsultarLibretaNotasUseCase {
  LibretaNotasResponseDto libreta(Long matriculaId);

  EstudiantesEnRiesgoResponseDto riesgo(Short periodoId);
}
