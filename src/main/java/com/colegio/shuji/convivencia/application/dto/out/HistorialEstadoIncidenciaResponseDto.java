package com.colegio.shuji.convivencia.application.dto.out;

import com.colegio.shuji.convivencia.domain.enums.EstadoIncidencia;
import java.time.OffsetDateTime;

public record HistorialEstadoIncidenciaResponseDto(
    Long id,
    EstadoIncidencia estadoAnterior,
    EstadoIncidencia estadoNuevo,
    Long usuarioId,
    String nombreUsuario,
    OffsetDateTime fechaCambio,
    String observacion,
    String resultadoVerificado,
    String participantes,
    String comunicacionApoderado) {}
