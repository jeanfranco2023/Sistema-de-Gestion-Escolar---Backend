package com.colegio.shuji.matricula.infrastructure.adapter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
    Assertions.assertEquals("${SMTP_PORT:587}", propiedades.getProperty("spring.mail.port"));
    Assertions.assertEquals("${SMTP_USERNAME:}", propiedades.getProperty("spring.mail.username"));
    Assertions.assertEquals("${SMTP_PASSWORD:}", propiedades.getProperty("spring.mail.password"));
    Assertions.assertEquals("${SMTP_FROM:}", propiedades.getProperty("app.mail.from"));

    Assertions.assertFalse(new YamlPropertySourceLoader()
        .load("render", new FileSystemResource("render.yaml")).isEmpty());
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

  private SimpleMailMessage capturarCorreo(JavaMailSender mailSender) {
    ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(mailSender).send(captor.capture());
    return captor.getValue();
  }
}
