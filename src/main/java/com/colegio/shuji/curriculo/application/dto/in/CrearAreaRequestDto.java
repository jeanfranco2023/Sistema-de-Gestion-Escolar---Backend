package com.colegio.shuji.curriculo.application.dto.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearAreaRequestDto(
    @NotNull Short nivelId,
    @NotBlank @Size(max = 20) String codigo,
    @NotBlank @Size(max = 100) String nombre) {}
