package com.colegio.shuji.curriculo.application.port.in;

import com.colegio.shuji.curriculo.application.dto.in.CrearAreaRequestDto;
import com.colegio.shuji.curriculo.application.dto.in.CrearBloqueRequestDto;
import com.colegio.shuji.curriculo.application.dto.in.CrearCompetenciaRequestDto;
import com.colegio.shuji.curriculo.application.dto.out.AreaCurricularResponseDto;
import com.colegio.shuji.curriculo.application.dto.out.BloqueHorarioResponseDto;
import com.colegio.shuji.curriculo.application.dto.out.CompetenciaResponseDto;
import java.util.List;

public interface GestionarCurriculoUseCase {
  AreaCurricularResponseDto crearArea(CrearAreaRequestDto r);

  List<AreaCurricularResponseDto> listarAreas(Short nivelId);

  CompetenciaResponseDto crearCompetencia(CrearCompetenciaRequestDto r);

  List<CompetenciaResponseDto> listarCompetencias(Short areaId);

  BloqueHorarioResponseDto crearBloque(CrearBloqueRequestDto r);

  List<BloqueHorarioResponseDto> listarBloques();
}
