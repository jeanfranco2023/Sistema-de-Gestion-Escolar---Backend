package com.colegio.shuji.evaluacion.application.dto.in;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;

public record RegistrarCalificacionesMasivasRequestDto(
    @NotNull Short periodoAcademicoId,
    @NotNull Long asignacionDocenteId,
    @NotEmpty @Size(max = 500) List<@Valid CalificacionItemRequestDto> calificaciones) {}
