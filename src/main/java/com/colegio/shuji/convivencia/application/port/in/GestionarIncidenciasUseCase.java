package com.colegio.shuji.convivencia.application.port.in;

import com.colegio.shuji.convivencia.application.dto.in.ActualizarEstadoIncidenciaRequestDto;
import com.colegio.shuji.convivencia.application.dto.out.HistorialConductualEstudianteResponseDto;
import com.colegio.shuji.convivencia.application.dto.out.IncidenciaResponseDto;
import java.util.List;

public interface GestionarIncidenciasUseCase {
  IncidenciaResponseDto actualizar(Long id, ActualizarEstadoIncidenciaRequestDto r);

  HistorialConductualEstudianteResponseDto historial(Long matriculaId);

  List<IncidenciaResponseDto> citaciones();
  List<IncidenciaResponseDto> listar();
}
