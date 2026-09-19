package com.colegio.shuji.comunicado.application.dto.out;

import java.time.OffsetDateTime;

public record ComunicadoResponseDto(
    Long id,
    String titulo,
    String contenido,
    Long remitenteUsuarioId,
    OffsetDateTime fechaPublicacion,
    Boolean requiereAcuse) {}
