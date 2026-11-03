package com.colegio.shuji.usuario.application.port.in;

import com.colegio.shuji.usuario.application.dto.out.CambioAuditoriaResponseDto;
import java.util.List;

public interface ConsultarAuditoriaCambiosUseCase {
  List<CambioAuditoriaResponseDto> listarRecientes(int limite);
}
