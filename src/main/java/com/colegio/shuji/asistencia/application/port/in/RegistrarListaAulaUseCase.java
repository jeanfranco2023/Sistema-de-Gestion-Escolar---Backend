package com.colegio.shuji.asistencia.application.port.in;

import com.colegio.shuji.asistencia.application.dto.in.JustificarInasistenciaRequestDto;
import com.colegio.shuji.asistencia.application.dto.in.RegistrarAsistenciaAulaRequestDto;
import com.colegio.shuji.asistencia.application.dto.out.AsistenciaAulaResponseDto;
import com.colegio.shuji.asistencia.application.dto.out.ReporteAsistenciaDiariaResponseDto;
import java.time.LocalDate;

public interface RegistrarListaAulaUseCase {
  AsistenciaAulaResponseDto registrar(RegistrarAsistenciaAulaRequestDto r);

  AsistenciaAulaResponseDto justificar(JustificarInasistenciaRequestDto r);

  ReporteAsistenciaDiariaResponseDto reporte(LocalDate fecha);
}
