package com.colegio.shuji.matricula.application.dto.out;

import com.colegio.shuji.matricula.domain.enums.*;

public record EstudianteApoderadoResponseDto(
    Long id,
    Long estudianteId,
    Long apoderadoId,
    Parentesco parentesco,
    Boolean esResponsableEconomico,
    Boolean tieneCustodia,
    Boolean permiteRecojo) {}
