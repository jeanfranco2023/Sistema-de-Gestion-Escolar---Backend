package com.colegio.shuji.academico.infrastructure.adapter;

import com.colegio.shuji.academico.domain.model.AnioLectivo;
import com.colegio.shuji.academico.infrastructure.entity.AnioLectivoEntity;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AnioLectivoPersistenceMapper {
  AnioLectivo toDomain(AnioLectivoEntity entity);

  AnioLectivoEntity toEntity(AnioLectivo model);

  void actualizar(AnioLectivo model, @org.mapstruct.MappingTarget AnioLectivoEntity entity);
}
