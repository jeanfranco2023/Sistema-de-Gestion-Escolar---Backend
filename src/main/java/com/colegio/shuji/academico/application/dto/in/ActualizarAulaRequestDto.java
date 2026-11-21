package com.colegio.shuji.academico.application.dto.in;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ActualizarAulaRequestDto(
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    String nombre,
    @Size(max = 150, message = "La ubicación no puede exceder 150 caracteres")
    String ubicacion,
    @NotNull(message = "La capacidad es obligatoria")
    @Min(value = 1, message = "La capacidad mínima es 1")
    @Max(value = 30, message = "La capacidad máxima permitida es 30")
    Short capacidad
) {}
