package com.colegio.shuji.comunicado.application.port.in;

import com.colegio.shuji.comunicado.application.dto.in.EmitirComunicadoRequestDto;
import com.colegio.shuji.comunicado.application.dto.out.ComunicadoResponseDto;
import com.colegio.shuji.comunicado.application.dto.out.MetricasLecturaResponseDto;

public interface PublicarComunicadoUseCase {
  ComunicadoResponseDto publicar(EmitirComunicadoRequestDto r);

  MetricasLecturaResponseDto metricas(Long id);
}
