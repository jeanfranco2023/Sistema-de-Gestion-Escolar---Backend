package com.colegio.shuji.asistencia.application.port.out;

public interface AlmacenJustificacionPort {
  record Archivo(String clave) {}

  Archivo guardar(Long asistenciaId, byte[] contenido);

  String urlFirmada(String clave);

  void eliminar(String clave);
}
