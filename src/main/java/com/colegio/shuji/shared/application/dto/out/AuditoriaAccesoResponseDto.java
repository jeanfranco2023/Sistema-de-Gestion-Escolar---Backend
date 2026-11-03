package com.colegio.shuji.shared.application.dto.out;

import java.time.Instant;

public record AuditoriaAccesoResponseDto(
    Long id,
    String solicitudId,
    Long usuarioId,
    String usuarioNombre,
    String roles,
    String metodo,
    String ruta,
    Integer estadoHttp,
    String direccionIp,
    String agenteUsuario,
    Long duracionMs,
    Instant fechaEvento) {}
