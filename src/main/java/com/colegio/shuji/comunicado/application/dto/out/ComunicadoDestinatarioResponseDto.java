package com.colegio.shuji.comunicado.application.dto.out;

import java.time.*;

public record ComunicadoDestinatarioResponseDto(
    Long id,
    Long comunicadoId,
    Long apoderadoId,
    Boolean leido,
    OffsetDateTime fechaLectura,
    Boolean acuseConfirmado,
    OffsetDateTime fechaAcuse) {}
