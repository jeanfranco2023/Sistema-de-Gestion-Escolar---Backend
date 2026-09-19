package com.colegio.shuji.matricula.application.dto.out;

import com.colegio.shuji.matricula.domain.enums.EstadoMatricula;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MatriculaResponseDto(
    Long id,
    UUID uuid,
    Short anioLectivoId,
    Long estudianteId,
    Integer seccionId,
    OffsetDateTime fechaMatricula,
    EstadoMatricula estadoMatricula,
    OffsetDateTime reservaExpiraAt,
    String observaciones) {}
