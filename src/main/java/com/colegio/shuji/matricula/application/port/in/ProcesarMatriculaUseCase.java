package com.colegio.shuji.matricula.application.port.in;

import com.colegio.shuji.matricula.application.dto.in.ConfirmarMatriculaRequestDto;
import com.colegio.shuji.matricula.application.dto.in.SolicitarMatriculaRequestDto;
import com.colegio.shuji.matricula.application.dto.out.FichaMatriculaResponseDto;
import com.colegio.shuji.matricula.application.dto.out.MatriculaResponseDto;
import java.util.List;

public interface ProcesarMatriculaUseCase {
  MatriculaResponseDto solicitar(SolicitarMatriculaRequestDto r);

  MatriculaResponseDto confirmar(ConfirmarMatriculaRequestDto r);

  FichaMatriculaResponseDto ficha(Long id);

  List<MatriculaResponseDto> listarPorSeccion(Integer seccionId, int page, int size);
}
