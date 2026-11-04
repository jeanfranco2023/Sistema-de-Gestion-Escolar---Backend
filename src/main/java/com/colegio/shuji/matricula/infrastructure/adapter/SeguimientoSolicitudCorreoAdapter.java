package com.colegio.shuji.matricula.infrastructure.adapter;

import com.colegio.shuji.matricula.application.port.out.SeguimientoSolicitudCorreoPort;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Adaptador de mensajería y correo institucional para notificaciones y seguimiento de solicitudes de matrícula.
 */
@Component
public class SeguimientoSolicitudCorreoAdapter implements SeguimientoSolicitudCorreoPort {
  private static final Logger log = LoggerFactory.getLogger(SeguimientoSolicitudCorreoAdapter.class);

  private final JavaMailSender mailSender;
  private final String frontendUrl;
  private final String fromAddress;

  public SeguimientoSolicitudCorreoAdapter(
      JavaMailSender mailSender,
      @Value("${app.frontend-url:${app.matricula-publica.frontend-url:http://localhost:4200}}") String frontendUrl,
      @Value("${app.mail.from:}") String fromAddress) {
    this.mailSender = mailSender;
    String url = frontendUrl != null && !frontendUrl.isBlank() ? frontendUrl.trim() : "http://localhost:4200";
    this.frontendUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    this.fromAddress = fromAddress == null ? "" : fromAddress.trim();
  }

  @Override
  public void enviarEnlaceSeguimiento(String destinatario, UUID solicitudId, String token) {
    String enlace = enlaceSeguimiento(solicitudId, token);
    enviar(destinatario, "Enlace para consultar tu solicitud de matrícula",
        "Puedes consultar el estado de tu solicitud de matrícula en este enlace:\n\n"
            + enlace + "\n\nSi no solicitaste esta matrícula, puedes ignorar este mensaje.", solicitudId);
  }

  @Override
  public void enviarActualizacionEstado(String destinatario, UUID solicitudId, String token, String nuevoEstado, String mensaje) {
    String enlace = enlaceSeguimiento(solicitudId, token);
    String detalle = mensaje == null || mensaje.isBlank() ? "" : "\n\n" + mensaje.trim();
    enviar(destinatario, "Actualización de tu solicitud de matrícula",
        "El estado de tu solicitud de matrícula cambió a: " + nuevoEstado + "."
            + detalle + "\n\nConsulta el detalle en este enlace:\n\n" + enlace, solicitudId);
  }

  private String enlaceSeguimiento(UUID solicitudId, String token) {
    return "%s/matricula/inicio?token=%s&id=%s".formatted(frontendUrl, token, solicitudId);
  }

  private void enviar(String destinatario, String asunto, String contenido, UUID solicitudId) {
    if (fromAddress.isBlank()) {
      throw new IllegalStateException("SMTP_FROM no está configurado");
    }
    SimpleMailMessage correo = new SimpleMailMessage();
    correo.setFrom(fromAddress);
    correo.setTo(destinatario.trim());
    correo.setSubject(asunto);
    correo.setText(contenido);
    mailSender.send(correo);
    log.info("Correo de matrícula enviado para la solicitud {}", solicitudId);
  }
}
