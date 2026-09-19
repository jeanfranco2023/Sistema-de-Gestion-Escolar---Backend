package com.colegio.shuji.convivencia.application.dto.out;

import com.colegio.shuji.convivencia.domain.enums.EstadoIncidencia;
import com.colegio.shuji.convivencia.domain.enums.TipoFalta;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record IncidenciaResponseDto(
    Long id,
    Long matriculaId,
    LocalDate fechaIncidencia,
    TipoFalta tipoFalta,
    String descripcion,
    Long reportadoPorUsuarioId,
    Boolean requiereCitacion,
    EstadoIncidencia estado,
    OffsetDateTime createdAt) {}
