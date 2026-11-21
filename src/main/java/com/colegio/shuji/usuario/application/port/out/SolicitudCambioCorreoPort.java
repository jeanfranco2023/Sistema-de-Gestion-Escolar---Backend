package com.colegio.shuji.usuario.application.port.out;

public interface SolicitudCambioCorreoPort {

  void enviarNotificacionSolicitudCambio(
      String destinatarioEmail,
      String codigoTicket,
      String tituloTicket,
      String estadoActual,
      String asunto,
      String cuerpoResumen);
}
