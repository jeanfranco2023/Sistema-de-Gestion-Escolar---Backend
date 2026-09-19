package com.colegio.shuji.tesoreria.infrastructure.adapter;

import com.colegio.shuji.tesoreria.domain.model.ComprobantePago;
import com.colegio.shuji.tesoreria.infrastructure.entity.ComprobanteEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ComprobantePagoPersistenceMapper {
  ComprobantePago toDomain(ComprobanteEntity entity);

  ComprobanteEntity toEntity(ComprobantePago model);

  void actualizar(ComprobantePago model, @org.mapstruct.MappingTarget ComprobanteEntity entity);
}
