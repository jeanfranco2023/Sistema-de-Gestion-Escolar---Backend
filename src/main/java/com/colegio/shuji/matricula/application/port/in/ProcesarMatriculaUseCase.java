package com.colegio.shuji.matricula.application.port.in;

import com.colegio.shuji.matricula.application.dto.in.*;
import com.colegio.shuji.matricula.application.dto.out.*;

public interface ProcesarMatriculaUseCase {
  MatriculaResponseDto solicitar(SolicitarMatriculaRequestDto r);

  MatriculaResponseDto confirmar(ConfirmarMatriculaRequestDto r);

  FichaMatriculaResponseDto ficha(Long id);
}
