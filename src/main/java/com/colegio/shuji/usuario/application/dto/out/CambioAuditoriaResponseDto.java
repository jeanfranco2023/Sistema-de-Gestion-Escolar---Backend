package com.colegio.shuji.usuario.application.dto.out;

import java.time.Instant;

public record CambioAuditoriaResponseDto(
    Long id,
    String tablaAfectada,
    Long registroId,
    String registroClave,
    String accion,
    String datosAnteriores,
    String datosNuevos,
    Long usuarioId,
    String usuarioNombre,
    String roles,
    boolean contextoHistoricoSinSnapshot,
    Instant fechaCreacion) {}
