package com.colegio.shuji.usuario.application.port.in;

import com.colegio.shuji.usuario.application.dto.out.CambioAuditoriaResponseDto;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ConsultarAuditoriaCambiosUseCase {
  List<CambioAuditoriaResponseDto> listarRecientes(int limite);

  Page<CambioAuditoriaResponseDto> filtrar(
      Instant fechaInicio,
      Instant fechaFin,
      Long usuarioId,
      String tabla,
      Pageable pageable);
}
