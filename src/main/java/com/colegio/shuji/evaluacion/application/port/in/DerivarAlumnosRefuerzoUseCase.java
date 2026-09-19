package com.colegio.shuji.evaluacion.application.port.in;

import com.colegio.shuji.evaluacion.application.dto.in.*;
import com.colegio.shuji.evaluacion.application.dto.out.*;
import java.util.*;

public interface DerivarAlumnosRefuerzoUseCase {
  SesionRefuerzoResponseDto programar(ProgramarRefuerzoRequestDto r);

  List<InscripcionRefuerzoResponseDto> derivar(Long sesionId);

  InscripcionRefuerzoResponseDto registrarAsistencia(RegistrarAsistenciaRefuerzoRequestDto r);

  List<SesionRefuerzoResponseDto> sesiones(Short periodoId);
}
