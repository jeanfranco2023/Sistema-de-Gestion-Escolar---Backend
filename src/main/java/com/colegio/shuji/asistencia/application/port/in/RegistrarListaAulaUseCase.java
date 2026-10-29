package com.colegio.shuji.asistencia.application.port.in;

import com.colegio.shuji.asistencia.application.dto.in.RegistrarAsistenciaAulaRequestDto;
import com.colegio.shuji.asistencia.application.dto.out.AsistenciaAulaResponseDto;
import com.colegio.shuji.asistencia.application.dto.out.ReporteAsistenciaDiariaResponseDto;
import com.colegio.shuji.asistencia.application.dto.out.DocenteMatriculaAsistenciaDto;
import com.colegio.shuji.asistencia.application.dto.out.DocenteAsistenciaDiariaResponseDto;
import java.util.List;
import java.time.LocalDate;

public interface RegistrarListaAulaUseCase {
  AsistenciaAulaResponseDto registrar(RegistrarAsistenciaAulaRequestDto r);

  AsistenciaAulaResponseDto justificar(Long asistenciaId, String motivo, String documentoSustentoKey);

  String documentoSustentoKey(Long asistenciaId);

  ReporteAsistenciaDiariaResponseDto reporte(LocalDate fecha);

  List<DocenteMatriculaAsistenciaDto> matriculasDelDocente();

  List<DocenteAsistenciaDiariaResponseDto> reporteDelDocente(LocalDate fecha);
}
