package com.colegio.shuji.usuario.application.port.in;

import com.colegio.shuji.usuario.application.dto.in.CambiarPasswordInicialRequestDto;

public interface CambiarPasswordInicialUseCase {
  void cambiarPasswordInicial(Long usuarioId, CambiarPasswordInicialRequestDto request);
}
