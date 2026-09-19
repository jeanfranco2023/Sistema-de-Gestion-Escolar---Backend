package com.colegio.shuji.matricula.application.dto.in;

import com.colegio.shuji.matricula.domain.enums.Parentesco;
import jakarta.validation.constraints.NotNull;

public record VincularApoderadoRequestDto(
    @NotNull Long estudianteId,
    @NotNull Long apoderadoId,
    @NotNull Parentesco parentesco,
    @NotNull Boolean esResponsableEconomico,
    @NotNull Boolean tieneCustodia,
    @NotNull Boolean permiteRecojo) {}
