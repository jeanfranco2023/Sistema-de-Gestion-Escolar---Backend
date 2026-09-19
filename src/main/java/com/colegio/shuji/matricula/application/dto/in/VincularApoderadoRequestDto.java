package com.colegio.shuji.matricula.application.dto.in;

import com.colegio.shuji.matricula.domain.enums.*;
import jakarta.validation.constraints.*;

public record VincularApoderadoRequestDto(
    @NotNull Long estudianteId,
    @NotNull Long apoderadoId,
    @NotNull Parentesco parentesco,
    @NotNull Boolean esResponsableEconomico,
    @NotNull Boolean tieneCustodia,
    @NotNull Boolean permiteRecojo) {}
