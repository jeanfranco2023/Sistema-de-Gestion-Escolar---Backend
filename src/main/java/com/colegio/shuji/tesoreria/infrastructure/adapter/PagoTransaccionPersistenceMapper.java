package com.colegio.shuji.tesoreria.infrastructure.adapter;

import com.colegio.shuji.tesoreria.domain.model.PagoTransaccion;
import com.colegio.shuji.tesoreria.infrastructure.entity.PagoTransaccionEntity;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PagoTransaccionPersistenceMapper {
  PagoTransaccion toDomain(PagoTransaccionEntity entity);

  PagoTransaccionEntity toEntity(PagoTransaccion model);

  void actualizar(PagoTransaccion model, @org.mapstruct.MappingTarget PagoTransaccionEntity entity);
}
