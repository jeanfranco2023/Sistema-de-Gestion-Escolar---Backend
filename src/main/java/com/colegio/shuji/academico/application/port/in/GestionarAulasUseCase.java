package com.colegio.shuji.academico.application.port.in;

import com.colegio.shuji.academico.application.dto.in.ActualizarAulaRequestDto;
import com.colegio.shuji.academico.application.dto.in.CambiarEstadoAulaRequestDto;
import com.colegio.shuji.academico.application.dto.in.CrearAulaRequestDto;
import com.colegio.shuji.academico.application.dto.out.AulaResponseDto;
import java.util.List;

public interface GestionarAulasUseCase {
  AulaResponseDto crearAula(CrearAulaRequestDto request);

  List<AulaResponseDto> listarAulas();

  AulaResponseDto actualizarAula(Integer id, ActualizarAulaRequestDto request);

  AulaResponseDto cambiarEstadoAula(Integer id, CambiarEstadoAulaRequestDto request);
}
