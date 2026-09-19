package com.colegio.shuji.convivencia.application.port.in;

import com.colegio.shuji.convivencia.application.dto.in.*;
import com.colegio.shuji.convivencia.application.dto.out.*;
import java.util.*;

public interface GestionarIncidenciasUseCase {
  IncidenciaResponseDto actualizar(Long id, ActualizarEstadoIncidenciaRequestDto r);

  HistorialConductualEstudianteResponseDto historial(Long matriculaId);

  List<IncidenciaResponseDto> citaciones();
}
