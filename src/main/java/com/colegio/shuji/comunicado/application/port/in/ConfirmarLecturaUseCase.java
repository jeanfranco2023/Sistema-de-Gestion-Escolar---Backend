package com.colegio.shuji.comunicado.application.port.in;

import com.colegio.shuji.comunicado.application.dto.in.*;
import com.colegio.shuji.comunicado.application.dto.out.*;

public interface ConfirmarLecturaUseCase {
  ComunicadoDestinatarioResponseDto confirmar(Long comunicadoId, ConfirmarAcuseReciboRequestDto r);
}
