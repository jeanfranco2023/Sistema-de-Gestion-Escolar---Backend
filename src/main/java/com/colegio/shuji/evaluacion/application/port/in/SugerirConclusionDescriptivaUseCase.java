package com.colegio.shuji.evaluacion.application.port.in;

import com.colegio.shuji.evaluacion.application.dto.in.SugerirConclusionDescriptivaRequestDto;
import com.colegio.shuji.evaluacion.application.dto.out.SugerenciaConclusionDescriptivaResponseDto;

public interface SugerirConclusionDescriptivaUseCase {
  SugerenciaConclusionDescriptivaResponseDto sugerir(SugerirConclusionDescriptivaRequestDto request);
}
