package com.colegio.shuji.asistencia.application.dto.out;

import com.colegio.shuji.asistencia.domain.enums.EstadoImportacionBiometrica;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ImportacionBiometricaResponseDto(
    UUID id,
    String nombreArchivo,
    EstadoImportacionBiometrica estado,
    Integer totalFilas,
    Integer marcasValidas,
    Integer marcasErroneas,
    Long loteId,
    String mensajeError,
    OffsetDateTime creadoEn,
    OffsetDateTime iniciadoEn,
    OffsetDateTime finalizadoEn) {}
