package com.colegio.shuji.matricula.application.dto.out;

import com.colegio.shuji.matricula.domain.enums.*;
import java.time.*;
import java.util.*;

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
