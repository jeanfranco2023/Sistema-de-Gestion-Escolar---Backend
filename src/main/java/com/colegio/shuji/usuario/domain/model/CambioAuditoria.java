package com.colegio.shuji.usuario.domain.model;

import java.time.Instant;

public record CambioAuditoria(
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
    Instant fechaCreacion) {}
