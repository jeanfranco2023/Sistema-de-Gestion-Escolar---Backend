package com.colegio.shuji.matricula.infrastructure.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import com.colegio.shuji.matricula.domain.enums.TipoDocumentoSolicitud;
import com.colegio.shuji.matricula.domain.enums.EstadoFirmaDigital;
import com.colegio.shuji.matricula.application.port.out.FirmaDigitalDocumentoPort;
import com.colegio.shuji.matricula.infrastructure.entity.ApoderadoEntity;
import com.colegio.shuji.matricula.infrastructure.entity.EstudianteEntity;
import com.colegio.shuji.matricula.infrastructure.entity.MatriculaEntity;
import com.colegio.shuji.matricula.infrastructure.entity.SolicitudMatriculaPublicaEntity;
import com.colegio.shuji.tesoreria.domain.enums.MetodoPago;
import com.colegio.shuji.matricula.application.port.out.GeminiDocumentoMatriculaPort;
import com.colegio.shuji.matricula.application.port.out.CuentaApoderadoPort;
import com.colegio.shuji.matricula.application.port.out.SeguimientoSolicitudCorreoPort;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
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
  @Mock private SeguimientoSolicitudCorreoPort correoSeguimiento;
  @Mock private CuentaApoderadoPort cuentaApoderadoPort;
  @Mock private MercadoPagoPort mercadoPago;
  @Mock private com.colegio.shuji.tesoreria.application.port.in.RegistrarCobroMatriculaUseCase cobrosMatricula;

  @InjectMocks private SolicitudMatriculaPublicaService service;

  @BeforeEach
  void inyectaElAdaptadorOpcionalDeCorreo() {
    ReflectionTestUtils.setField(service, "correoSeguimiento", correoSeguimiento);
    ReflectionTestUtils.setField(service, "cuentaApoderadoPort", cuentaApoderadoPort);
    ReflectionTestUtils.setField(service, "comprobantePdfAdapter",
        new com.colegio.shuji.tesoreria.infrastructure.adapter.ComprobanteMatriculaPdfAdapter());
    ReflectionTestUtils.setField(service, "montoCobroMatriculaPublica", new BigDecimal("350.00"));
  }

  @Test
  void edadFueraDeRangoQuedaPendienteDeAutorizacionSinIniciarPago() {
    prepararAdmision();
    ReflectionTestUtils.setField(service, "validarEdad", true);
    when(solicitudes.saveAndFlush(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    var request = solicitudPublicaConNacimiento(java.time.LocalDate.of(2021, 4, 1));
    var respuesta = service.crear(request);
    assertEquals(EstadoSolicitudMatriculaPublica.AUTORIZACION_EDAD_PENDIENTE, respuesta.estado());
    org.mockito.Mockito.verify(correoSeguimiento).enviarEnlaceSeguimiento(
        org.mockito.ArgumentMatchers.eq("familia@example.test"),
        org.mockito.ArgumentMatchers.any(UUID.class),
        org.mockito.ArgumentMatchers.anyString());
    verify(secciones).buscarPorAnioLectivoId((short) 2027);
    verify(solicitudes).saveAndFlush(org.mockito.ArgumentMatchers.any());
    org.mockito.Mockito.verifyNoInteractions(mercadoPago, cobrosMatricula);
  }

  @Test
  void devuelveEnlaceDeSeguimientoSiFallaElCorreoInicial() {
    prepararAdmision();
    when(solicitudes.saveAndFlush(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    org.mockito.Mockito.doThrow(new IllegalStateException("smtp no disponible"))
        .when(correoSeguimiento).enviarEnlaceSeguimiento(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any(UUID.class),
            org.mockito.ArgumentMatchers.anyString());

    var respuesta = service.crear(solicitudPublicaConNacimiento(java.time.LocalDate.of(2020, 4, 1)));

    assertNotNull(respuesta.id());
    assertNotNull(respuesta.token());
    assertEquals(EstadoSolicitudMatriculaPublica.DOCUMENTOS_PENDIENTES, respuesta.estado());
  }

  @Test
  void permiteContinuarConEdadFueraDeRangoCuandoLaValidacionEstaDesactivada() {
    prepararAdmision();
    ReflectionTestUtils.setField(service, "validarEdad", false);
    var captor = org.mockito.ArgumentCaptor.forClass(SolicitudMatriculaPublicaEntity.class);
    when(solicitudes.saveAndFlush(captor.capture()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var respuesta = service.crear(solicitudPublicaConNacimiento(java.time.LocalDate.of(2020, 4, 1)));

    assertEquals(EstadoSolicitudMatriculaPublica.DOCUMENTOS_PENDIENTES, respuesta.estado());
    assertFalse(captor.getValue().isRequiereAutorizacionEdad());
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

  @Test
  void creacionConUbigeoNuloUOmisibleGuardaCorrectamente() {
    prepararAdmision();
    org.mockito.ArgumentCaptor<SolicitudMatriculaPublicaEntity> captor =
        org.mockito.ArgumentCaptor.forClass(SolicitudMatriculaPublicaEntity.class);
    when(solicitudes.saveAndFlush(captor.capture()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var request = new com.colegio.shuji.matricula.application.dto.in.CrearSolicitudMatriculaPublicaRequestDto(
        (short) 2027, (short) 1, "12345678", "Ana", "Perez", "Ruiz",
        java.time.LocalDate.of(2020, 3, 31), Genero.F,
        "87654321", "Rosa", "Perez", "Ruiz", "999888777", "familia@example.test",
        "Lima", null, Parentesco.MADRE, true);

    service.crear(request);
    assertNull(captor.getValue().getUbigeoApoderado());
    assertEquals("Ana", captor.getValue().getNombresEstudiante());
  }

  @Test
  void conservaTokenAnteriorSiNoPuedeEnviarLaActualizacion() {
    var solicitud = new SolicitudMatriculaPublicaEntity();
    solicitud.setId(SOLICITUD_ID);
    solicitud.setTokenHash("hash-anterior");
    solicitud.setEmailApoderado("familia@example.test");
    solicitud.setEstado(EstadoSolicitudMatriculaPublica.DOCUMENTOS_OBSERVADOS);
    org.mockito.Mockito.doThrow(new IllegalStateException("smtp no disponible"))
        .when(correoSeguimiento).enviarActualizacionEstado(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.eq(SOLICITUD_ID),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString());

    var nuevoToken = ReflectionTestUtils.invokeMethod(
        service, "rotarTokenYNotificar", solicitud, "actualización de prueba");
    assertNull(nuevoToken);
    assertEquals("hash-anterior", solicitud.getTokenHash());
  }

  @Test
  void permiteValidarDocumentosCuandoEstadoEsAutorizacionEdadPendiente() throws Exception {
    var solicitud = new SolicitudMatriculaPublicaEntity();
    solicitud.setId(SOLICITUD_ID);
    solicitud.setTokenHash(hash(TOKEN));
    solicitud.setEstado(EstadoSolicitudMatriculaPublica.AUTORIZACION_EDAD_PENDIENTE);
    solicitud.setRequiereAutorizacionEdad(true);
    solicitud.setParentesco(Parentesco.MADRE);
    solicitud.setConsentimientoGemini(true);
    solicitud.setNumeroDocumentoEstudiante("12345678");
    solicitud.setNombresEstudiante("Ana");
    solicitud.setApellidoPaternoEstudiante("Perez");
    solicitud.setApellidoMaternoEstudiante("Ruiz");
    solicitud.setNumeroDocumentoApoderado("87654321");
    solicitud.setNombresApoderado("Rosa");
    solicitud.setApellidoPaternoApoderado("Perez");
    solicitud.setApellidoMaternoApoderado("Ruiz");

    when(solicitudes.findById(SOLICITUD_ID)).thenReturn(Optional.of(solicitud));
    when(solicitudes.bloquearPorId(SOLICITUD_ID)).thenReturn(Optional.of(solicitud));
    when(solicitudes.saveAndFlush(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));

    var prep = service.prepararValidacion(SOLICITUD_ID, TOKEN, TipoDocumentoSolicitud.PARTIDA_NACIMIENTO);
    assertNotNull(prep);
    assertEquals("12345678", prep.dni());

    var resGemini = new GeminiDocumentoMatriculaPort.Resultado(true, true, true, "OK", true, true);
    var resFirma = new FirmaDigitalDocumentoPort.Resultado(EstadoFirmaDigital.VALIDA, "Firma verificada", "certSha256");

    var resp = service.guardarResultadoDocumento(SOLICITUD_ID, TOKEN, TipoDocumentoSolicitud.PARTIDA_NACIMIENTO,
        resGemini, resFirma, true, null, "bucket", "key", "application/pdf", 1024L, "sha256");
    assertNotNull(resp);
  }

  private void prepararAdmision() {
    var anio = new AnioLectivoEntity();
    anio.setAnio((short) 2027); anio.setAbierto(true);
    when(anios.findById((short) 2027)).thenReturn(Optional.of(anio));
    var nivel = new NivelEntity();
    nivel.setCodigo(com.colegio.shuji.academico.domain.enums.NivelCodigo.PRIMARIA);
    var grado = new GradoEntity(); grado.setId((short) 1); grado.setNumeroGrado((short) 1); grado.setRelacion0(nivel);
    var seccion = new SeccionEntity();
    seccion.setAnioLectivoId((short) 2027); seccion.setGradoId((short) 1); seccion.setRelacion1(grado);
    seccion.setCupoMaximo((short) 20); seccion.setVacantesOcupadas((short) 0);
    when(secciones.buscarPorAnioLectivoId((short) 2027)).thenReturn(java.util.List.of(seccion));
  }

  private com.colegio.shuji.matricula.application.dto.in.CrearSolicitudMatriculaPublicaRequestDto solicitudPublicaConNacimiento(
      java.time.LocalDate nacimiento) {
    return new com.colegio.shuji.matricula.application.dto.in.CrearSolicitudMatriculaPublicaRequestDto(
        (short) 2027, (short) 1, "12345678", "Ana", "Perez", "Ruiz", nacimiento, Genero.F,
        "87654321", "Rosa", "Perez", "Ruiz", "999888777", "familia@example.test", "Lima", "150101", Parentesco.MADRE, true);
  }

  @Test
  void generaComprobanteInternoAlConfirmarseElPagoYEscapaDatos() throws Exception {
    var solicitud = solicitudConfirmada();
    solicitud.setPagoMonto(new BigDecimal("350.00"));
    solicitud.setNombresEstudiante("Ana <Lopez>");
    ReflectionTestUtils.setField(service, "montoCobroMatriculaPublica", new BigDecimal("1.00"));
    when(solicitudes.findById(SOLICITUD_ID)).thenReturn(Optional.of(solicitud));

    String html = service.generarComprobanteHtml(SOLICITUD_ID, TOKEN);

    assertTrue(html.contains("REC-MAT-42"));
    assertTrue(html.contains("Ana &lt;Lopez&gt;"));
    assertTrue(html.contains("S/ 1.00 PEN"));
    assertTrue(html.contains("S/ 350.00 PEN"));
    assertTrue(html.contains("No es una boleta electrónica"));
    assertFalse(html.contains("Ana <Lopez>"));
  }

  @Test
  void generaFichaConDatosAcademicosYDelApoderado() throws Exception {
    when(solicitudes.findById(SOLICITUD_ID)).thenReturn(Optional.of(solicitudConfirmada()));
    var anio = new AnioLectivoEntity();
    anio.setAnio((short) 2027);
    when(anios.findById((short) 2027)).thenReturn(Optional.of(anio));
    var nivel = new NivelEntity();
    nivel.setNombre("Primaria");
    var grado = new GradoEntity();
    grado.setNombre("Segundo grado");
    grado.setRelacion0(nivel);
    var seccion = new SeccionEntity();
    seccion.setLetra("A");
    seccion.setRelacion1(grado);
    seccion.setGradoId((short) 1);
    when(secciones.buscarPorAnioLectivoId((short) 2027)).thenReturn(java.util.List.of(seccion));

    String html = service.generarFichaMatriculaHtml(SOLICITUD_ID, TOKEN);

    assertTrue(html.contains("Ficha de matrícula"));
    assertTrue(html.contains("Primaria · Segundo grado · sección y aula pendientes"));
    assertTrue(html.contains("2027"));
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
    solicitud.setGradoId((short) 1);
    prepararEvidenciaCompleta(solicitud);
    solicitud.setEstadoPartida(com.colegio.shuji.matricula.domain.enums.EstadoValidacionDocumento.VALIDADO);
    solicitud.setEstadoDniC4(com.colegio.shuji.matricula.domain.enums.EstadoValidacionDocumento.VALIDADO);
    solicitud.setEstadoRecibo(com.colegio.shuji.matricula.domain.enums.EstadoValidacionDocumento.VALIDADO);
    ReflectionTestUtils.setField(service, "montoCobroMatriculaPublica", new BigDecimal("1.00"));
    when(solicitudes.bloquearPorId(SOLICITUD_ID)).thenReturn(Optional.of(solicitud));
    when(mercadoPago.crearPreferenciaMatriculaPublica(
        SolicitudMatriculaPublicaService.REFERENCIA_PAGO_PREFIX + SOLICITUD_ID,
        new BigDecimal("1.00"),
        "Derecho de matrícula escolar"))
        .thenReturn(new com.colegio.shuji.tesoreria.application.dto.out.PreferenciaMercadoPagoResponseDto(
            "pref-350", "https://checkout.example/normal", "https://checkout.example/sandbox", "TEST-key"));

    var respuesta = service.crearPago(SOLICITUD_ID, TOKEN);

    assertEquals(new BigDecimal("350.00"), respuesta.monto());
    assertEquals("https://checkout.example/normal", respuesta.enlacePago());
    assertNull(solicitud.getSeccionId());
    org.mockito.Mockito.verify(secciones, org.mockito.Mockito.never()).reservarVacante(org.mockito.ArgumentMatchers.any());
    verify(mercadoPago).crearPreferenciaMatriculaPublica(
        SolicitudMatriculaPublicaService.REFERENCIA_PAGO_PREFIX + SOLICITUD_ID,
        new BigDecimal("1.00"),
        "Derecho de matrícula escolar");
    assertEquals(new BigDecimal("350.00"), solicitud.getPagoMonto());
  }
  @Test
  void preparaPagoDe350SinReservarVacante() throws Exception {
    var solicitud = new SolicitudMatriculaPublicaEntity();
    solicitud.setPagoMonto(new BigDecimal("350.00"));
    solicitud.setId(SOLICITUD_ID);
    solicitud.setTokenHash(hash(TOKEN));
    solicitud.setEstado(EstadoSolicitudMatriculaPublica.PAGO_PENDIENTE);
    solicitud.setEstadoPartida(com.colegio.shuji.matricula.domain.enums.EstadoValidacionDocumento.VALIDADO);
    solicitud.setEstadoDniC4(com.colegio.shuji.matricula.domain.enums.EstadoValidacionDocumento.VALIDADO);
    solicitud.setEstadoRecibo(com.colegio.shuji.matricula.domain.enums.EstadoValidacionDocumento.VALIDADO);
    solicitud.setGradoId((short) 1);
    solicitud.setAnioLectivoId((short) 2027);
    prepararEvidenciaCompleta(solicitud);
    solicitud.setEstadoPartida(com.colegio.shuji.matricula.domain.enums.EstadoValidacionDocumento.VALIDADO);
    solicitud.setEstadoDniC4(com.colegio.shuji.matricula.domain.enums.EstadoValidacionDocumento.VALIDADO);
    solicitud.setEstadoRecibo(com.colegio.shuji.matricula.domain.enums.EstadoValidacionDocumento.VALIDADO);
    when(solicitudes.bloquearPorId(SOLICITUD_ID)).thenReturn(Optional.of(solicitud));
    when(mercadoPago.crearPreferenciaMatriculaPublica(
        SolicitudMatriculaPublicaService.REFERENCIA_PAGO_PREFIX + SOLICITUD_ID,
        new BigDecimal("350.00"), "Derecho de matrícula escolar"))
        .thenReturn(new com.colegio.shuji.tesoreria.application.dto.out.PreferenciaMercadoPagoResponseDto(
            "pref-350", "https://checkout.example/normal", "https://checkout.example/sandbox", "TEST-key"));

    var pago = service.crearPago(SOLICITUD_ID, TOKEN);

    assertEquals(new BigDecimal("350.00"), pago.monto());
    assertFalse(solicitud.isVacanteReservada());
    assertNull(solicitud.getSeccionId());
    org.mockito.Mockito.verify(secciones, org.mockito.Mockito.never()).reservarVacante(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void asignaSeccionDespuesDelPagoYCompletaLosEstados() throws Exception {
    var solicitud = solicitudConfirmada();
    solicitud.setMatriculaId(42L);
    var matricula = new com.colegio.shuji.matricula.infrastructure.entity.MatriculaEntity();
    matricula.setId(42L);
    matricula.setEstudianteId(101L);
    matricula.setAnioLectivoId((short) 2027);
    matricula.setEstadoMatricula(com.colegio.shuji.matricula.domain.enums.EstadoMatricula.PENDIENTE_ASIGNACION);
    var seccion = new SeccionEntity();
    seccion.setId(9);
    seccion.setAnioLectivoId((short) 2027);
    seccion.setGradoId((short) 1);
    seccion.setVacantesOcupadas((short) 29);
    seccion.setCupoMaximo((short) 30);
    when(solicitudes.bloquearPorId(SOLICITUD_ID)).thenReturn(Optional.of(solicitud));
    when(matriculas.bloquearPorId(42L)).thenReturn(Optional.of(matricula));
    when(secciones.bloquearPorId(9)).thenReturn(Optional.of(seccion));
    when(matriculas.saveAndFlush(matricula)).thenReturn(matricula);
    when(solicitudes.saveAndFlush(solicitud)).thenReturn(solicitud);
    when(documentos.findByIdSolicitudIdIn(java.util.List.of(SOLICITUD_ID))).thenReturn(java.util.List.of());

    var resultado = service.asignarSeccion(SOLICITUD_ID, 9);

    assertEquals(EstadoSolicitudMatriculaPublica.MATRICULADA, resultado.estado());
    assertEquals(9, resultado.seccionId());
    assertEquals(com.colegio.shuji.matricula.domain.enums.EstadoMatricula.MATRICULADO,
        matricula.getEstadoMatricula());
    assertEquals(9, matricula.getSeccionId());
  }

  @Test
  void noAsignaSeccionSiMatriculaVinculadaPerteneceAOtroEstudiante() throws Exception {
    assertVinculoMatriculaInconsistenteRechazado(999L, (short) 2027);
  }

  @Test
  void noAsignaSeccionSiMatriculaVinculadaPerteneceAOtroAnio() throws Exception {
    assertVinculoMatriculaInconsistenteRechazado(101L, (short) 2025);
  }

  private void assertVinculoMatriculaInconsistenteRechazado(Long estudianteId, short anioLectivoId)
      throws Exception {
    var solicitud = solicitudConfirmada();
    solicitud.setEstudianteId(101L);
    solicitud.setMatriculaId(42L);
    var matricula = new com.colegio.shuji.matricula.infrastructure.entity.MatriculaEntity();
    matricula.setId(42L);
    matricula.setEstudianteId(estudianteId);
    matricula.setAnioLectivoId(anioLectivoId);
    matricula.setEstadoMatricula(com.colegio.shuji.matricula.domain.enums.EstadoMatricula.PENDIENTE_ASIGNACION);
    when(solicitudes.bloquearPorId(SOLICITUD_ID)).thenReturn(Optional.of(solicitud));
    when(matriculas.bloquearPorId(42L)).thenReturn(Optional.of(matricula));

    var error = assertThrows(
        ResponseStatusException.class, () -> service.asignarSeccion(SOLICITUD_ID, 9));

    assertEquals(409, error.getStatusCode().value());
    assertEquals(EstadoSolicitudMatriculaPublica.PAGO_CONFIRMADO_PENDIENTE_ASIGNACION, solicitud.getEstado());
    assertEquals(com.colegio.shuji.matricula.domain.enums.EstadoMatricula.PENDIENTE_ASIGNACION,
        matricula.getEstadoMatricula());
    assertNull(matricula.getSeccionId());
    org.mockito.Mockito.verify(secciones, org.mockito.Mockito.never()).bloquearPorId(9);
    org.mockito.Mockito.verify(matriculas, org.mockito.Mockito.never()).saveAndFlush(matricula);
    org.mockito.Mockito.verify(solicitudes, org.mockito.Mockito.never()).saveAndFlush(solicitud);
  }

  @Test
  void noAsignaUnaSeccionQueYaAlcanzoElCupo() throws Exception {
    var solicitud = solicitudConfirmada();
    solicitud.setMatriculaId(42L);
    var matricula = new com.colegio.shuji.matricula.infrastructure.entity.MatriculaEntity();
    matricula.setId(42L);
    matricula.setEstudianteId(101L);
    matricula.setAnioLectivoId((short) 2027);
    matricula.setEstadoMatricula(com.colegio.shuji.matricula.domain.enums.EstadoMatricula.PENDIENTE_ASIGNACION);
    var seccion = new SeccionEntity();
    seccion.setId(9);
    seccion.setAnioLectivoId((short) 2027);
    seccion.setGradoId((short) 1);
    seccion.setVacantesOcupadas((short) 30);
    seccion.setCupoMaximo((short) 30);
    when(solicitudes.bloquearPorId(SOLICITUD_ID)).thenReturn(Optional.of(solicitud));
    when(matriculas.bloquearPorId(42L)).thenReturn(Optional.of(matricula));
    when(secciones.bloquearPorId(9)).thenReturn(Optional.of(seccion));

    var error = assertThrows(ResponseStatusException.class, () -> service.asignarSeccion(SOLICITUD_ID, 9));

    assertEquals(409, error.getStatusCode().value());
    assertEquals(EstadoSolicitudMatriculaPublica.PAGO_CONFIRMADO_PENDIENTE_ASIGNACION, solicitud.getEstado());
    assertEquals(com.colegio.shuji.matricula.domain.enums.EstadoMatricula.PENDIENTE_ASIGNACION,
        matricula.getEstadoMatricula());
    assertNull(matricula.getSeccionId());
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

  @Test
  void darVistoBuenoAdminApruebaDocumentosYActualizaEstado() throws Exception {
    var solicitud = new SolicitudMatriculaPublicaEntity();
    solicitud.setId(SOLICITUD_ID);
    solicitud.setTokenHash(hash(TOKEN));
    solicitud.setEstado(EstadoSolicitudMatriculaPublica.DOCUMENTOS_OBSERVADOS);
    solicitud.setEstadoPartida(com.colegio.shuji.matricula.domain.enums.EstadoValidacionDocumento.OBSERVADO);
    solicitud.setEstadoDniC4(com.colegio.shuji.matricula.domain.enums.EstadoValidacionDocumento.VALIDADO);
    solicitud.setEstadoRecibo(com.colegio.shuji.matricula.domain.enums.EstadoValidacionDocumento.VALIDADO);
    solicitud.setEmailApoderado("apoderado@test.com");
    solicitud.setNombresEstudiante("Carlos");
    solicitud.setApellidoPaternoEstudiante("Perez");
    solicitud.setApellidoMaternoEstudiante("Gomez");
    solicitud.setNumeroDocumentoEstudiante("12345678");
    solicitud.setNombresApoderado("Padre");
    solicitud.setApellidoPaternoApoderado("Perez");
    solicitud.setApellidoMaternoApoderado("Ruiz");
    solicitud.setNumeroDocumentoApoderado("87654321");
    solicitud.setParentesco(Parentesco.PADRE);
    solicitud.setCelularApoderado("999888777");
    solicitud.setAnioLectivoId((short) 2027);
    solicitud.setGradoId((short) 1);
    when(solicitudes.bloquearPorId(SOLICITUD_ID)).thenReturn(Optional.of(solicitud));
    when(solicitudes.saveAndFlush(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));

    var res = service.darVistoBuenoAdmin(SOLICITUD_ID, "Visto bueno directivo", 1L);

    assertEquals(EstadoSolicitudMatriculaPublica.DOCUMENTOS_VALIDADOS, res.estado());
    org.junit.jupiter.api.Assertions.assertEquals(com.colegio.shuji.matricula.domain.enums.EstadoValidacionDocumento.VALIDADO, solicitud.getEstadoPartida());
    org.junit.jupiter.api.Assertions.assertNotEquals(hash(TOKEN), solicitud.getTokenHash());
  }

  private SolicitudMatriculaPublicaEntity solicitudConfirmada() throws Exception {
    var solicitud = new SolicitudMatriculaPublicaEntity();
    solicitud.setId(SOLICITUD_ID);
    solicitud.setTokenHash(hash(TOKEN));
    solicitud.setEstado(EstadoSolicitudMatriculaPublica.PAGO_CONFIRMADO_PENDIENTE_ASIGNACION);
    solicitud.setMatriculaId(42L);
    solicitud.setPagoId("987654321");
    solicitud.setComprobantePagoCodigo("REC-MAT-42");
    solicitud.setFechaEmisionDocumentos(OffsetDateTime.parse("2026-09-23T18:30:00Z"));
    solicitud.setPagoMonto(new BigDecimal("1.00"));
    solicitud.setEstudianteId(101L);
    solicitud.setNombresEstudiante("Ana");
    solicitud.setApellidoPaternoEstudiante("Lopez");
    solicitud.setApellidoMaternoEstudiante("Diaz");
    solicitud.setNumeroDocumentoEstudiante("12345678");
    solicitud.setAnioLectivoId((short) 2027);
    solicitud.setGradoId((short) 1);
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

  @Test
  void finalizarPagoAprovisionaUsuarioApoderadoYEnviaCorreoConCredenciales() throws Exception {
    var solicitud = solicitudConfirmada();
    solicitud.setEstado(EstadoSolicitudMatriculaPublica.PAGO_PENDIENTE);
    solicitud.setEmailApoderado("apoderado@example.test");
    solicitud.setNumeroDocumentoApoderado("87654321");
    solicitud.setNombresApoderado("Carlos");
    solicitud.setApellidoPaternoApoderado("Mendoza");
    solicitud.setApellidoMaternoApoderado("Perez");
    solicitud.setCelularApoderado("987654321");
    solicitud.setDireccionApoderado("Av. Lima 123");
    solicitud.setNumeroDocumentoEstudiante("12345678");
    solicitud.setNombresEstudiante("Mateo");
    solicitud.setApellidoPaternoEstudiante("Mendoza");
    solicitud.setApellidoMaternoEstudiante("Perez");
    solicitud.setFechaNacimientoEstudiante(java.time.LocalDate.of(2018, 5, 20));
    solicitud.setGeneroEstudiante(Genero.M);
    solicitud.setGradoId((short) 1);
    solicitud.setAnioLectivoId((short) 2027);
    solicitud.setPagoMonto(new BigDecimal("350.00"));
    prepararEvidenciaCompleta(solicitud);
    solicitud.setEstadoPartida(com.colegio.shuji.matricula.domain.enums.EstadoValidacionDocumento.VALIDADO);
    solicitud.setEstadoDniC4(com.colegio.shuji.matricula.domain.enums.EstadoValidacionDocumento.VALIDADO);
    solicitud.setEstadoRecibo(com.colegio.shuji.matricula.domain.enums.EstadoValidacionDocumento.VALIDADO);

    when(solicitudes.bloquearPorId(SOLICITUD_ID)).thenReturn(Optional.of(solicitud));
    when(solicitudes.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

    var anio = new com.colegio.shuji.academico.infrastructure.entity.AnioLectivoEntity();
    anio.setId((short) 2027);
    anio.setAnio((short) 2027);
    anio.setAbierto(true);
    when(anios.bloquearPorId((short) 2027)).thenReturn(Optional.of(anio));

    var seccion = new SeccionEntity();
    seccion.setId(1);
    seccion.setAnioLectivoId((short) 2027);
    seccion.setGradoId((short) 1);
    when(secciones.buscarPorAnioLectivoId((short) 2027)).thenReturn(java.util.List.of(seccion));

    when(estudiantes.buscarPorNumeroDocumento("12345678")).thenReturn(java.util.List.of());
    when(estudiantes.saveAndFlush(any())).thenAnswer(inv -> {
      EstudianteEntity e = inv.getArgument(0);
      e.setId(501L);
      return e;
    });

    when(apoderados.buscarPorNumeroDocumento("87654321")).thenReturn(java.util.List.of());
    when(apoderados.saveAndFlush(any())).thenAnswer(inv -> {
      ApoderadoEntity a = inv.getArgument(0);
      a.setId(301L);
      return a;
    });

    when(matriculas.saveAndFlush(any())).thenAnswer(inv -> {
      MatriculaEntity m = inv.getArgument(0);
      m.setId(901L);
      return m;
    });

    when(cobrosMatricula.registrar(any(), any(), any())).thenReturn("B001-00000001");

    when(cuentaApoderadoPort.asegurarCuentaApoderado(
        any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(new CuentaApoderadoPort.CredencialesApoderado(
            888L, "apod_87654321", "apoderado@example.test", "Shuji2026!xyz999", true));

    var pago = new com.colegio.shuji.tesoreria.application.port.out.VerificarPagoPort.PagoVerificado(
        "trans-123", null, new BigDecimal("350.00"), "PEN", MetodoPago.YAPE, true,
        SolicitudMatriculaPublicaService.REFERENCIA_PAGO_PREFIX + SOLICITUD_ID);

    var res = service.finalizarPagoVerificado(SOLICITUD_ID, TOKEN, pago);

    assertEquals(EstadoSolicitudMatriculaPublica.PAGO_CONFIRMADO_PENDIENTE_ASIGNACION, res.estado());

    verify(cuentaApoderadoPort).asegurarCuentaApoderado(
        "87654321", "apoderado@example.test", "Carlos", "Mendoza", "Perez", "987654321", "Av. Lima 123");

    var pdf = org.mockito.ArgumentCaptor.forClass(byte[].class);
    verify(correoSeguimiento).enviarCredencialesMatricula(
        org.mockito.ArgumentMatchers.eq("apoderado@example.test"),
        org.mockito.ArgumentMatchers.contains("Carlos Mendoza"),
        org.mockito.ArgumentMatchers.contains("Mateo Mendoza"),
        org.mockito.ArgumentMatchers.eq("Shuji2026!xyz999"),
        any(),
        org.mockito.ArgumentMatchers.eq("B001-00000001"),
        org.mockito.ArgumentMatchers.eq(SOLICITUD_ID),
        any(),
        pdf.capture(),
        any());
    try (var documento = org.apache.pdfbox.Loader.loadPDF(pdf.getValue())) {
      assertEquals(1, documento.getNumberOfPages());
      String contenido = new org.apache.pdfbox.text.PDFTextStripper().getText(documento);
      assertTrue(contenido.contains("B001-00000001"));
      assertTrue(contenido.contains("apod_87654321"));
      assertTrue(contenido.contains("Shuji2026!xyz999"));
    } catch (java.io.IOException ex) {
      throw new AssertionError("Constancia PDF inválida", ex);
    }
  }

  @Test
  void catalogoOrdenaSeccionesPorNivelYGradoEscolarAscendente() {
    var anio = new AnioLectivoEntity();
    anio.setId((short) 2027);
    anio.setAnio((short) 2027);
    anio.setAbierto(true);
    when(anios.findAll()).thenReturn(java.util.List.of(anio));

    var nivelSecundaria = new NivelEntity();
    nivelSecundaria.setId((short) 3);
    nivelSecundaria.setCodigo(com.colegio.shuji.academico.domain.enums.NivelCodigo.SECUNDARIA);
    nivelSecundaria.setNombre("Secundaria");

    var gradoSec5 = new GradoEntity();
    gradoSec5.setId((short) 14);
    gradoSec5.setNumeroGrado((short) 5);
    gradoSec5.setNombre("5° de Secundaria");
    gradoSec5.setRelacion0(nivelSecundaria);

    var seccionSec5A = new SeccionEntity();
    seccionSec5A.setId(301);
    seccionSec5A.setAnioLectivoId((short) 2027);
    seccionSec5A.setGradoId((short) 14);
    seccionSec5A.setRelacion1(gradoSec5);
    seccionSec5A.setLetra("A");
    seccionSec5A.setCupoMaximo((short) 30);
    seccionSec5A.setVacantesOcupadas((short) 5);

    var nivelPrimaria = new NivelEntity();
    nivelPrimaria.setId((short) 2);
    nivelPrimaria.setCodigo(com.colegio.shuji.academico.domain.enums.NivelCodigo.PRIMARIA);
    nivelPrimaria.setNombre("Primaria");

    var gradoPrim2 = new GradoEntity();
    gradoPrim2.setId((short) 5);
    gradoPrim2.setNumeroGrado((short) 2);
    gradoPrim2.setNombre("2° de Primaria");
    gradoPrim2.setRelacion0(nivelPrimaria);

    var seccionPrim2A = new SeccionEntity();
    seccionPrim2A.setId(201);
    seccionPrim2A.setAnioLectivoId((short) 2027);
    seccionPrim2A.setGradoId((short) 5);
    seccionPrim2A.setRelacion1(gradoPrim2);
    seccionPrim2A.setLetra("A");
    seccionPrim2A.setCupoMaximo((short) 30);
    seccionPrim2A.setVacantesOcupadas((short) 10);

    var nivelInicial = new NivelEntity();
    nivelInicial.setId((short) 1);
    nivelInicial.setCodigo(com.colegio.shuji.academico.domain.enums.NivelCodigo.INICIAL);
    nivelInicial.setNombre("Inicial");

    var gradoIni3Anios = new GradoEntity();
    gradoIni3Anios.setId((short) 1);
    gradoIni3Anios.setNumeroGrado((short) 1);
    gradoIni3Anios.setNombre("Inicial 3 años");
    gradoIni3Anios.setRelacion0(nivelInicial);

    var seccionIni3A = new SeccionEntity();
    seccionIni3A.setId(101);
    seccionIni3A.setAnioLectivoId((short) 2027);
    seccionIni3A.setGradoId((short) 1);
    seccionIni3A.setRelacion1(gradoIni3Anios);
    seccionIni3A.setLetra("A");
    seccionIni3A.setCupoMaximo((short) 25);
    seccionIni3A.setVacantesOcupadas((short) 2);

    var gradoIni5Anios = new GradoEntity();
    gradoIni5Anios.setId((short) 3);
    gradoIni5Anios.setNumeroGrado((short) 3);
    gradoIni5Anios.setNombre("Inicial 5 años");
    gradoIni5Anios.setRelacion0(nivelInicial);

    var seccionIni5B = new SeccionEntity();
    seccionIni5B.setId(102);
    seccionIni5B.setAnioLectivoId((short) 2027);
    seccionIni5B.setGradoId((short) 3);
    seccionIni5B.setRelacion1(gradoIni5Anios);
    seccionIni5B.setLetra("B");
    seccionIni5B.setCupoMaximo((short) 25);
    seccionIni5B.setVacantesOcupadas((short) 4);

    when(secciones.buscarPorAnioLectivoId((short) 2027)).thenReturn(
        java.util.List.of(seccionSec5A, seccionIni5B, seccionPrim2A, seccionIni3A));

    var catalogo = service.catalogo();

    assertNotNull(catalogo);
    assertEquals(1, catalogo.anios().size());
    assertEquals(4, catalogo.secciones().size());

    assertEquals("Inicial 3 años", catalogo.secciones().get(0).grado());
    assertEquals("Inicial 5 años", catalogo.secciones().get(1).grado());
    assertEquals("2° de Primaria", catalogo.secciones().get(2).grado());
    assertEquals("5° de Secundaria", catalogo.secciones().get(3).grado());
  }

  private String hash(String token) throws Exception {
    byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
    return HexFormat.of().formatHex(digest);
  }
}
