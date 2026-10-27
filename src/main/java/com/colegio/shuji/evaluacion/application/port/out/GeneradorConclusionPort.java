package com.colegio.shuji.evaluacion.application.port.out;

import com.colegio.shuji.evaluacion.application.dto.in.SugerirConclusionDescriptivaRequestDto;
import com.colegio.shuji.evaluacion.application.dto.out.SugerenciaConclusionDescriptivaResponseDto;

public interface GeneradorConclusionPort {
  SugerenciaConclusionDescriptivaResponseDto generar(SugerirConclusionDescriptivaRequestDto contexto);
}
