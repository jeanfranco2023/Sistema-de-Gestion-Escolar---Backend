package com.colegio.shuji.matricula.application.port.out;

import java.util.UUID;

public interface SeguimientoSolicitudCorreoPort {
  void enviarEnlaceSeguimiento(String destinatario, UUID solicitudId, String token);
}
