package com.colegio.shuji.matricula.infrastructure.adapter;

import com.colegio.shuji.matricula.application.port.out.SeguimientoSolicitudCorreoPort;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Adaptador de mensajería y correo institucional para notificaciones, seguimiento de solicitudes
 * y entrega de credenciales de acceso para matrícula.
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
    String asunto = "Enlace para consultar tu solicitud de matrícula";

    String textoPlano = """
        I.E.P. SHUJI KITAMURA - SOLICITUD DE MATRÍCULA

        Puedes consultar el estado de tu solicitud de matrícula en este enlace:

        %s

        Si no solicitaste esta matrícula, puedes ignorar este mensaje.
        """.formatted(enlace);

    String cuerpoHtml = """
        <p style="margin-top: 0; margin-bottom: 16px;">
          Hemos recibido tu solicitud de matrícula institucional. Puedes consultar el avance de tu trámite, validar documentos y continuar con los siguientes pasos desde nuestro portal en cualquier momento:
        </p>
        <div style="background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 10px; padding: 14px 18px; margin: 20px 0;">
          <span style="color: #64748b; font-size: 13px;">Código de Solicitud:</span><br>
          <strong style="color: #14213d; font-family: monospace; font-size: 15px;">%s</strong>
        </div>
        """.formatted(solicitudId);

    String ctaHtml = botonCta("Consultar mi Solicitud de Matrícula", enlace);
    String enlaceSecundario = enlaceDirecto(enlace);

    String html = plantillaBase("SOLICITUD DE MATRÍCULA · SEGUIMIENTO", cuerpoHtml, ctaHtml, enlaceSecundario);
    enviar(destinatario, asunto, textoPlano, html, solicitudId);
  }

  @Override
  public void enviarActualizacionEstado(String destinatario, UUID solicitudId, String token, String nuevoEstado, String mensaje) {
    String enlace = enlaceSeguimiento(solicitudId, token);
    String detallePlano = mensaje == null || mensaje.isBlank() ? "" : "\n\n" + mensaje.trim();
    String asunto = "Actualización de tu solicitud de matrícula";

    String textoPlano = "El estado de tu solicitud de matrícula cambió a: " + nuevoEstado + "."
        + detallePlano + "\n\nConsulta el detalle en este enlace:\n\n" + enlace;

    String estadoEtiqueta = formatearEstado(nuevoEstado);
    String mensajeHtml = (mensaje != null && !mensaje.isBlank())
        ? """
          <div style="background-color: #f1f5f9; border-left: 4px solid #14213d; border-radius: 8px; padding: 14px 18px; margin: 18px 0; font-size: 13px; color: #334155;">
            %s
          </div>
          """.formatted(escapeHtml(mensaje.trim()))
        : "";

    String cuerpoHtml = """
        <p style="margin-top: 0; margin-bottom: 16px;">
          Te informamos que tu solicitud de matrícula ha cambiado de estado:
        </p>
        <div style="margin: 20px 0;">
          <span style="display: inline-block; background-color: #eff6ff; border: 1px solid #bfdbfe; color: #1e40af; font-size: 13px; font-weight: 700; padding: 6px 14px; border-radius: 20px;">
            %s
          </span>
        </div>
        %s
        """.formatted(escapeHtml(estadoEtiqueta), mensajeHtml);

    String ctaHtml = botonCta("Ver Detalle y Continuar Trámite", enlace);
    String enlaceSecundario = enlaceDirecto(enlace);

    String html = plantillaBase("ACTUALIZACIÓN DE ESTADO · MATRÍCULA", cuerpoHtml, ctaHtml, enlaceSecundario);
    enviar(destinatario, asunto, textoPlano, html, solicitudId);
  }

  @Override
  public void enviarCredencialesMatricula(
      String destinatario,
      String nombreApoderado,
      String nombreEstudiante,
      String passwordTemporal,
      String detalleGrado,
      String comprobantePago,
      UUID solicitudId,
      String token) {
    enviarCredencialesMatricula(
        destinatario,
        nombreApoderado,
        nombreEstudiante,
        passwordTemporal,
        detalleGrado,
        comprobantePago,
        solicitudId,
        token,
        null,
        null);
  }

  @Override
  public void enviarCredencialesMatricula(
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

    String enlaceSeguimiento = enlaceSeguimiento(solicitudId, token);
    String enlaceLogin = enlaceLogin();
    String asunto = "Pago de matrícula confirmado - I.E.P. Shuji Kitamura";
    String nombreAdjunto = (nombreArchivoPdf != null && !nombreArchivoPdf.isBlank()) ? nombreArchivoPdf.trim() : "Constancia_Matricula.pdf";

    boolean accesoAdjunto = passwordTemporal != null && !passwordTemporal.isBlank()
        && pdfComprobante != null && pdfComprobante.length > 0;
    String detallePassPlano = accesoAdjunto
        ? "Tu usuario y contraseña temporal están en el PDF adjunto. Cámbiala al ingresar por primera vez.\n" + enlaceLogin
        : (passwordTemporal != null && !passwordTemporal.isBlank())
        ? """
          Acceso al Portal Institucional:
          - Enlace de ingreso: %s
          - Usuario / Correo: %s
          - Contraseña temporal: %s

          IMPORTANTE: Al ingresar por primera vez con tu contraseña temporal, la plataforma te solicitará inmediatamente cambiarla por tu clave definitiva por motivos de seguridad institucional.
          """.formatted(enlaceLogin, destinatario.trim(), passwordTemporal.trim())
        : """
          Acceso al Portal Institucional:
          - Enlace de ingreso: %s
          - Usuario / Correo: %s

          Si ya tienes una cuenta, ingresa con tu contraseña habitual.
          Si no recuerdas tu clave, puedes recuperarla en el enlace de ingreso seleccionando "¿Olvidaste tu clave?".
          """.formatted(enlaceLogin, destinatario.trim());

    String avisoAdjuntoPlano = (pdfComprobante != null && pdfComprobante.length > 0)
        ? "\n📎 COMPROBANTE OFICIAL ADJUNTO:\nHemos adjuntado a este correo tu comprobante electrónico / boucher oficial en formato PDF (%s) para tus registros.\n".formatted(nombreAdjunto)
        : "";

    String textoPlano = """
        I.E.P. SHUJI KITAMURA - PAGO DE MATRÍCULA CONFIRMADO

        Estimado(a) %s:
        ¡Felicitaciones! Te confirmamos que el pago de la matrícula para tu menor hijo(a) %s ha sido procesado exitosamente.

        Detalles de la matrícula:
        - Estudiante: %s
        - Grado / Nivel: %s
        - Comprobante de pago: %s
        %s
        %s

        Consulta de constancia y seguimiento:
        %s
        """.formatted(
            nombreApoderado != null ? nombreApoderado : "Apoderado(a)",
            nombreEstudiante != null ? nombreEstudiante : "el estudiante",
            nombreEstudiante != null ? nombreEstudiante : "-",
            detalleGrado != null ? detalleGrado : "-",
            comprobantePago != null ? comprobantePago : "-",
            avisoAdjuntoPlano,
            detallePassPlano,
            enlaceSeguimiento);

    String bloqueCredencialesHtml;
    if (accesoAdjunto) {
      bloqueCredencialesHtml = "<p>Tu usuario y contraseña temporal están en el PDF adjunto. "
          + "El portal te pedirá cambiarla en el primer ingreso.</p>";
    } else if (passwordTemporal != null && !passwordTemporal.isBlank()) {
      bloqueCredencialesHtml = """
          <div style="background-color: #fffdfa; border: 1px solid #e9d5a1; border-left: 4px solid #c49a58; border-radius: 10px; padding: 18px 20px; margin: 24px 0;">
            <div style="font-size: 14px; font-weight: 700; color: #76551f; margin-bottom: 12px;">
              🔑 Tus Credenciales de Acceso al Portal Institucional
            </div>
            <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%%" style="font-size: 13px;">
              <tr>
                <td style="padding: 6px 0; color: #64748b; width: 38%%;">Usuario / Correo:</td>
                <td style="padding: 6px 0; color: #14213d; font-weight: 700;">%s</td>
              </tr>
              <tr>
                <td style="padding: 6px 0; color: #64748b;">Contraseña temporal:</td>
                <td style="padding: 6px 0;">
                  <span style="display: inline-block; background-color: #f1f5f9; border: 1px dashed #94a3b8; padding: 4px 12px; font-family: 'Consolas', 'Courier New', monospace; font-size: 14px; font-weight: 700; color: #14213d; border-radius: 6px;">
                    %s
                  </span>
                </td>
              </tr>
            </table>
            <div style="margin-top: 14px; padding: 10px 12px; background-color: #fef3c7; border-radius: 8px; font-size: 12px; color: #92400e; line-height: 1.45;">
              ⚠️ <strong>Primer inicio de sesión:</strong> Por seguridad institucional, al iniciar sesión por primera vez con esta contraseña temporal, el sistema te solicitará establecer tu contraseña definitiva y personal.
            </div>
          </div>
          """.formatted(escapeHtml(destinatario.trim()), escapeHtml(passwordTemporal.trim()));
    } else {
      bloqueCredencialesHtml = """
          <div style="background-color: #f0fdf4; border: 1px solid #bbf7d0; border-left: 4px solid #16a34a; border-radius: 10px; padding: 16px 20px; margin: 24px 0;">
            <div style="font-size: 14px; font-weight: 700; color: #15803d; margin-bottom: 6px;">
              Acceso al portal institucional
            </div>
            <p style="margin: 0; font-size: 13px; color: #334155;">
              Si ya tienes una cuenta con el correo <strong>%s</strong>, ingresa con tu contraseña habitual.
            </p>
            <p style="margin: 8px 0 0 0; font-size: 12px; color: #64748b;">
              Si no recuerdas tu contraseña, puedes recuperarla en cualquier momento seleccionando <em>"¿Olvidaste tu clave?"</em> en la pantalla de inicio de sesión.
            </p>
          </div>
          """.formatted(escapeHtml(destinatario.trim()));
    }

    String avisoAdjuntoHtml = (pdfComprobante != null && pdfComprobante.length > 0)
        ? """
          <div style="background-color: #f0fdf4; border: 1px solid #bbf7d0; border-left: 4px solid #16a34a; border-radius: 10px; padding: 14px 18px; margin: 20px 0;">
            <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%%">
              <tr>
                <td style="width: 32px; vertical-align: top; font-size: 20px;">📎</td>
                <td style="vertical-align: top;">
                  <strong style="color: #15803d; font-size: 13px; display: block; margin-bottom: 2px;">
                    Constancia de pago adjunta en PDF
                  </strong>
                  <span style="font-size: 12px; color: #334155; line-height: 1.45; display: block;">
                    Hemos adjuntado a este correo el documento oficial <strong>%s</strong> para tu resguardo y archivo familiar.
                  </span>
                </td>
              </tr>
            </table>
          </div>
          """.formatted(escapeHtml(nombreAdjunto))
        : "";

    String cuerpoHtml = """
        <div style="margin-bottom: 16px;">
          <span style="display: inline-block; background-color: #ecfdf5; border: 1px solid #a7f3d0; color: #065f46; font-size: 12px; font-weight: 700; padding: 5px 14px; border-radius: 20px; text-transform: uppercase; letter-spacing: 0.5px;">
            ✓ Pago y Matrícula Confirmados
          </span>
        </div>
        <p style="margin-top: 0; margin-bottom: 14px; font-size: 15px;">
          Hola, <strong>%s</strong>:
        </p>
        <p style="margin-top: 0; margin-bottom: 18px;">
          Confirmamos el pago de matrícula de <strong>%s</strong>. Puedes consultar los datos de la sección y el avance de la matrícula en el enlace de seguimiento.
        </p>

        <!-- Ficha Resumen -->
        <div style="background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 16px 20px; margin: 20px 0;">
          <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%%" style="font-size: 13px;">
            <tr>
              <td style="padding: 5px 0; color: #64748b; width: 38%%;">Estudiante:</td>
              <td style="padding: 5px 0; color: #1e293b; font-weight: 600;">%s</td>
            </tr>
            <tr>
              <td style="padding: 5px 0; color: #64748b;">Grado / Nivel:</td>
              <td style="padding: 5px 0; color: #1e293b; font-weight: 600;">%s</td>
            </tr>
            <tr>
              <td style="padding: 5px 0; color: #64748b;">Comprobante:</td>
              <td style="padding: 5px 0; color: #1e293b; font-weight: 600;">%s</td>
            </tr>
          </table>
        </div>

        %s

        %s
        """.formatted(
            nombreApoderado != null ? escapeHtml(nombreApoderado) : "Apoderado(a)",
            nombreEstudiante != null ? escapeHtml(nombreEstudiante) : "el estudiante",
            nombreEstudiante != null ? escapeHtml(nombreEstudiante) : "-",
            detalleGrado != null ? escapeHtml(detalleGrado) : "-",
            comprobantePago != null ? escapeHtml(comprobantePago) : "-",
            avisoAdjuntoHtml,
            bloqueCredencialesHtml);

    String ctaHtml = botonCta("Ingresar al Portal Institucional", enlaceLogin);
    String enlaceSecundario = """
        <div style="text-align: center; margin-top: 14px;">
          <a href="%s" style="color: #76551f; font-size: 13px; font-weight: 600; text-decoration: underline;">
            Ver constancia y ficha de matrícula en línea &rarr;
          </a>
        </div>
        """.formatted(enlaceSeguimiento);

    String html = plantillaBase("PAGO DE MATRÍCULA CONFIRMADO · PLATAFORMA EDUCATIVA", cuerpoHtml, ctaHtml, enlaceSecundario);
    if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
      org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
          new org.springframework.transaction.support.TransactionSynchronization() {
            @Override
            public void afterCommit() {
              enviar(destinatario, asunto, textoPlano, html, solicitudId, pdfComprobante, nombreAdjunto);
            }
          });
    } else {
      enviar(destinatario, asunto, textoPlano, html, solicitudId, pdfComprobante, nombreAdjunto);
    }
  }

  private String enlaceSeguimiento(UUID solicitudId, String token) {
    return "%s/matricula/inicio?token=%s&id=%s".formatted(frontendUrl, token, solicitudId);
  }

  private String enlaceLogin() {
    return "%s/auth/login".formatted(frontendUrl);
  }

  private String botonCta(String texto, String enlace) {
    return """
        <div style="text-align: center; margin: 28px 0 16px 0;">
          <a href="%s" style="display: inline-block; background-color: #14213d; color: #ffffff !important; padding: 13px 28px; font-size: 14px; font-weight: 600; text-decoration: none; border-radius: 8px; box-shadow: 0 4px 12px rgba(20, 33, 61, 0.15);">
            %s &rarr;
          </a>
        </div>
        """.formatted(enlace, escapeHtml(texto));
  }

  private String enlaceDirecto(String enlace) {
    return """
        <div style="margin-top: 18px; font-size: 12px; color: #64748b; text-align: center;">
          Si el botón no funciona, copia y pega este enlace en tu navegador:<br>
          <a href="%s" style="color: #1e40af; word-break: break-all;">%s</a>
        </div>
        """.formatted(enlace, enlace);
  }

  private String plantillaBase(
      String subtitulo,
      String contenidoPrincipal,
      String callToActionHtml,
      String enlaceSecundarioHtml) {
    return """
        <!DOCTYPE html>
        <html lang="es">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>I.E.P. Shuji Kitamura</title>
        </head>
        <body style="margin: 0; padding: 0; background-color: #f4f6fb; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #1e293b; -webkit-font-smoothing: antialiased;">
          <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%%" style="background-color: #f4f6fb; padding: 32px 12px;">
            <tr>
              <td align="center">
                <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%%" style="max-width: 580px; background-color: #ffffff; border-radius: 16px; border: 1px solid #e2e8f0; overflow: hidden; box-shadow: 0 10px 25px -5px rgba(20, 33, 61, 0.08);">
                  <!-- Encabezado Institucional -->
                  <tr>
                    <td style="background-color: #14213d; border-top: 4px solid #c49a58; padding: 28px 32px; text-align: center;">
                      <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%%">
                        <tr>
                          <td align="center">
                            <div style="display: inline-block; width: 44px; height: 44px; line-height: 44px; border-radius: 50%%; background-color: #1f3156; border: 2px solid #c49a58; color: #ffffff; font-weight: 800; font-size: 16px; margin-bottom: 10px; text-align: center;">
                              SK
                            </div>
                            <div style="font-size: 19px; font-weight: 800; color: #ffffff; letter-spacing: 0.8px; margin: 0;">
                              I.E.P. SHUJI KITAMURA
                            </div>
                            <div style="font-size: 11px; font-weight: 600; color: #c49a58; letter-spacing: 1.6px; text-transform: uppercase; margin-top: 4px;">
                              %s
                            </div>
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>

                  <!-- Contenido del Mensaje -->
                  <tr>
                    <td style="padding: 32px 28px; line-height: 1.6; font-size: 14px; color: #334155;">
                      %s

                      %s

                      %s
                    </td>
                  </tr>

                  <!-- Pie de Página Institucional -->
                  <tr>
                    <td style="background-color: #f8fafc; border-top: 1px solid #e2e8f0; padding: 22px 28px; text-align: center; font-size: 12px; color: #64748b; line-height: 1.5;">
                      <strong style="color: #1e293b;">I.E.P. Shuji Kitamura</strong> &bull; Excelencia y Formación Integral<br>
                      Plataforma de Matrícula y Gestión Escolar Institucional<br>
                      <span style="font-size: 11px; color: #94a3b8; display: inline-block; margin-top: 8px;">
                        Este es un mensaje institucional automático. Si no reconoces esta operación o necesitas ayuda, contáctanos a soporte@shujikitamura.edu.pe
                      </span>
                    </td>
                  </tr>
                </table>
              </td>
            </tr>
          </table>
        </body>
        </html>
        """.formatted(subtitulo, contenidoPrincipal, callToActionHtml, enlaceSecundarioHtml);
  }

  private void enviar(String destinatario, String asunto, String textoPlano, String contenidoHtml, UUID solicitudId) {
    enviar(destinatario, asunto, textoPlano, contenidoHtml, solicitudId, null, null);
  }

  private void enviar(
      String destinatario,
      String asunto,
      String textoPlano,
      String contenidoHtml,
      UUID solicitudId,
      byte[] adjuntoBytes,
      String nombreAdjunto) {
    String remitente = fromAddress.isBlank() ? "no-reply@shujikitamura.edu.pe" : fromAddress;
    try {
      MimeMessage mimeMessage = mailSender != null ? mailSender.createMimeMessage() : null;
      if (mimeMessage != null) {
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
        helper.setFrom(remitente);
        helper.setTo(destinatario.trim());
        helper.setSubject(asunto);
        helper.setText(textoPlano, contenidoHtml);
        if (adjuntoBytes != null && adjuntoBytes.length > 0) {
          helper.addAttachment(
              nombreAdjunto != null && !nombreAdjunto.isBlank() ? nombreAdjunto : "Constancia_Matricula.pdf",
              new org.springframework.core.io.ByteArrayResource(adjuntoBytes),
              "application/pdf");
        }
        mailSender.send(mimeMessage);
        log.info("Correo de matrícula HTML enviado exitosamente vía SMTP para la solicitud {}", solicitudId);
        return;
      }
      if (adjuntoBytes != null && adjuntoBytes.length > 0) {
        throw new IllegalStateException("SMTP no pudo preparar el PDF adjunto");
      }
      SimpleMailMessage correo = new SimpleMailMessage();
      correo.setFrom(remitente);
      correo.setTo(destinatario.trim());
      correo.setSubject(asunto);
      correo.setText(textoPlano);
      mailSender.send(correo);
      log.info("Correo de matrícula enviado exitosamente vía SMTP para la solicitud {}", solicitudId);
    } catch (Exception ex) {
      log.warn("No se pudo entregar el correo por SMTP ({}: {}) para la solicitud {}",
          ex.getClass().getSimpleName(), ex.getMessage(), solicitudId);
    }
  }

  private String formatearEstado(String estado) {
    if (estado == null) return "En trámite";
    return switch (estado) {
      case "DOCUMENTOS_VALIDADOS" -> "Documentación validada exitosamente";
      case "DOCUMENTOS_OBSERVADOS" -> "Documentación observada · Requiere corrección";
      case "DOCUMENTOS_EN_VALIDACION" -> "Documentos en proceso de validación";
      case "AUTORIZACION_EDAD_PENDIENTE" -> "Pendiente de autorización institucional de edad";
      case "PAGO_PENDIENTE" -> "Documentos aprobados · Pago pendiente";
      case "PAGO_CONFIRMADO_PENDIENTE_ASIGNACION" -> "Pago confirmado · Pendiente de asignación de aula";
      case "MATRICULADA" -> "Matrícula finalizada y aula asignada";
      case "RECHAZADA" -> "Solicitud desestimada institucionalmente";
      default -> estado.replace('_', ' ');
    };
  }

  private String escapeHtml(String text) {
    if (text == null) return "";
    return text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;");
  }
}
