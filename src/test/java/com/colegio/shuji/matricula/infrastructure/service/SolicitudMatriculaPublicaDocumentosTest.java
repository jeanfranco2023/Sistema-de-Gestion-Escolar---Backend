package com.colegio.shuji.matricula.infrastructure.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
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
import com.colegio.shuji.tesoreria.application.port.out.VerificarPagoPort.PagoVerificado;
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
  @Mock private com.colegio.shuji.matricula.infrastructure.repository.JpaSolicitudMatriculaDocumentoRepository documentos;
  @Mock private com.colegio.shuji.matricula.infrastructure.repository.JpaDocumentoMatriculaVersionRepository versiones;
  @Mock private JpaAnioLectivoRepository anios;
  @Mock private JpaSeccionRepository secciones;
  @Mock private JpaEstudianteRepository estudiantes;
  @Mock private JpaApoderadoRepository apoderados;
  @Mock private JpaEstudianteApoderadoRepository vinculos;
  @Mock private JpaMatriculaRepository matriculas;
  @Mock private GeminiDocumentoMatriculaPort gemini;
  @Mock private MercadoPagoPort mercadoPago;
  @Mock private com.colegio.shuji.tesoreria.application.port.in.RegistrarCobroMatriculaUseCase cobrosMatricula;

  @InjectMocks private SolicitudMatriculaPublicaService service;

  @Test
  void edadFueraDeRangoQuedaPendienteDeAutorizacionSinIniciarPago() {
    prepararAdmision();
    when(solicitudes.saveAndFlush(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    var request = solicitudPublicaConNacimiento(java.time.LocalDate.of(2020, 4, 1));
    var respuesta = service.crear(request);
    assertEquals(EstadoSolicitudMatriculaPublica.AUTORIZACION_EDAD_PENDIENTE, respuesta.estado());
    verify(secciones).bloquearPorId(9);
    verify(solicitudes).saveAndFlush(org.mockito.ArgumentMatchers.any());
    org.mockito.Mockito.verifyNoInteractions(mercadoPago, cobrosMatricula);
  }

  @Test
  void colisionDeInsercionDevuelveConflictoSinConsultarLaTransaccionAbortada() {
    prepararAdmision();
    when(solicitudes.saveAndFlush(org.mockito.ArgumentMatchers.any()))
        .thenThrow(new org.springframework.dao.DataIntegrityViolationException("Colisión concurrente sintética"));
    var request = solicitudPublicaConNacimiento(java.time.LocalDate.of(2020, 3, 31));
    var error = assertThrows(ResponseStatusException.class, () -> service.crear(request));
    assertEquals(409, error.getStatusCode().value());
    var orden = org.mockito.Mockito.inOrder(solicitudes);
    orden.verify(solicitudes).existsByNumeroDocumentoEstudianteAndEstadoIn(
        org.mockito.ArgumentMatchers.eq("12345678"), org.mockito.ArgumentMatchers.anyList());
    orden.verify(solicitudes).saveAndFlush(org.mockito.ArgumentMatchers.any());
    orden.verifyNoMoreInteractions();
    org.mockito.Mockito.verifyNoInteractions(mercadoPago);
  }

  private void prepararAdmision() {
    var anio = new AnioLectivoEntity();
    anio.setAnio((short) 2026); anio.setAbierto(true);
    when(anios.findById((short) 2026)).thenReturn(Optional.of(anio));
    var nivel = new NivelEntity();
    nivel.setCodigo(com.colegio.shuji.academico.domain.enums.NivelCodigo.PRIMARIA);
    var grado = new GradoEntity(); grado.setNumeroGrado((short) 1); grado.setRelacion0(nivel);
    var seccion = new SeccionEntity();
    seccion.setAnioLectivoId((short) 2026); seccion.setRelacion1(grado);
    seccion.setCupoMaximo((short) 20); seccion.setVacantesOcupadas((short) 0);
    when(secciones.bloquearPorId(9)).thenReturn(Optional.of(seccion));
  }

  private com.colegio.shuji.matricula.application.dto.in.CrearSolicitudMatriculaPublicaRequestDto solicitudPublicaConNacimiento(
      java.time.LocalDate nacimiento) {
    return new com.colegio.shuji.matricula.application.dto.in.CrearSolicitudMatriculaPublicaRequestDto(
        (short) 2026, 9, "12345678", "Ana", "Perez", "Ruiz", nacimiento, Genero.F,
        "87654321", "Rosa", "Perez", "Ruiz", "999888777", "familia@example.test", "Lima", "150101", Parentesco.MADRE, true);
  }

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


  @Test
  void creaPreferenciaDe350YDevuelveElEnlaceValidadoPorElAdaptador() throws Exception {
    var solicitud = new SolicitudMatriculaPublicaEntity();
    solicitud.setId(SOLICITUD_ID);
    solicitud.setTokenHash(hash(TOKEN));
    solicitud.setEstado(EstadoSolicitudMatriculaPublica.DOCUMENTOS_VALIDADOS);
    solicitud.setEstadoPartida(com.colegio.shuji.matricula.domain.enums.EstadoValidacionDocumento.VALIDADO);
    solicitud.setEstadoDniC4(com.colegio.shuji.matricula.domain.enums.EstadoValidacionDocumento.VALIDADO);
    solicitud.setEstadoRecibo(com.colegio.shuji.matricula.domain.enums.EstadoValidacionDocumento.VALIDADO);
    solicitud.setSeccionId(9);
    prepararEvidenciaCompleta(solicitud);
    when(solicitudes.bloquearPorId(SOLICITUD_ID)).thenReturn(Optional.of(solicitud));
    when(secciones.reservarVacante(9)).thenReturn(1);
    when(mercadoPago.crearPreferenciaMatriculaPublica(
        SolicitudMatriculaPublicaService.REFERENCIA_PAGO_PREFIX + SOLICITUD_ID,
        new BigDecimal("350.00"),
        "Derecho de matrícula escolar"))
        .thenReturn(new com.colegio.shuji.tesoreria.application.dto.out.PreferenciaMercadoPagoResponseDto(
            "pref-350", "https://checkout.example/normal", "https://checkout.example/sandbox", "TEST-key"));

    var respuesta = service.crearPago(SOLICITUD_ID, TOKEN);

    assertEquals(new BigDecimal("350.00"), respuesta.monto());
    assertEquals("https://checkout.example/normal", respuesta.enlacePago());
    verify(mercadoPago).crearPreferenciaMatriculaPublica(
        SolicitudMatriculaPublicaService.REFERENCIA_PAGO_PREFIX + SOLICITUD_ID,
        new BigDecimal("350.00"),
        "Derecho de matrícula escolar");
  }
  @Test
  void aceptaMontoDeMatriculaDe350YContinuaValidandoLaVacante() {
    var solicitud = new SolicitudMatriculaPublicaEntity();
    solicitud.setPagoMonto(new BigDecimal("350.00"));
    solicitud.setId(SOLICITUD_ID);
    solicitud.setEstado(EstadoSolicitudMatriculaPublica.PAGO_PENDIENTE);
    solicitud.setEstadoPartida(com.colegio.shuji.matricula.domain.enums.EstadoValidacionDocumento.VALIDADO);
    solicitud.setEstadoDniC4(com.colegio.shuji.matricula.domain.enums.EstadoValidacionDocumento.VALIDADO);
    solicitud.setEstadoRecibo(com.colegio.shuji.matricula.domain.enums.EstadoValidacionDocumento.VALIDADO);
    solicitud.setSeccionId(9);
    solicitud.setAnioLectivoId((short) 2026);
    prepararEvidenciaCompleta(solicitud);
    when(solicitudes.bloquearPorId(SOLICITUD_ID)).thenReturn(Optional.of(solicitud));
    when(secciones.reservarVacante(9)).thenReturn(0);
    var pago = new PagoVerificado(
        "pago-test-350", null, new BigDecimal("350.00"), "PEN", null, true,
        SolicitudMatriculaPublicaService.REFERENCIA_PAGO_PREFIX + SOLICITUD_ID);

    var error = assertThrows(
        ResponseStatusException.class,
        () -> service.finalizarPagoVerificado(SOLICITUD_ID, null, pago));

    assertEquals(409, error.getStatusCode().value());
    assertTrue(error.getReason().contains("Pago aprobado, pero la vacante"));
  }
  private void prepararEvidenciaCompleta(SolicitudMatriculaPublicaEntity solicitud) {
    solicitud.setParentesco(Parentesco.MADRE);
    var archivos = new java.util.ArrayList<com.colegio.shuji.matricula.infrastructure.entity.SolicitudMatriculaDocumentoEntity>();
    var evidencias = new java.util.ArrayList<com.colegio.shuji.matricula.infrastructure.entity.DocumentoMatriculaVersionEntity>();
    for (var tipo : com.colegio.shuji.matricula.domain.enums.TipoDocumentoSolicitud.values()) {
      var version = new com.colegio.shuji.matricula.infrastructure.entity.DocumentoMatriculaVersionEntity();
      version.setId(UUID.randomUUID()); version.setMimeType("application/pdf"); version.setIaValida(true);
      version.setDniApiCoincide(true); version.setVinculoParentalCoincide(tipo == com.colegio.shuji.matricula.domain.enums.TipoDocumentoSolicitud.PARTIDA_NACIMIENTO ? true : null);
      version.setEstadoFirma(com.colegio.shuji.matricula.domain.enums.EstadoFirmaDigital.VALIDA);
      var archivo = new com.colegio.shuji.matricula.infrastructure.entity.SolicitudMatriculaDocumentoEntity();
      archivo.setId(new com.colegio.shuji.matricula.infrastructure.entity.SolicitudMatriculaDocumentoId(SOLICITUD_ID,tipo));
      archivo.setVersionId(version.getId()); archivos.add(archivo); evidencias.add(version);
    }
    when(documentos.findByIdSolicitudIdIn(java.util.List.of(SOLICITUD_ID))).thenReturn(archivos);
    when(versiones.findAllById(org.mockito.ArgumentMatchers.any())).thenReturn(evidencias);
  }

  private SolicitudMatriculaPublicaEntity solicitudConfirmada() throws Exception {
    var solicitud = new SolicitudMatriculaPublicaEntity();
    solicitud.setTokenHash(hash(TOKEN));
    solicitud.setEstado(EstadoSolicitudMatriculaPublica.MATRICULADA);
    solicitud.setMatriculaId(42L);
    solicitud.setPagoId("987654321");
    solicitud.setComprobantePagoCodigo("REC-MAT-42");
    solicitud.setFechaEmisionDocumentos(OffsetDateTime.parse("2026-09-23T18:30:00Z"));
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
