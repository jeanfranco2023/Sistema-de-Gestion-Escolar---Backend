package com.colegio.shuji.curriculo.application.mapper;

import com.colegio.shuji.curriculo.application.dto.in.AsignarDocenteRequestDto;
import com.colegio.shuji.curriculo.application.dto.in.CrearAreaRequestDto;
import com.colegio.shuji.curriculo.application.dto.in.CrearBloqueRequestDto;
import com.colegio.shuji.curriculo.application.dto.in.CrearCompetenciaRequestDto;
import com.colegio.shuji.curriculo.application.dto.out.AreaCurricularResponseDto;
import com.colegio.shuji.curriculo.application.dto.out.AsignacionDocenteResponseDto;
import com.colegio.shuji.curriculo.application.dto.out.BloqueHorarioResponseDto;
import com.colegio.shuji.curriculo.application.dto.out.CompetenciaResponseDto;
import com.colegio.shuji.curriculo.application.dto.out.HorarioSeccionResponseDto;
import com.colegio.shuji.curriculo.domain.model.AreaCurricular;
import com.colegio.shuji.curriculo.domain.model.AsignacionDocente;
import com.colegio.shuji.curriculo.domain.model.BloqueHorario;
import com.colegio.shuji.curriculo.domain.model.Competencia;
import com.colegio.shuji.curriculo.domain.model.HorarioSeccion;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CurriculoMapper {
  AreaCurricular toDomain(CrearAreaRequestDto request);

  Competencia toDomain(CrearCompetenciaRequestDto request);

  BloqueHorario toDomain(CrearBloqueRequestDto request);

  AsignacionDocente toDomain(AsignarDocenteRequestDto request);

  AreaCurricularResponseDto toResponse(AreaCurricular model);

  CompetenciaResponseDto toResponse(Competencia model);

  AsignacionDocenteResponseDto toResponse(AsignacionDocente model);

  BloqueHorarioResponseDto toResponse(BloqueHorario model);

  HorarioSeccionResponseDto toResponse(HorarioSeccion model);
}
