package com.colegio.shuji.asistencia.application.mapper;

import com.colegio.shuji.asistencia.application.dto.in.RegistrarAsistenciaAulaRequestDto;
import com.colegio.shuji.asistencia.application.dto.out.AsistenciaAulaResponseDto;
import com.colegio.shuji.asistencia.application.dto.out.ConciliacionAsistenciaResponseDto;
import com.colegio.shuji.asistencia.application.dto.out.LoteBiometricoResponseDto;
import com.colegio.shuji.asistencia.application.dto.out.MarcaPorteriaResponseDto;
import com.colegio.shuji.asistencia.domain.model.AsistenciaAula;
import com.colegio.shuji.asistencia.domain.model.ConciliacionAsistencia;
import com.colegio.shuji.asistencia.domain.model.LoteBiometrico;
import com.colegio.shuji.asistencia.domain.model.MarcaPorteria;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AsistenciaMapper {
  AsistenciaAula toDomain(RegistrarAsistenciaAulaRequestDto request);

  LoteBiometricoResponseDto toResponse(LoteBiometrico model);

  MarcaPorteriaResponseDto toResponse(MarcaPorteria model);

  AsistenciaAulaResponseDto toResponse(AsistenciaAula model);

  ConciliacionAsistenciaResponseDto toResponse(ConciliacionAsistencia model);
}
