package com.colegio.shuji.comunicado.application.port.in;

import com.colegio.shuji.comunicado.application.dto.in.ConfirmarAcuseReciboRequestDto;
import com.colegio.shuji.comunicado.application.dto.out.ComunicadoDestinatarioResponseDto;

public interface ConfirmarLecturaUseCase {
  ComunicadoDestinatarioResponseDto confirmar(Long comunicadoId, ConfirmarAcuseReciboRequestDto r);
}
