package com.colegio.shuji.curriculo.application.port.in;

import com.colegio.shuji.curriculo.application.dto.in.*;
import com.colegio.shuji.curriculo.application.dto.out.*;

public interface GenerarMallaHorariaUseCase {
  HorarioSeccionResponseDto programar(ProgramarHorarioRequestDto r);

  MallaHorariaResponseDto porSeccion(Integer id, Short anioId);

  MallaHorariaResponseDto porDocente(Long id, Short anioId);
}
