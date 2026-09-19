package com.colegio.shuji.academico.infrastructure.adapter;

import com.colegio.shuji.academico.domain.model.PeriodoAcademico;
import com.colegio.shuji.academico.infrastructure.entity.PeriodoAcademicoEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PeriodoAcademicoPersistenceMapper {
  PeriodoAcademico toDomain(PeriodoAcademicoEntity entity);

  PeriodoAcademicoEntity toEntity(PeriodoAcademico model);

  void actualizar(
      PeriodoAcademico model, @org.mapstruct.MappingTarget PeriodoAcademicoEntity entity);
}
