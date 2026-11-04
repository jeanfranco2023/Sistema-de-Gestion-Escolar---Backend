package com.colegio.shuji.matricula.application.port.out;

import java.util.UUID;

public interface SeguimientoSolicitudCorreoPort {
  void enviarEnlaceSeguimiento(String destinatario, UUID solicitudId, String token);

  default void enviarActualizacionEstado(String destinatario, UUID solicitudId, String token, String nuevoEstado, String mensaje) {
    enviarEnlaceSeguimiento(destinatario, solicitudId, token);
  }
}
