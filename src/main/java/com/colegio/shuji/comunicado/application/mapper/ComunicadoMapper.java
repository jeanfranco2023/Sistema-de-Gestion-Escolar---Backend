package com.colegio.shuji.comunicado.application.mapper;

import com.colegio.shuji.comunicado.application.dto.in.EmitirComunicadoRequestDto;
import com.colegio.shuji.comunicado.application.dto.out.ComunicadoDestinatarioResponseDto;
import com.colegio.shuji.comunicado.application.dto.out.ComunicadoResponseDto;
import com.colegio.shuji.comunicado.domain.model.ComunicadoDestinatario;
import com.colegio.shuji.comunicado.domain.model.ComunicadoOficial;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ComunicadoMapper {
  ComunicadoOficial toDomain(EmitirComunicadoRequestDto request);

  ComunicadoResponseDto toResponse(ComunicadoOficial model);

  ComunicadoDestinatarioResponseDto toResponse(ComunicadoDestinatario model);
}
