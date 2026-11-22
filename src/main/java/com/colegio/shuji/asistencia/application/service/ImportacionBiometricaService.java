package com.colegio.shuji.asistencia.application.service;

import com.colegio.shuji.asistencia.application.dto.in.ImportarLoteBiometricoRequestDto;
import com.colegio.shuji.asistencia.application.dto.out.ImportacionBiometricaResponseDto;
import com.colegio.shuji.asistencia.application.dto.out.LoteBiometricoResponseDto;
import com.colegio.shuji.asistencia.application.port.in.ProcesarBiometricoUseCase;
import com.colegio.shuji.asistencia.application.port.out.ImportacionBiometricaEstadoPort;
import com.colegio.shuji.asistencia.domain.enums.EstadoImportacionBiometrica;
import com.colegio.shuji.shared.application.port.out.ActorActualPort;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Procesa lotes biométricos en línea manteniendo el contrato de estado para consultas existentes.
 */
@Service
@RequiredArgsConstructor
public class ImportacionBiometricaService {
  private final ImportacionBiometricaEstadoPort estadoPort;
  private final ProcesarBiometricoUseCase procesador;
  private final ActorActualPort actor;

  /**
   * Importa el lote biométrico en línea y devuelve el resumen en estado COMPLETADO.
   */
  @Transactional
  public ImportacionBiometricaResponseDto encolar(ImportarLoteBiometricoRequestDto request) {
    LoteBiometricoResponseDto lote = procesador.importar(request);
    return new ImportacionBiometricaResponseDto(
        UUID.randomUUID(),
        lote.nombreArchivo(),
        EstadoImportacionBiometrica.COMPLETADO,
        lote.totalFilas(),
        lote.marcasValidas(),
        lote.marcasErroneas(),
        lote.id(),
        null,
        lote.fechaCreacion(),
        lote.fechaCreacion(),
        lote.fechaCreacion());
  }

  /**
   * Consulta el estado persistido de una importación biométrica histórica del usuario autenticado.
   */
  @Transactional(readOnly = true)
  public ImportacionBiometricaResponseDto estado(UUID id) {
    return estadoPort.buscarEstado(id, actor.usuarioId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Importación no encontrada"));
  }
}
