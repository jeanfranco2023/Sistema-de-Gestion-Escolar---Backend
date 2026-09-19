package com.colegio.shuji.tesoreria.application.mapper;

import com.colegio.shuji.tesoreria.application.dto.in.RegistrarPagoCajaRequestDto;
import com.colegio.shuji.tesoreria.application.dto.out.ComprobanteResponseDto;
import com.colegio.shuji.tesoreria.application.dto.out.ConceptoCobroResponseDto;
import com.colegio.shuji.tesoreria.application.dto.out.ObligacionResponseDto;
import com.colegio.shuji.tesoreria.application.dto.out.TransaccionResponseDto;
import com.colegio.shuji.tesoreria.domain.model.ComprobantePago;
import com.colegio.shuji.tesoreria.domain.model.ConceptoCobro;
import com.colegio.shuji.tesoreria.domain.model.ObligacionPago;
import com.colegio.shuji.tesoreria.domain.model.PagoTransaccion;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TesoreriaMapper {
  PagoTransaccion toDomain(RegistrarPagoCajaRequestDto request);

  ConceptoCobroResponseDto toResponse(ConceptoCobro model);

  ObligacionResponseDto toResponse(ObligacionPago model);

  TransaccionResponseDto toResponse(PagoTransaccion model);

  ComprobanteResponseDto toResponse(ComprobantePago model);
}
