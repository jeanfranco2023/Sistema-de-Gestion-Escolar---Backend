package com.colegio.shuji.academico.application.dto.in;

import jakarta.validation.constraints.*;

public record CrearGradoRequestDto(
    @NotNull Short nivelId,
    @NotNull @Min(1) @Max(6) Short numeroGrado,
    @NotBlank @Size(max = 50) String nombre) {}
