package com.colegio.shuji.convivencia.domain.model;

import com.colegio.shuji.convivencia.domain.enums.EstadoIncidencia;
import java.time.OffsetDateTime;

public record CambioEstadoIncidencia(
    Long id,
    Long incidenciaId,
    EstadoIncidencia estadoAnterior,
    EstadoIncidencia estadoNuevo,
    Long usuarioId,
    String nombreUsuario,
    OffsetDateTime fechaCambio,
    String observacion,
    String resultadoVerificado,
    String participantes,
    String comunicacionApoderado) {}
