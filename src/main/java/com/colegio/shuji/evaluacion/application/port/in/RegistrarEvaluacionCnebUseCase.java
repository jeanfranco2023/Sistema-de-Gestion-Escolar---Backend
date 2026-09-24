package com.colegio.shuji.evaluacion.application.port.in;

import com.colegio.shuji.evaluacion.application.dto.in.RegistrarCalificacionesMasivasRequestDto;
import com.colegio.shuji.evaluacion.application.dto.out.CalificacionResponseDto;
import com.colegio.shuji.evaluacion.application.dto.out.FilaCalificacionResponseDto;
import java.util.List;

public interface RegistrarEvaluacionCnebUseCase {
  List<CalificacionResponseDto> registrar(RegistrarCalificacionesMasivasRequestDto r);
  List<CalificacionResponseDto> listar(Long asignacionDocenteId, Short periodoAcademicoId);
  List<FilaCalificacionResponseDto> listarMatriz(Long asignacionDocenteId, Short periodoAcademicoId, Short competenciaId);
}
