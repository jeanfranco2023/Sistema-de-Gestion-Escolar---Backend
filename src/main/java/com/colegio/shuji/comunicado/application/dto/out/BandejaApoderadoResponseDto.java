package com.colegio.shuji.comunicado.application.dto.out;

import java.util.List;

public record BandejaApoderadoResponseDto(
    Long apoderadoId, List<ComunicadoResponseDto> comunicados) {}
