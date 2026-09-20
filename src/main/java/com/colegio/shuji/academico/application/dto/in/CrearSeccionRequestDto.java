package com.colegio.shuji.academico.application.dto.in;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CrearSeccionRequestDto(
    @NotNull Short anioLectivoId,
    @NotNull Short gradoId,
    @NotNull Short nivelId,
    @NotBlank @Pattern(regexp = "[A-Z]") String letra,
    @NotNull @Min(1) Short cupoMaximo,
    @NotBlank @Size(max = 30) String aulaFisica) {}
