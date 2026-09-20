package com.colegio.shuji.curriculo.application.dto.in;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearCompetenciaRequestDto(
    @NotNull Short areaId,
    @NotNull @Min(1) Short numeroOrden,
    @NotBlank @Size(max = 150) String nombre,
    @Size(max = 4000) String descripcion) {}
