package com.colegio.shuji.asistencia.application.port.in;

import com.colegio.shuji.asistencia.application.dto.out.*;
import java.time.*;
import java.util.*;

public interface EjecutarConciliacionDiariaUseCase {
  List<ConciliacionAsistenciaResponseDto> conciliar(
      Short anioId, LocalDate fecha, boolean turnoCerrado);

  List<DiscrepanciaAlertaResponseDto> alertas(LocalDate fecha);
}
