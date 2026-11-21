package com.colegio.shuji.matricula.application.port.out;

import java.util.UUID;

public interface SeguimientoSolicitudCorreoPort {
  void enviarEnlaceSeguimiento(String destinatario, UUID solicitudId, String token);

  default void enviarActualizacionEstado(String destinatario, UUID solicitudId, String token, String nuevoEstado, String mensaje) {
    enviarEnlaceSeguimiento(destinatario, solicitudId, token);
  }

  default void enviarCredencialesMatricula(
      String destinatario,
      String nombreApoderado,
      String nombreEstudiante,
      String passwordTemporal,
      String detalleGrado,
      String comprobantePago,
      UUID solicitudId,
      String token) {
    enviarCredencialesMatricula(destinatario, nombreApoderado, nombreEstudiante, passwordTemporal, detalleGrado, comprobantePago, solicitudId, token, null, null);
  }

  default void enviarCredencialesMatricula(
      String destinatario,
      String nombreApoderado,
      String nombreEstudiante,
      String passwordTemporal,
      String detalleGrado,
      String comprobantePago,
      UUID solicitudId,
      String token,
      byte[] pdfComprobante,
      String nombreArchivoPdf) {
    enviarActualizacionEstado(destinatario, solicitudId, token, "PAGO_CONFIRMADO", "Pago de matrícula confirmado");
  }
}
