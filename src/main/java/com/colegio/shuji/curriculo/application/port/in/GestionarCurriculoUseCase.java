package com.colegio.shuji.curriculo.application.port.in;

import com.colegio.shuji.curriculo.application.dto.in.*;
import com.colegio.shuji.curriculo.application.dto.out.*;
import java.util.*;

public interface GestionarCurriculoUseCase {
  AreaCurricularResponseDto crearArea(CrearAreaRequestDto r);

  List<AreaCurricularResponseDto> listarAreas(Short nivelId);

  CompetenciaResponseDto crearCompetencia(CrearCompetenciaRequestDto r);

  List<CompetenciaResponseDto> listarCompetencias(Short areaId);

  BloqueHorarioResponseDto crearBloque(CrearBloqueRequestDto r);

  List<BloqueHorarioResponseDto> listarBloques();
}
