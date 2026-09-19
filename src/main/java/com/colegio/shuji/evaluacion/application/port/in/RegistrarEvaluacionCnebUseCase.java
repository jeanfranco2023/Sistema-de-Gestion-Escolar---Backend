package com.colegio.shuji.evaluacion.application.port.in;

import com.colegio.shuji.evaluacion.application.dto.in.*;
import com.colegio.shuji.evaluacion.application.dto.out.*;
import java.util.*;

public interface RegistrarEvaluacionCnebUseCase {
  List<CalificacionResponseDto> registrar(RegistrarCalificacionesMasivasRequestDto r);
}
