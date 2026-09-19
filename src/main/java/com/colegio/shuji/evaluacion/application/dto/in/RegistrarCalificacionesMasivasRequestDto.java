package com.colegio.shuji.evaluacion.application.dto.in;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record RegistrarCalificacionesMasivasRequestDto(
    @NotNull Short periodoAcademicoId,
    @NotNull Long asignacionDocenteId,
    @NotEmpty @Size(max = 500) List<@Valid CalificacionItemRequestDto> calificaciones) {}
