package com.colegio.shuji.matricula.infrastructure.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    verify(secciones).buscarPorAnioLectivoId((short) 2026);
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

  @Test
  void creacionConUbigeoNuloUOmisibleGuardaCorrectamente() {
    prepararAdmision();
    org.mockito.ArgumentCaptor<SolicitudMatriculaPublicaEntity> captor =
        org.mockito.ArgumentCaptor.forClass(SolicitudMatriculaPublicaEntity.class);
    when(solicitudes.saveAndFlush(captor.capture()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var request = new com.colegio.shuji.matricula.application.dto.in.CrearSolicitudMatriculaPublicaRequestDto(
        (short) 2026, (short) 1, "12345678", "Ana", "Perez", "Ruiz",
        java.time.LocalDate.of(2020, 3, 31), Genero.F,
        "87654321", "Rosa", "Perez", "Ruiz", "999888777", "familia@example.test",
        "Lima", null, Parentesco.MADRE, true);

    service.crear(request);
    assertNull(captor.getValue().getUbigeoApoderado());
    assertEquals("Ana", captor.getValue().getNombresEstudiante());
  }

  private void prepararAdmision() {
    var anio = new AnioLectivoEntity();
    anio.setAnio((short) 2026); anio.setAbierto(true);
    when(anios.findById((short) 2026)).thenReturn(Optional.of(anio));
    var nivel = new NivelEntity();
    nivel.setCodigo(com.colegio.shuji.academico.domain.enums.NivelCodigo.PRIMARIA);
    var grado = new GradoEntity(); grado.setId((short) 1); grado.setNumeroGrado((short) 1); grado.setRelacion0(nivel);
    var seccion = new SeccionEntity();
    seccion.setAnioLectivoId((short) 2026); seccion.setGradoId((short) 1); seccion.setRelacion1(grado);
    seccion.setCupoMaximo((short) 20); seccion.setVacantesOcupadas((short) 0);
    when(secciones.buscarPorAnioLectivoId((short) 2026)).thenReturn(java.util.List.of(seccion));
  }

  private com.colegio.shuji.matricula.application.dto.in.CrearSolicitudMatriculaPublicaRequestDto solicitudPublicaConNacimiento(
      java.time.LocalDate nacimiento) {
    return new com.colegio.shuji.matricula.application.dto.in.CrearSolicitudMatriculaPublicaRequestDto(
        (short) 2026, (short) 1, "12345678", "Ana", "Perez", "Ruiz", nacimiento, Genero.F,
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
    seccion.setGradoId((short) 1);
    when(secciones.buscarPorAnioLectivoId((short) 2026)).thenReturn(java.util.List.of(seccion));

    String html = service.generarFichaMatriculaHtml(SOLICITUD_ID, TOKEN);

    assertTrue(html.contains("Ficha de matrícula"));
    assertTrue(html.contains("Primaria · Segundo grado · sección y aula pendientes"));
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
    solicitud.setGradoId((short) 1);
    prepararEvidenciaCompleta(solicitud);
    when(solicitudes.bloquearPorId(SOLICITUD_ID)).thenReturn(Optional.of(solicitud));
    when(mercadoPago.crearPreferenciaMatriculaPublica(
        SolicitudMatriculaPublicaService.REFERENCIA_PAGO_PREFIX + SOLICITUD_ID,
        new BigDecimal("350.00"),
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
        new BigDecimal("350.00"),
        "Derecho de matrícula escolar");
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
    solicitud.setAnioLectivoId((short) 2026);
    prepararEvidenciaCompleta(solicitud);
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
    matricula.setAnioLectivoId((short) 2026);
    matricula.setEstadoMatricula(com.colegio.shuji.matricula.domain.enums.EstadoMatricula.PENDIENTE_ASIGNACION);
    var seccion = new SeccionEntity();
    seccion.setId(9);
    seccion.setAnioLectivoId((short) 2026);
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
    assertVinculoMatriculaInconsistenteRechazado(999L, (short) 2026);
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
    matricula.setAnioLectivoId((short) 2026);
    matricula.setEstadoMatricula(com.colegio.shuji.matricula.domain.enums.EstadoMatricula.PENDIENTE_ASIGNACION);
    var seccion = new SeccionEntity();
    seccion.setId(9);
    seccion.setAnioLectivoId((short) 2026);
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
    solicitud.setAnioLectivoId((short) 2026);
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

  private String hash(String token) throws Exception {
    byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
    return HexFormat.of().formatHex(digest);
  }
}
