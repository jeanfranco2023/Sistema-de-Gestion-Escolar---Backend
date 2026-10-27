package com.colegio.shuji.evaluacion.application.port.in;

public interface DescargarPlantillaCnebUseCase {
  byte[] descargar(Long asignacionId, Short periodoId, Short competenciaId);
}
