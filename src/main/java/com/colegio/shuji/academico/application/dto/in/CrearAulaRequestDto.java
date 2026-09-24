package com.colegio.shuji.academico.application.dto.in;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearAulaRequestDto(
    @NotBlank @Size(max = 30) String codigo,
    @NotBlank @Size(max = 40) String nombre,
    @Size(max = 150) String ubicacion,
    @NotNull @Min(1) Short capacidad) {}
