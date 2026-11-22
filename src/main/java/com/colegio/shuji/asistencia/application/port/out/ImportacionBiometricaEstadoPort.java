package com.colegio.shuji.asistencia.application.port.out;

import com.colegio.shuji.asistencia.application.dto.out.ImportacionBiometricaResponseDto;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida para consultar el estado histórico de importaciones biométricas. */
public interface ImportacionBiometricaEstadoPort {
  Optional<ImportacionBiometricaResponseDto> buscarEstado(UUID id, Long usuarioId);
}
