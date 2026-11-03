package com.colegio.shuji.matricula.application.port.in;

import com.colegio.shuji.matricula.application.dto.in.ConfirmarMatriculaRequestDto;
import com.colegio.shuji.matricula.application.dto.in.CambiarEstadoMatriculaRequestDto;
import com.colegio.shuji.matricula.application.dto.in.SolicitarMatriculaRequestDto;
import com.colegio.shuji.matricula.application.dto.out.FichaMatriculaResponseDto;
import com.colegio.shuji.matricula.application.dto.out.MatriculaResponseDto;
import java.util.List;

public interface ProcesarMatriculaUseCase {
  MatriculaResponseDto solicitar(SolicitarMatriculaRequestDto r);

  MatriculaResponseDto confirmar(ConfirmarMatriculaRequestDto r);

  MatriculaResponseDto cambiarEstado(Long matriculaId, CambiarEstadoMatriculaRequestDto r);

  FichaMatriculaResponseDto ficha(Long id);

  List<MatriculaResponseDto> listarPorSeccion(Integer seccionId, int page, int size);

  List<MatriculaResponseDto> listarPorAnio(Short anioLectivoId);

  List<Long> listarEstudianteIdsMatriculados(Short anioLectivoId);
}
