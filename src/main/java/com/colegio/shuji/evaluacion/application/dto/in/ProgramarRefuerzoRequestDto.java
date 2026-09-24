package com.colegio.shuji.evaluacion.application.dto.in;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public record ProgramarRefuerzoRequestDto(
    @NotNull Short anioLectivoId,
    @NotNull Short periodoAcademicoId,
    @NotNull Short areaCurricularId,
    @NotNull Long docenteUsuarioId,
    @NotBlank @Size(max = 40) String tema,
    @NotNull @FutureOrPresent LocalDate fechaProgramada,
    @NotNull LocalTime horaInicio,
    @NotNull LocalTime horaFin,
    @Size(max = 30) String aulaAsignada) {}
