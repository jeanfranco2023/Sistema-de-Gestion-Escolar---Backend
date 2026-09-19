package com.colegio.shuji.academico.application.port.in;

import com.colegio.shuji.academico.application.dto.out.*;

public interface ConsultarVacantesUseCase {
  VacantesSeccionResponseDto vacantes(Integer id);
}
