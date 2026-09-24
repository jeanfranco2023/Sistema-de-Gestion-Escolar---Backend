package com.colegio.shuji.matricula.application.dto.in;

import com.colegio.shuji.matricula.domain.enums.TipoDocumentoSolicitud;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ObservarSolicitudMatriculaPublicaRequestDto(
    @NotNull TipoDocumentoSolicitud tipo,
    @NotBlank @Size(max = 500) String observacion) {}
