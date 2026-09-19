package com.colegio.shuji.asistencia.application.port.in;

import com.colegio.shuji.asistencia.application.dto.out.ConciliacionAsistenciaResponseDto;
import com.colegio.shuji.asistencia.application.dto.out.DiscrepanciaAlertaResponseDto;
import java.time.LocalDate;
import java.util.List;

public interface EjecutarConciliacionDiariaUseCase {
  List<ConciliacionAsistenciaResponseDto> conciliar(
      Short anioId, LocalDate fecha, boolean turnoCerrado);

  List<DiscrepanciaAlertaResponseDto> alertas(LocalDate fecha);
}
