package com.colegio.shuji.academico.application.mapper;

import com.colegio.shuji.academico.application.dto.in.CrearAnioLectivoRequestDto;
import com.colegio.shuji.academico.application.dto.in.CrearGradoRequestDto;
import com.colegio.shuji.academico.application.dto.in.CrearPeriodoRequestDto;
import com.colegio.shuji.academico.application.dto.in.CrearSeccionRequestDto;
import com.colegio.shuji.academico.application.dto.out.AnioLectivoResponseDto;
import com.colegio.shuji.academico.application.dto.out.GradoResponseDto;
import com.colegio.shuji.academico.application.dto.out.NivelResponseDto;
import com.colegio.shuji.academico.application.dto.out.PeriodoResponseDto;
import com.colegio.shuji.academico.application.dto.out.SeccionResponseDto;
import com.colegio.shuji.academico.domain.model.AnioLectivo;
import com.colegio.shuji.academico.domain.model.Grado;
import com.colegio.shuji.academico.domain.model.Nivel;
import com.colegio.shuji.academico.domain.model.PeriodoAcademico;
import com.colegio.shuji.academico.domain.model.Seccion;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AcademicoMapper {
  AnioLectivo toDomain(CrearAnioLectivoRequestDto request);

  PeriodoAcademico toDomain(CrearPeriodoRequestDto request);

  Seccion toDomain(CrearSeccionRequestDto request);

  Grado toDomain(CrearGradoRequestDto request);

  AnioLectivoResponseDto toResponse(AnioLectivo model);

  PeriodoResponseDto toResponse(PeriodoAcademico model);

  NivelResponseDto toResponse(Nivel model);

  GradoResponseDto toResponse(Grado model);

  SeccionResponseDto toResponse(Seccion model);
}
