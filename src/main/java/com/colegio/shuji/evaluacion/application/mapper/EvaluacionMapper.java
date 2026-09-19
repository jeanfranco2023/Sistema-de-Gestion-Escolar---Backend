package com.colegio.shuji.evaluacion.application.mapper;

import com.colegio.shuji.evaluacion.application.dto.in.ProgramarRefuerzoRequestDto;
import com.colegio.shuji.evaluacion.application.dto.out.CalificacionResponseDto;
import com.colegio.shuji.evaluacion.application.dto.out.InscripcionRefuerzoResponseDto;
import com.colegio.shuji.evaluacion.application.dto.out.SesionRefuerzoResponseDto;
import com.colegio.shuji.evaluacion.domain.model.CalificacionCneb;
import com.colegio.shuji.evaluacion.domain.model.InscripcionRefuerzo;
import com.colegio.shuji.evaluacion.domain.model.SesionRefuerzo;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EvaluacionMapper {
  SesionRefuerzo toDomain(ProgramarRefuerzoRequestDto request);

  CalificacionResponseDto toResponse(CalificacionCneb model);

  SesionRefuerzoResponseDto toResponse(SesionRefuerzo model);

  InscripcionRefuerzoResponseDto toResponse(InscripcionRefuerzo model);
}
