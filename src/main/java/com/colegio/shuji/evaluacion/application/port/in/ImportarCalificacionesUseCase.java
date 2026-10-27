package com.colegio.shuji.evaluacion.application.port.in;

import com.colegio.shuji.evaluacion.application.dto.out.ImportacionCalificacionesResponseDto;
import java.util.List;

public interface ImportarCalificacionesUseCase {
  ImportacionCalificacionesResponseDto importar(List<List<String>> filas, Short periodoId, Integer seccionId,
      Long asignacionDocenteId, boolean confirmar);
}
