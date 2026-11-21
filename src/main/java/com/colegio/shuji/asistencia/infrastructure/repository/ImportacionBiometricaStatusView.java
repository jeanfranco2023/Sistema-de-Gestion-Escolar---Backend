package com.colegio.shuji.asistencia.infrastructure.repository;

import com.colegio.shuji.asistencia.domain.enums.EstadoImportacionBiometrica;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface ImportacionBiometricaStatusView {
  UUID getId();
  Long getUsuarioId();
  String getNombreArchivo();
  EstadoImportacionBiometrica getEstado();
  Integer getTotalFilas();
  Integer getMarcasValidas();
  Integer getMarcasErroneas();
  Long getLoteId();
  String getMensajeError();
  OffsetDateTime getCreadoEn();
  OffsetDateTime getIniciadoEn();
  OffsetDateTime getFinalizadoEn();
}
