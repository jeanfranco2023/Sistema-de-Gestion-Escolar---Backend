package com.colegio.shuji.evaluacion.application.port.in;

import com.colegio.shuji.evaluacion.application.dto.out.*;

public interface ConsultarLibretaNotasUseCase {
  LibretaNotasResponseDto libreta(Long matriculaId);

  EstudiantesEnRiesgoResponseDto riesgo(Short periodoId);
}
