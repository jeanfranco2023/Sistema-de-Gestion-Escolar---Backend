package com.colegio.shuji.matricula.infrastructure.adapter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class SeguimientoSolicitudCorreoAdapterTest {
  @Test
  void applicationYamlDefineLasPropiedadesDeBrevoSmtp() throws IOException {
    var propiedades = new YamlPropertySourceLoader()
        .load("application", new FileSystemResource("src/main/resources/application.yml")).getFirst();

    Assertions.assertEquals("${SMTP_HOST:smtp-relay.brevo.com}", propiedades.getProperty("spring.mail.host"));
    Assertions.assertEquals("${SMTP_PORT:2525}", propiedades.getProperty("spring.mail.port"));
    Assertions.assertEquals("${SMTP_USERNAME:}", propiedades.getProperty("spring.mail.username"));
    Assertions.assertEquals("${SMTP_PASSWORD:}", propiedades.getProperty("spring.mail.password"));
    Assertions.assertEquals("${SMTP_FROM:}", propiedades.getProperty("app.mail.from"));

    var renderSource = (org.springframework.core.env.EnumerablePropertySource<?>) new YamlPropertySourceLoader()
        .load("render", new FileSystemResource("render.yaml")).getFirst();
    Assertions.assertTrue(renderSource.getPropertyNames().length > 0);
    String clavePuerto = java.util.Arrays.stream(renderSource.getPropertyNames())
        .filter(nombre -> nombre.endsWith(".key") && "SMTP_PORT".equals(renderSource.getProperty(nombre)))
        .findFirst()
        .map(nombre -> nombre.substring(0, nombre.length() - 4) + ".value")
        .orElseThrow();
    Assertions.assertEquals("2525", String.valueOf(renderSource.getProperty(clavePuerto)));
  }

  @Test
  void enviaEnlaceDeSeguimientoPorSmtp() {
    JavaMailSender mailSender = mock(JavaMailSender.class);
    var adapter = new SeguimientoSolicitudCorreoAdapter(
        mailSender, "https://portal.example.test/", "matricula@example.test");
    UUID solicitudId = UUID.randomUUID();
    String token = "test-" + UUID.randomUUID();

    adapter.enviarEnlaceSeguimiento("familia@example.test", solicitudId, token);

    SimpleMailMessage correo = capturarCorreo(mailSender);
    assertEquals("matricula@example.test", correo.getFrom());
    assertArrayEquals(new String[] {"familia@example.test"}, correo.getTo());
    assertEquals("Enlace para consultar tu solicitud de matrícula", correo.getSubject());
    assertTrue(correo.getText().contains(
        "https://portal.example.test/matricula/inicio?token=" + token + "&id=" + solicitudId));
  }

  @Test
  void simulaEnvioAlCorreoDePruebaSolicitado() {
    JavaMailSender mailSender = mock(JavaMailSender.class);
    var adapter = new SeguimientoSolicitudCorreoAdapter(
        mailSender, "http://localhost:4200/", "matricula@example.test");

    adapter.enviarEnlaceSeguimiento(
        "megamedifire98@gmail.com", UUID.randomUUID(), "token-de-prueba");

    SimpleMailMessage correo = capturarCorreo(mailSender);
    assertArrayEquals(new String[] {"megamedifire98@gmail.com"}, correo.getTo());
    assertEquals("Enlace para consultar tu solicitud de matrícula", correo.getSubject());
    assertTrue(correo.getText().contains("http://localhost:4200/matricula/inicio?token=token-de-prueba"));
  }

  @Test
  void enviaActualizacionDeEstadoPorSmtp() {
    JavaMailSender mailSender = mock(JavaMailSender.class);
    var adapter = new SeguimientoSolicitudCorreoAdapter(
        mailSender, "https://portal.example.test", "matricula@example.test");
    UUID solicitudId = UUID.randomUUID();
    String token = "test-" + UUID.randomUUID();

    adapter.enviarActualizacionEstado("familia@example.test", solicitudId, token,
        "DOCUMENTOS_VALIDADOS", "Tus documentos fueron aceptados.");

    SimpleMailMessage correo = capturarCorreo(mailSender);
    assertEquals("Actualización de tu solicitud de matrícula", correo.getSubject());
    assertTrue(correo.getText().contains("DOCUMENTOS_VALIDADOS"));
    assertTrue(correo.getText().contains("Tus documentos fueron aceptados."));
    assertTrue(correo.getText().contains(token));
  }

  @Test
  void enviaCredencialesMatriculaPorSmtpSimpleMail() {
    JavaMailSender mailSender = mock(JavaMailSender.class);
    var adapter = new SeguimientoSolicitudCorreoAdapter(
        mailSender, "https://portal.example.test", "matricula@example.test");
    UUID solicitudId = UUID.randomUUID();
    String token = "token-pago-123";

    adapter.enviarCredencialesMatricula(
        "apoderado@example.test",
        "Carlos Mendoza",
        "Mateo Mendoza",
        "Shuji2026!abc123",
        "Primaria · 1° Grado",
        "B001-00001234",
        solicitudId,
        token);

    SimpleMailMessage correo = capturarCorreo(mailSender);
    assertEquals("Pago de matrícula confirmado - I.E.P. Shuji Kitamura", correo.getSubject());
    assertArrayEquals(new String[] {"apoderado@example.test"}, correo.getTo());
    assertTrue(correo.getText().contains("Carlos Mendoza"));
    assertTrue(correo.getText().contains("Mateo Mendoza"));
    assertTrue(correo.getText().contains("Shuji2026!abc123"));
    assertTrue(correo.getText().contains("Primaria · 1° Grado"));
    assertTrue(correo.getText().contains("B001-00001234"));
    assertTrue(correo.getText().contains("https://portal.example.test/auth/login"));
    assertTrue(correo.getText().contains("https://portal.example.test/matricula/inicio?token=" + token + "&id=" + solicitudId));
  }

  @Test
  void enviaCredencialesMatriculaHtmlCuandoMimeMessageDisponible() throws Exception {
    JavaMailSender mailSender = mock(JavaMailSender.class);
    MimeMessage mimeMessage = new MimeMessage((Session) null);
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

    var adapter = new SeguimientoSolicitudCorreoAdapter(
        mailSender, "https://portal.example.test", "matricula@example.test");
    UUID solicitudId = UUID.randomUUID();
    String token = "token-html-456";

    adapter.enviarCredencialesMatricula(
        "apoderado@example.test",
        "Lucia Ramirez",
        "Sofia Ramirez",
        "Shuji2026!xyz789",
        "Secundaria · 2° Año",
        "B001-00005678",
        solicitudId,
        token,
        new byte[] {37, 80, 68, 70},
        "constancia.pdf");

    ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
    verify(mailSender).send(captor.capture());
    MimeMessage enviado = captor.getValue();

    assertEquals("Pago de matrícula confirmado - I.E.P. Shuji Kitamura", enviado.getSubject());
    assertEquals("apoderado@example.test", enviado.getAllRecipients()[0].toString());

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    enviado.writeTo(baos);
    String rawContent = baos.toString();

    assertTrue(rawContent.contains("I.E.P. SHUJI KITAMURA"));
    assertTrue(rawContent.contains("Lucia Ramirez"));
    assertTrue(rawContent.contains("Sofia Ramirez"));
    Assertions.assertFalse(rawContent.contains("Shuji2026!xyz789"));
    assertTrue(rawContent.contains("constancia.pdf"));
    assertTrue(rawContent.contains("Secundaria"));
    assertTrue(rawContent.contains("B001-00005678"));
    assertTrue(rawContent.contains("https://portal.example.test/auth/login"));
    assertTrue(rawContent.contains("PDF adjunto"));
  }

  @Test
  void enviaEnlaceSeguimientoHtmlCuandoMimeMessageDisponible() throws Exception {
    JavaMailSender mailSender = mock(JavaMailSender.class);
    MimeMessage mimeMessage = new MimeMessage((Session) null);
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

    var adapter = new SeguimientoSolicitudCorreoAdapter(
        mailSender, "https://portal.example.test", "matricula@example.test");
    UUID solicitudId = UUID.randomUUID();
    String token = "token-html-link";

    adapter.enviarEnlaceSeguimiento("familia@example.test", solicitudId, token);

    ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
    verify(mailSender).send(captor.capture());
    MimeMessage enviado = captor.getValue();

    assertEquals("Enlace para consultar tu solicitud de matrícula", enviado.getSubject());
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    enviado.writeTo(baos);
    String rawContent = baos.toString();
    assertTrue(rawContent.contains("I.E.P. SHUJI KITAMURA"));
    assertTrue(rawContent.contains(solicitudId.toString()));
    assertTrue(rawContent.contains(token));
  }

  @Test
  void enviaActualizacionEstadoHtmlCuandoMimeMessageDisponible() throws Exception {
    JavaMailSender mailSender = mock(JavaMailSender.class);
    MimeMessage mimeMessage = new MimeMessage((Session) null);
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

    var adapter = new SeguimientoSolicitudCorreoAdapter(
        mailSender, "https://portal.example.test", "matricula@example.test");
    UUID solicitudId = UUID.randomUUID();
    String token = "token-html-estado";

    adapter.enviarActualizacionEstado(
        "familia@example.test",
        solicitudId,
        token,
        "DOCUMENTOS_VALIDADOS",
        "Documentación verificada por IA");

    ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
    verify(mailSender).send(captor.capture());
    MimeMessage enviado = captor.getValue();

    assertEquals("Actualización de tu solicitud de matrícula", enviado.getSubject());
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    enviado.writeTo(baos);
    String rawContent = baos.toString();
    assertTrue(rawContent.contains("I.E.P. SHUJI KITAMURA"));
    assertTrue(rawContent.contains("Documentaci"));
    assertTrue(rawContent.contains("verificada por IA"));
  }

  @Test
  void cuandoFallaSmtpActivaContingenciaSinLanzarExcepcion() {
    JavaMailSender mailSender = mock(JavaMailSender.class);
    org.mockito.Mockito.doThrow(new org.springframework.mail.MailAuthenticationException("535 Authentication failed"))
        .when(mailSender).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
    var adapter = new SeguimientoSolicitudCorreoAdapter(
        mailSender, "http://localhost:4200", "matricula@example.test");

    org.junit.jupiter.api.Assertions.assertDoesNotThrow(() ->
        adapter.enviarEnlaceSeguimiento("familia@example.test", UUID.randomUUID(), "token-123"));
  }

  private SimpleMailMessage capturarCorreo(JavaMailSender mailSender) {
    ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(mailSender).send(captor.capture());
    return captor.getValue();
  }
}
