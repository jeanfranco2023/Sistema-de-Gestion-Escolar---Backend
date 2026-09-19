package com.colegio.shuji.evaluacion.application.dto.out;

import com.colegio.shuji.evaluacion.domain.enums.CalificacionCualitativa;
import java.time.OffsetDateTime;

public record CalificacionResponseDto(
    Long id,
    Long matriculaId,
    Short anioLectivoId,
    Integer seccionId,
    Short periodoAcademicoId,
    Long asignacionDocenteId,
    Short areaCurricularId,
    Long docenteUsuarioId,
    Short competenciaId,
    CalificacionCualitativa calificacionCualitativa,
    String conclusionDescriptiva,
    Boolean sugerenciaIaUtilizada,
    Boolean requiereRefuerzo,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
