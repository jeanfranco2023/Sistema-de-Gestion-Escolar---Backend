package com.colegio.shuji.asistencia.infrastructure.adapter;

import com.colegio.shuji.asistencia.application.dto.out.ImportacionBiometricaResponseDto;
import com.colegio.shuji.asistencia.application.port.out.ImportacionBiometricaEstadoPort;
import com.colegio.shuji.asistencia.infrastructure.repository.JpaImportacionBiometricaRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Adaptador JPA para consultar el estado de importaciones biométricas. */
@Component
@RequiredArgsConstructor
public class ImportacionBiometricaEstadoAdapter implements ImportacionBiometricaEstadoPort {
  private final JpaImportacionBiometricaRepository repository;

  @Override
  public Optional<ImportacionBiometricaResponseDto> buscarEstado(UUID id, Long usuarioId) {
    return repository.buscarEstado(id, usuarioId)
        .map(estado -> new ImportacionBiometricaResponseDto(
            estado.getId(),
            estado.getNombreArchivo(),
            estado.getEstado(),
            estado.getTotalFilas(),
            estado.getMarcasValidas(),
            estado.getMarcasErroneas(),
            estado.getLoteId(),
            estado.getMensajeError(),
            estado.getCreadoEn(),
            estado.getIniciadoEn(),
            estado.getFinalizadoEn()));
  }
}
