package com.colegio.shuji.matricula.infrastructure.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.colegio.shuji.academico.infrastructure.entity.AnioLectivoEntity;
import com.colegio.shuji.academico.infrastructure.entity.GradoEntity;
import com.colegio.shuji.academico.infrastructure.entity.NivelEntity;
import com.colegio.shuji.academico.infrastructure.entity.SeccionEntity;
import com.colegio.shuji.academico.infrastructure.repository.JpaAnioLectivoRepository;
import com.colegio.shuji.academico.infrastructure.repository.JpaSeccionRepository;
import com.colegio.shuji.matricula.domain.enums.EstadoSolicitudMatriculaPublica;
import com.colegio.shuji.matricula.domain.enums.Genero;
import com.colegio.shuji.matricula.domain.enums.Parentesco;
import com.colegio.shuji.matricula.infrastructure.entity.SolicitudMatriculaPublicaEntity;
import com.colegio.shuji.matricula.application.port.out.GeminiDocumentoMatriculaPort;
import com.colegio.shuji.matricula.infrastructure.repository.JpaApoderadoRepository;
import com.colegio.shuji.matricula.infrastructure.repository.JpaEstudianteApoderadoRepository;
import com.colegio.shuji.matricula.infrastructure.repository.JpaEstudianteRepository;
import com.colegio.shuji.matricula.infrastructure.repository.JpaMatriculaRepository;
import com.colegio.shuji.matricula.infrastructure.repository.JpaSolicitudMatriculaPublicaRepository;
import com.colegio.shuji.tesoreria.application.port.out.MercadoPagoPort;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class SolicitudMatriculaPublicaDocumentosTest {
  private static final UUID SOLICITUD_ID = UUID.fromString("b4a3a712-6f89-4d48-80c1-ec1d5b37cf22");
  private static final String TOKEN = "token-privado-de-prueba";

  @Mock private JpaSolicitudMatriculaPublicaRepository solicitudes;
  @Mock private JpaAnioLectivoRepository anios;
  @Mock private JpaSeccionRepository secciones;
  @Mock private JpaEstudianteRepository estudiantes;
  @Mock private JpaApoderadoRepository apoderados;
  @Mock private JpaEstudianteApoderadoRepository vinculos;
  @Mock private JpaMatriculaRepository matriculas;
  @Mock private GeminiDocumentoMatriculaPort gemini;
  @Mock private MercadoPagoPort mercadoPago;

  @InjectMocks private SolicitudMatriculaPublicaService service;

  @Test
  void generaComprobanteInternoAlConfirmarseElPagoYEscapaDatos() throws Exception {
    var solicitud = solicitudConfirmada();
    solicitud.setNombresEstudiante("Ana <Lopez>");
    when(solicitudes.findById(SOLICITUD_ID)).thenReturn(Optional.of(solicitud));

    String html = service.generarComprobanteHtml(SOLICITUD_ID, TOKEN);

    assertTrue(html.contains("REC-MAT-42"));
    assertTrue(html.contains("Ana &lt;Lopez&gt;"));
    assertTrue(html.contains("S/ 1.00 PEN"));
    assertTrue(html.contains("No es una boleta electrónica"));
    assertFalse(html.contains("Ana <Lopez>"));
  }

  @Test
  void generaFichaConDatosAcademicosYDelApoderado() throws Exception {
    when(solicitudes.findById(SOLICITUD_ID)).thenReturn(Optional.of(solicitudConfirmada()));
    var anio = new AnioLectivoEntity();
    anio.setAnio((short) 2026);
    when(anios.findById((short) 2026)).thenReturn(Optional.of(anio));
    var nivel = new NivelEntity();
    nivel.setNombre("Primaria");
    var grado = new GradoEntity();
    grado.setNombre("Segundo grado");
    grado.setRelacion0(nivel);
    var seccion = new SeccionEntity();
    seccion.setLetra("A");
    seccion.setRelacion1(grado);
    when(secciones.findById(9)).thenReturn(Optional.of(seccion));

    String html = service.generarFichaMatriculaHtml(SOLICITUD_ID, TOKEN);

    assertTrue(html.contains("Ficha de matrícula"));
    assertTrue(html.contains("Primaria · Segundo grado A"));
    assertTrue(html.contains("2026"));
    assertTrue(html.contains("Apoderado Uno Apellido Materno"));
  }

  @Test
  void noPermiteEmitirDocumentosAntesDeConfirmarLaMatricula() throws Exception {
    var solicitud = solicitudConfirmada();
    solicitud.setEstado(EstadoSolicitudMatriculaPublica.PAGO_PENDIENTE);
    when(solicitudes.findById(SOLICITUD_ID)).thenReturn(Optional.of(solicitud));

    var error = assertThrows(
        ResponseStatusException.class,
        () -> service.generarComprobanteHtml(SOLICITUD_ID, TOKEN));

    assertEquals(409, error.getStatusCode().value());
  }

  private SolicitudMatriculaPublicaEntity solicitudConfirmada() throws Exception {
    var solicitud = new SolicitudMatriculaPublicaEntity();
    solicitud.setTokenHash(hash(TOKEN));
    solicitud.setEstado(EstadoSolicitudMatriculaPublica.MATRICULADA);
    solicitud.setMatriculaId(42L);
    solicitud.setPagoId("987654321");
    solicitud.setComprobantePagoCodigo("REC-MAT-42");
    solicitud.setDocumentosEmitidosAt(OffsetDateTime.parse("2026-09-23T18:30:00Z"));
    solicitud.setPagoMonto(new BigDecimal("1.00"));
    solicitud.setNombresEstudiante("Ana");
    solicitud.setApellidoPaternoEstudiante("Lopez");
    solicitud.setApellidoMaternoEstudiante("Diaz");
    solicitud.setNumeroDocumentoEstudiante("12345678");
    solicitud.setAnioLectivoId((short) 2026);
    solicitud.setSeccionId(9);
    solicitud.setFechaNacimientoEstudiante(java.time.LocalDate.of(2015, 3, 17));
    solicitud.setGeneroEstudiante(Genero.F);
    solicitud.setNombresApoderado("Apoderado");
    solicitud.setApellidoPaternoApoderado("Uno");
    solicitud.setApellidoMaternoApoderado("Apellido Materno");
    solicitud.setNumeroDocumentoApoderado("87654321");
    solicitud.setParentesco(Parentesco.MADRE);
    solicitud.setCelularApoderado("999111222");
    solicitud.setEmailApoderado("familia@example.com");
    solicitud.setDireccionApoderado("Calle de prueba 123");
    return solicitud;
  }

  private String hash(String token) throws Exception {
    byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
    return HexFormat.of().formatHex(digest);
  }
}
