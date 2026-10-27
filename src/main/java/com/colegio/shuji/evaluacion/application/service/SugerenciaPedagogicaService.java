package com.colegio.shuji.evaluacion.application.service;

import com.colegio.shuji.evaluacion.application.dto.in.SugerirConclusionDescriptivaRequestDto;
import com.colegio.shuji.evaluacion.application.dto.out.SugerenciaConclusionDescriptivaResponseDto;
import com.colegio.shuji.evaluacion.application.port.in.SugerirConclusionDescriptivaUseCase;
import com.colegio.shuji.evaluacion.application.port.out.GeneradorConclusionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SugerenciaPedagogicaService implements SugerirConclusionDescriptivaUseCase {
  private final GeneradorConclusionPort generador;
  public SugerenciaConclusionDescriptivaResponseDto sugerir(SugerirConclusionDescriptivaRequestDto request) {
    return generador.generar(request);
  }
}
