package com.colegio.shuji.curriculo.application.port.out;

import com.colegio.shuji.curriculo.application.dto.out.DocenteOpcionResponseDto;
import java.util.List;

public interface DocenteHabilitadoPort {
  List<DocenteOpcionResponseDto> listarPorArea(Short areaId);
}
