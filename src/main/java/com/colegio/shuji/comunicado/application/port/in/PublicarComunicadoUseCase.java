package com.colegio.shuji.comunicado.application.port.in;

import com.colegio.shuji.comunicado.application.dto.in.*;
import com.colegio.shuji.comunicado.application.dto.out.*;

public interface PublicarComunicadoUseCase {
  ComunicadoResponseDto publicar(EmitirComunicadoRequestDto r);

  MetricasLecturaResponseDto metricas(Long id);
}
