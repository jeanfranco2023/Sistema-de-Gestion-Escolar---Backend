package com.colegio.shuji.academico.application.dto.in;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearGradoRequestDto(
    @NotNull Short nivelId,
    @NotNull @Min(1) @Max(6) Short numeroGrado,
    @NotBlank @Size(max = 50) String nombre) {}
