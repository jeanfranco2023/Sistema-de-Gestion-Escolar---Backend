package com.colegio.shuji.usuario.infrastructure.adapter;

import com.colegio.shuji.usuario.application.port.out.SolicitudCambioCorreoPort;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class SolicitudCambioCorreoAdapter implements SolicitudCambioCorreoPort {

  private static final Logger log = LoggerFactory.getLogger(SolicitudCambioCorreoAdapter.class);

  private final JavaMailSender mailSender;
  private final String frontendUrl;
  private final String fromAddress;

  public SolicitudCambioCorreoAdapter(
      JavaMailSender mailSender,
      @Value("${app.frontend-url:${app.matricula-publica.frontend-url:http://localhost:4200}}")
          String frontendUrl,
      @Value("${app.mail.from:}") String fromAddress) {
    this.mailSender = mailSender;
    String url =
        frontendUrl != null && !frontendUrl.isBlank() ? frontendUrl.trim() : "http://localhost:4200";
    this.frontendUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    this.fromAddress = fromAddress == null ? "" : fromAddress.trim();
  }

  @Override
  public void enviarNotificacionSolicitudCambio(
      String destinatarioEmail,
      String codigoTicket,
      String tituloTicket,
      String estadoActual,
      String asunto,
      String cuerpoResumen) {
    if (destinatarioEmail == null || destinatarioEmail.isBlank()) {
      throw new IllegalArgumentException("El solicitante no tiene correo institucional.");
    }

    String enlace = frontendUrl + "/admin/solicitudes-cambio?codigo=" + codigoTicket;
    String textoPlano =
        "I.E.P. SHUJI KITAMURA - GESTIÓN DE SOLICITUDES DE CAMBIO\n\n"
            + "Folio: "
            + codigoTicket
            + "\n"
            + "Título: "
            + tituloTicket
            + "\n"
            + "Estado: "
            + estadoActual
            + "\n\n"
            + (cuerpoResumen != null ? cuerpoResumen : "")
            + "\n\nEnlace a la bandeja: "
            + enlace;

    String html =
        """
        <!DOCTYPE html>
        <html lang="es">
        <body style="margin:0;padding:24px;font-family:'Segoe UI',Arial,sans-serif;background-color:#f8fafc;color:#1e293b;">
          <div style="max-width:600px;margin:0 auto;background:#ffffff;border:1px solid #e2e8f0;border-radius:12px;overflow:hidden;">
            <div style="background:#14213d;color:#ffffff;padding:18px 24px;">
              <div style="font-size:12px;letter-spacing:0.08em;text-transform:uppercase;color:#fca311;font-weight:700;">I.E.P. Shuji Kitamura · Gestión de Cambios</div>
              <h2 style="margin:6px 0 0;font-size:18px;">%s · %s</h2>
            </div>
            <div style="padding:24px;">
              <p style="margin:0 0 12px;">Estado actual del ticket: <strong>%s</strong></p>
              <div style="background:#f1f5f9;border-left:4px solid #14213d;border-radius:6px;padding:14px 16px;white-space:pre-line;font-size:14px;color:#334155;">%s</div>
              <div style="margin-top:20px;">
                <a href="%s" style="display:inline-block;background:#14213d;color:#ffffff;text-decoration:none;padding:10px 18px;border-radius:8px;font-weight:600;font-size:14px;">Ver Solicitudes de Cambio</a>
              </div>
            </div>
          </div>
        </body>
        </html>
        """
            .formatted(
                escapeHtml(codigoTicket),
                escapeHtml(tituloTicket),
                escapeHtml(estadoActual),
                escapeHtml(cuerpoResumen != null ? cuerpoResumen : ""),
                escapeHtml(enlace));

    MimeMessage mime = mailSender.createMimeMessage();
    if (mime != null) {
      try {
        MimeMessageHelper helper =
            new MimeMessageHelper(mime, true, StandardCharsets.UTF_8.name());
        if (!fromAddress.isBlank()) {
          helper.setFrom(fromAddress);
        }
        helper.setTo(destinatarioEmail.trim());
        helper.setSubject(asunto);
        helper.setText(textoPlano, html);
        mailSender.send(mime);
        log.info(
            "Notificación de solicitud de cambio {} enviada a {}",
            codigoTicket,
            destinatarioEmail);
        return;
      } catch (Exception ex) {
        throw new IllegalStateException(
            "Error enviando correo MIME para solicitud " + codigoTicket + ": " + ex.getMessage(),
            ex);
      }
    }

    SimpleMailMessage fallback = new SimpleMailMessage();
    if (!fromAddress.isBlank()) {
      fallback.setFrom(fromAddress);
    }
    fallback.setTo(destinatarioEmail.trim());
    fallback.setSubject(asunto);
    fallback.setText(textoPlano);
    mailSender.send(fallback);
  }

  private String escapeHtml(String input) {
    if (input == null) {
      return "";
    }
    return input
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }
}
