package com.colegio.shuji.academico.application.port.in;

import com.colegio.shuji.academico.application.dto.out.VacantesSeccionResponseDto;

public interface ConsultarVacantesUseCase {
  VacantesSeccionResponseDto vacantes(Integer id);
}
