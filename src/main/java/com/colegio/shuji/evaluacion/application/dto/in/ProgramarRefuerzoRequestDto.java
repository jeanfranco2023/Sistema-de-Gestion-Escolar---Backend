package com.colegio.shuji.evaluacion.application.dto.in;

import jakarta.validation.constraints.*;
import java.time.*;

public record ProgramarRefuerzoRequestDto(
    @NotNull Short anioLectivoId,
    @NotNull Short periodoAcademicoId,
    @NotNull Short areaCurricularId,
    @NotNull Long docenteUsuarioId,
    @NotBlank @Size(max = 150) String tema,
    @NotNull @FutureOrPresent LocalDate fechaProgramada,
    @NotNull LocalTime horaInicio,
    @NotNull LocalTime horaFin,
    @Size(max = 30) String aulaAsignada) {}
