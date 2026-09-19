package com.colegio.shuji.comunicado.application.dto.out;

import java.util.*;

public record BandejaApoderadoResponseDto(
    Long apoderadoId, List<ComunicadoResponseDto> comunicados) {}
