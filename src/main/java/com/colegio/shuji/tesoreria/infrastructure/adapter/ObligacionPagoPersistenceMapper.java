package com.colegio.shuji.tesoreria.infrastructure.adapter;

import com.colegio.shuji.tesoreria.domain.model.ObligacionPago;
import com.colegio.shuji.tesoreria.infrastructure.entity.ObligacionPagoEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ObligacionPagoPersistenceMapper {
  ObligacionPago toDomain(ObligacionPagoEntity entity);

  ObligacionPagoEntity toEntity(ObligacionPago model);

  void actualizar(ObligacionPago model, @org.mapstruct.MappingTarget ObligacionPagoEntity entity);
}
