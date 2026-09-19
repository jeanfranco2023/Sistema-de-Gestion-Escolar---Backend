package com.colegio.shuji.asistencia.application.port.in;

import com.colegio.shuji.asistencia.application.dto.in.*;
import com.colegio.shuji.asistencia.application.dto.out.*;
import java.time.*;

public interface RegistrarListaAulaUseCase {
  AsistenciaAulaResponseDto registrar(RegistrarAsistenciaAulaRequestDto r);

  AsistenciaAulaResponseDto justificar(JustificarInasistenciaRequestDto r);

  ReporteAsistenciaDiariaResponseDto reporte(LocalDate fecha);
}
