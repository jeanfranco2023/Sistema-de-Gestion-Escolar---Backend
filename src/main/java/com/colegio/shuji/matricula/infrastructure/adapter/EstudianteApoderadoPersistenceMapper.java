package com.colegio.shuji.matricula.infrastructure.adapter;

import com.colegio.shuji.matricula.domain.model.EstudianteApoderado;
import com.colegio.shuji.matricula.infrastructure.entity.EstudianteApoderadoEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EstudianteApoderadoPersistenceMapper {
  EstudianteApoderado toDomain(EstudianteApoderadoEntity entity);

  EstudianteApoderadoEntity toEntity(EstudianteApoderado model);

  void actualizar(
      EstudianteApoderado model, @org.mapstruct.MappingTarget EstudianteApoderadoEntity entity);
}
