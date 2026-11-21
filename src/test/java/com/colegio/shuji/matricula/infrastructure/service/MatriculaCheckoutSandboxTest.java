package com.colegio.shuji.matricula.infrastructure.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.colegio.shuji.academico.domain.enums.NivelCodigo;
import com.colegio.shuji.academico.infrastructure.entity.AnioLectivoEntity;
import com.colegio.shuji.academico.infrastructure.entity.GradoEntity;
import com.colegio.shuji.academico.infrastructure.entity.NivelEntity;
import com.colegio.shuji.academico.infrastructure.entity.SeccionEntity;
import com.colegio.shuji.academico.infrastructure.repository.JpaAnioLectivoRepository;
import com.colegio.shuji.academico.infrastructure.repository.JpaGradoRepository;
import com.colegio.shuji.academico.infrastructure.repository.JpaSeccionRepository;
import com.colegio.shuji.matricula.application.dto.in.RegistrarMatriculaPresencialRequestDto;
import com.colegio.shuji.matricula.application.port.out.AlmacenDocumentosMatriculaPort;
import com.colegio.shuji.matricula.application.port.out.CuentaApoderadoPort;
import com.colegio.shuji.matricula.application.port.out.GeminiDocumentoMatriculaPort;
import com.colegio.shuji.matricula.domain.enums.EstadoFirmaDigital;
import com.colegio.shuji.matricula.domain.enums.EstadoSolicitudMatriculaPublica;
import com.colegio.shuji.matricula.domain.enums.EstadoValidacionDocumento;
import com.colegio.shuji.matricula.domain.enums.Genero;
import com.colegio.shuji.matricula.domain.enums.Parentesco;
import com.colegio.shuji.matricula.domain.enums.TipoDocumentoSolicitud;
import com.colegio.shuji.matricula.infrastructure.entity.ApoderadoEntity;
import com.colegio.shuji.matricula.infrastructure.entity.DocumentoMatriculaVersionEntity;
import com.colegio.shuji.matricula.infrastructure.entity.EstudianteEntity;
import com.colegio.shuji.matricula.infrastructure.entity.MatriculaEntity;
import com.colegio.shuji.matricula.infrastructure.entity.SolicitudMatriculaDocumentoEntity;
import com.colegio.shuji.matricula.infrastructure.entity.SolicitudMatriculaDocumentoId;
import com.colegio.shuji.matricula.infrastructure.entity.SolicitudMatriculaPublicaEntity;
import com.colegio.shuji.matricula.infrastructure.repository.JpaApoderadoRepository;
import com.colegio.shuji.matricula.infrastructure.repository.JpaDocumentoMatriculaVersionRepository;
import com.colegio.shuji.matricula.infrastructure.repository.JpaEstudianteApoderadoRepository;
import com.colegio.shuji.matricula.infrastructure.repository.JpaEstudianteRepository;
import com.colegio.shuji.matricula.infrastructure.repository.JpaMatriculaRepository;
import com.colegio.shuji.matricula.infrastructure.repository.JpaSolicitudMatriculaDocumentoRepository;
import com.colegio.shuji.matricula.infrastructure.repository.JpaSolicitudMatriculaPublicaRepository;
import com.colegio.shuji.shared.domain.exception.BusinessException;
import com.colegio.shuji.tesoreria.application.dto.out.ComprobanteResponseDto;
import com.colegio.shuji.tesoreria.application.port.in.EmitirComprobanteUseCase;
import com.colegio.shuji.tesoreria.application.port.in.RegistrarCobroMatriculaUseCase;
import com.colegio.shuji.tesoreria.application.port.out.ConceptoCobroRepositoryPort;
import com.colegio.shuji.tesoreria.application.port.out.MercadoPagoPort;
import com.colegio.shuji.tesoreria.application.port.out.ObligacionRepositoryPort;
import com.colegio.shuji.tesoreria.application.port.out.PagoRepositoryPort;
import com.colegio.shuji.tesoreria.application.port.out.VerificarPagoPort.PagoVerificado;
import com.colegio.shuji.tesoreria.domain.enums.EstadoComprobante;
import com.colegio.shuji.tesoreria.domain.enums.MetodoPago;
import com.colegio.shuji.tesoreria.domain.enums.TipoComprobante;
import com.colegio.shuji.tesoreria.domain.enums.TipoConcepto;
import com.colegio.shuji.tesoreria.domain.model.ConceptoCobro;
import com.colegio.shuji.tesoreria.domain.model.ObligacionPago;
import com.colegio.shuji.tesoreria.domain.model.PagoTransaccion;
import com.colegio.shuji.tesoreria.infrastructure.adapter.ComprobanteMatriculaPdfAdapter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class MatriculaCheckoutSandboxTest {

  private static final byte[] MIN_PDF =
      "%PDF-1.4\n1 0 obj<<>>endobj\ntrailer<<>>\n%%EOF".getBytes(StandardCharsets.US_ASCII);
  private static final byte[] MIN_PNG =
      new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 1};

  private static String sha256(String token) throws Exception {
    byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
    return HexFormat.of().formatHex(digest);
  }

  @Nested
  class SolicitudMatriculaPublicaSandboxTests {
    @Mock private JpaSolicitudMatriculaPublicaRepository solicitudes;
    @Mock private JpaSolicitudMatriculaDocumentoRepository documentos;
    @Mock private JpaDocumentoMatriculaVersionRepository versiones;
    @Mock private JpaAnioLectivoRepository anios;
    @Mock private JpaSeccionRepository secciones;
    @Mock private JpaEstudianteRepository estudiantes;
    @Mock private JpaApoderadoRepository apoderados;
    @Mock private JpaEstudianteApoderadoRepository vinculos;
    @Mock private JpaMatriculaRepository matriculas;
    @Mock private GeminiDocumentoMatriculaPort gemini;
    @Mock private MercadoPagoPort mercadoPago;
    @Mock private RegistrarCobroMatriculaUseCase cobrosMatricula;
    @Mock private CuentaApoderadoPort cuentaApoderadoPort;

    @InjectMocks private SolicitudMatriculaPublicaService service;

    @BeforeEach
    void initFields() {
      ReflectionTestUtils.setField(service, "cuentaApoderadoPort", cuentaApoderadoPort);
      ReflectionTestUtils.setField(service, "comprobantePdfAdapter", new ComprobanteMatriculaPdfAdapter());
      ReflectionTestUtils.setField(service, "montoMatriculaPublica", new BigDecimal("350.00"));
      ReflectionTestUtils.setField(service, "montoCobroMatriculaPublica", new BigDecimal("350.00"));
    }

    private void mockEvidenciaCompleta(UUID solId) {
      var docs = new java.util.ArrayList<SolicitudMatriculaDocumentoEntity>();
      var vers = new java.util.ArrayList<DocumentoMatriculaVersionEntity>();
      for (TipoDocumentoSolicitud tipo : TipoDocumentoSolicitud.REQUERIDOS) {
        UUID verId = UUID.randomUUID();
        var ver = new DocumentoMatriculaVersionEntity();
        ver.setId(verId);
        ver.setSolicitudId(solId);
        ver.setTipo(tipo);
        ver.setMimeType("application/pdf");
        ver.setIaValida(true);
        ver.setDniApiCoincide(true);
        ver.setVinculoParentalCoincide(true);
        ver.setEstadoFirma(EstadoFirmaDigital.VALIDA);
        vers.add(ver);

        var doc = new SolicitudMatriculaDocumentoEntity();
        doc.setId(new SolicitudMatriculaDocumentoId(solId, tipo));
        doc.setVersionId(verId);
        doc.setMimeType("application/pdf");
        docs.add(doc);
      }
      when(documentos.findByIdSolicitudIdIn(anyList())).thenReturn(docs);
      when(versiones.findAllById(any())).thenReturn(vers);
    }

    @Test
    void rechazaSimularPagoPublicoCuandoSandboxEstaDeshabilitado() {
      ReflectionTestUtils.setField(service, "mercadoPagoSandbox", false);
      var ex =
          assertThrows(
              ResponseStatusException.class,
              () -> service.simularPago(UUID.randomUUID(), "tok", MetodoPago.YAPE, "YAPE-123456"));
      assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void rechazaEfectivoEnSimularPagoPublico() {
      ReflectionTestUtils.setField(service, "mercadoPagoSandbox", true);
      var ex =
          assertThrows(
              ResponseStatusException.class,
              () -> service.simularPago(UUID.randomUUID(), "tok", MetodoPago.EFECTIVO, "EF-1"));
      assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void simularPagoYapeFinalizaMatriculaYEmiteComprobante() throws Exception {
      ReflectionTestUtils.setField(service, "mercadoPagoSandbox", true);
      UUID solId = UUID.randomUUID();
      String token = "tok-secreto";
      var ent = solicitudPendientePago(solId, token);

      when(solicitudes.bloquearPorId(solId)).thenReturn(Optional.of(ent));
      mockEvidenciaCompleta(solId);
      var anio = new AnioLectivoEntity();
      anio.setId((short) 1);
      anio.setAnio((short) 2026);
      anio.setAbierto(true);
      when(anios.bloquearPorId((short) 1)).thenReturn(Optional.of(anio));
      var seccion = new SeccionEntity();
      seccion.setId(37);
      seccion.setAnioLectivoId((short) 1);
      seccion.setGradoId((short) 10);
      seccion.setLetra("A");
      when(secciones.buscarPorAnioLectivoId((short) 1)).thenReturn(List.of(seccion));
      lenient().when(estudiantes.buscarPorNumeroDocumento("71234567")).thenReturn(List.of());
      lenient()
          .when(estudiantes.saveAndFlush(any()))
          .thenAnswer(
              inv -> {
                EstudianteEntity e = inv.getArgument(0);
                e.setId(501L);
                return e;
              });
      lenient().when(matriculas.existsByEstudianteIdAndAnioLectivoId(501L, (short) 1)).thenReturn(false);
      lenient().when(apoderados.buscarPorNumeroDocumento("41234567")).thenReturn(List.of());
      lenient()
          .when(apoderados.saveAndFlush(any()))
          .thenAnswer(
              inv -> {
                ApoderadoEntity a = inv.getArgument(0);
                a.setId(601L);
                return a;
              });
      lenient().when(vinculos.buscarPorEstudianteId(501L)).thenReturn(List.of());
      lenient().when(vinculos.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
      lenient()
          .when(matriculas.saveAndFlush(any()))
          .thenAnswer(
              inv -> {
                MatriculaEntity m = inv.getArgument(0);
                m.setId(700L);
                return m;
              });
      when(cobrosMatricula.registrar(eq(700L), any(), any(PagoVerificado.class)))
          .thenReturn("B001-00000123");
      when(solicitudes.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

      var resp = service.simularPago(solId, token, MetodoPago.YAPE, "YAPE-482910");

      assertEquals(EstadoSolicitudMatriculaPublica.PAGO_CONFIRMADO_PENDIENTE_ASIGNACION, resp.estado());
      var pagoCaptor = ArgumentCaptor.forClass(PagoVerificado.class);
      verify(cobrosMatricula).registrar(eq(700L), eq(new BigDecimal("350.00")), pagoCaptor.capture());
      assertEquals(MetodoPago.YAPE, pagoCaptor.getValue().metodo());
      assertTrue(pagoCaptor.getValue().aprobado());
    }

    @Test
    void crearPagoUsaFallbackSandboxSiFallaMercadoPago() throws Exception {
      ReflectionTestUtils.setField(service, "mercadoPagoSandbox", true);
      UUID solId = UUID.randomUUID();
      String token = "tok-secreto";
      var ent = solicitudPendientePago(solId, token);
      ent.setEstado(EstadoSolicitudMatriculaPublica.DOCUMENTOS_VALIDADOS);
      ent.setPagoEnlace(null);

      when(solicitudes.bloquearPorId(solId)).thenReturn(Optional.of(ent));
      mockEvidenciaCompleta(solId);
      when(mercadoPago.crearPreferenciaMatriculaPublica(anyString(), any(), anyString()))
          .thenThrow(new BusinessException("Mercado Pago HTTP 500 simulado"));
      when(solicitudes.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

      var resp = service.crearPago(solId, token);

      assertNotNull(resp.enlacePago());
      assertTrue(resp.enlacePago().contains("SIM-" + solId));
      assertEquals(EstadoSolicitudMatriculaPublica.PAGO_PENDIENTE, ent.getEstado());
    }

    private SolicitudMatriculaPublicaEntity solicitudPendientePago(UUID solId, String token) throws Exception {
      var ent = new SolicitudMatriculaPublicaEntity();
      ent.setId(solId);
      ent.setTokenHash(sha256(token));
      ent.setEstado(EstadoSolicitudMatriculaPublica.PAGO_PENDIENTE);
      ent.setAnioLectivoId((short) 1);
      ent.setGradoId((short) 10);
      ent.setNombresEstudiante("Camila");
      ent.setApellidoPaternoEstudiante("Rojas");
      ent.setApellidoMaternoEstudiante("Paz");
      ent.setNumeroDocumentoEstudiante("71234567");
      ent.setNombresApoderado("Marco");
      ent.setApellidoPaternoApoderado("Rojas");
      ent.setApellidoMaternoApoderado("Silva");
      ent.setNumeroDocumentoApoderado("41234567");
      ent.setEmailApoderado("marco@example.test");
      ent.setCelularApoderado("987654321");
      ent.setDireccionApoderado("Av. Lima 123");
      ent.setParentesco(Parentesco.MADRE);
      ent.setFechaNacimientoEstudiante(LocalDate.of(2010, 4, 1));
      ent.setEstadoPartida(EstadoValidacionDocumento.VALIDADO);
      ent.setEstadoDniC4(EstadoValidacionDocumento.VALIDADO);
      ent.setEstadoRecibo(EstadoValidacionDocumento.VALIDADO);
      ent.setPagoMonto(new BigDecimal("350.00"));
      ent.setMatriculaId(700L);
      return ent;
    }
  }

  @Nested
  class MatriculaPresencialSandboxYCashTests {
    @Mock private JpaAnioLectivoRepository anios;
    @Mock private JpaGradoRepository grados;
    @Mock private JpaSeccionRepository secciones;
    @Mock private JpaEstudianteRepository estudiantes;
    @Mock private JpaApoderadoRepository apoderados;
    @Mock private JpaEstudianteApoderadoRepository vinculos;
    @Mock private JpaMatriculaRepository matriculas;
    @Mock private JpaSolicitudMatriculaPublicaRepository solicitudes;
    @Mock private JpaSolicitudMatriculaDocumentoRepository documentos;
    @Mock private JpaDocumentoMatriculaVersionRepository versiones;
    @Mock private ConceptoCobroRepositoryPort conceptos;
    @Mock private ObligacionRepositoryPort obligaciones;
    @Mock private PagoRepositoryPort pagos;
    @Mock private EmitirComprobanteUseCase comprobantes;
    @Mock private AlmacenDocumentosMatriculaPort almacenDocumentos;
    @Mock private SolicitudMatriculaPublicaService solicitudesPublicas;
    @Mock private MercadoPagoPort mercadoPago;
    @Mock private CuentaApoderadoPort cuentaApoderadoPort;

    @InjectMocks private MatriculaPresencialService service;

    @BeforeEach
    void initFields() {
      ReflectionTestUtils.setField(service, "pdfAdapter", new ComprobanteMatriculaPdfAdapter());
      ReflectionTestUtils.setField(service, "cuentaApoderadoPort", cuentaApoderadoPort);
      ReflectionTestUtils.setField(service, "mercadoPagoSandbox", true);
      TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void cleanupSync() {
      if (TransactionSynchronizationManager.isSynchronizationActive()) {
        TransactionSynchronizationManager.clearSynchronization();
      }
    }

    @Test
    void rechazaSimularPagoPresencialSiSandboxDesactivado() {
      ReflectionTestUtils.setField(service, "mercadoPagoSandbox", false);
      var ex =
          assertThrows(
              ResponseStatusException.class,
              () -> service.simularPagoEnLinea(UUID.randomUUID(), MetodoPago.PLIN, "PLIN-123456"));
      assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void registraMatriculaEnEfectivoSinConstanciaDePagoYGuardaSolo4Documentos() {
      var nivel = new NivelEntity();
      nivel.setId((short) 3);
      nivel.setCodigo(NivelCodigo.SECUNDARIA);
      nivel.setNombre("Secundaria");

      var anio = new AnioLectivoEntity();
      anio.setId((short) 2);
      anio.setAnio((short) 2027);
      anio.setAbierto(true);

      var grado = new GradoEntity();
      grado.setId((short) 10);
      grado.setNumeroGrado((short) 5);
      grado.setNombre("5° de Secundaria");
      grado.setRelacion0(nivel);

      var seccion = new SeccionEntity();
      seccion.setId(37);
      seccion.setAnioLectivoId((short) 2);
      seccion.setGradoId((short) 10);
      seccion.setNivelId((short) 3);
      seccion.setLetra("A");
      seccion.setCupoMaximo((short) 30);
      seccion.setVacantesOcupadas((short) 20);
      seccion.setRelacion1(grado);

      when(solicitudesPublicas.obtenerMontoMatriculaPublica()).thenReturn(new BigDecimal("350.00"));
      when(anios.bloquearPorId((short) 2)).thenReturn(Optional.of(anio));
      when(grados.bloquearPorId((short) 10)).thenReturn(Optional.of(grado));
      when(secciones.bloquearPorId(37)).thenReturn(Optional.of(seccion));
      when(solicitudes.existsByNumeroDocumentoEstudianteAndEstadoIn(eq("75551111"), anyList())).thenReturn(false);
      lenient()
          .when(conceptos.listar())
          .thenReturn(
              List.of(
                  ConceptoCobro.builder()
                      .id((short) 1)
                      .codigo("MAT")
                      .nombre("Matrícula")
                      .tipoConcepto(TipoConcepto.MATRICULA)
                      .montoSugerido(new BigDecimal("350.00"))
                      .activo(true)
                      .build()));
      when(estudiantes.buscarPorNumeroDocumento("75551111")).thenReturn(List.of());
      when(estudiantes.saveAndFlush(any()))
          .thenAnswer(
              inv -> {
                EstudianteEntity e = inv.getArgument(0);
                e.setId(801L);
                return e;
              });
      lenient().when(matriculas.existsByEstudianteIdAndAnioLectivoId(801L, (short) 2)).thenReturn(false);
      lenient().when(matriculas.buscarPorEstudianteId(801L)).thenReturn(List.of());
      when(apoderados.buscarPorNumeroDocumento("45551111")).thenReturn(List.of());
      when(apoderados.saveAndFlush(any()))
          .thenAnswer(
              inv -> {
                ApoderadoEntity a = inv.getArgument(0);
                a.setId(901L);
                return a;
              });
      lenient().when(vinculos.buscarPorEstudianteId(801L)).thenReturn(List.of());
      lenient().when(vinculos.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
      when(matriculas.saveAndFlush(any()))
          .thenAnswer(
              inv -> {
                MatriculaEntity m = inv.getArgument(0);
                m.setId(1001L);
                return m;
              });
      when(solicitudes.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
      when(obligaciones.guardar(any()))
          .thenAnswer(
              inv -> {
                ObligacionPago o = inv.getArgument(0);
                o.setId(2001L);
                return o;
              });
      when(pagos.guardar(any()))
          .thenAnswer(
              inv -> {
                PagoTransaccion p = inv.getArgument(0);
                p.setId(3001L);
                return p;
              });
      when(comprobantes.emitirAutomaticoParaPago(3001L, TipoComprobante.BOLETA, "B001"))
          .thenReturn(
              new ComprobanteResponseDto(
                  50L,
                  3001L,
                  TipoComprobante.BOLETA,
                  "B001",
                  88,
                  OffsetDateTime.now(),
                  new BigDecimal("350.00"),
                  EstadoComprobante.EMITIDO,
                  null,
                  null,
                  null));
      lenient().when(versiones.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
      lenient().when(documentos.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
      when(almacenDocumentos.guardar(any(), any(), any(), anyString()))
          .thenReturn(new AlmacenDocumentosMatriculaPort.ArchivoGuardado("shuji-bucket", "doc-key"));

      var req = new RegistrarMatriculaPresencialRequestDto();
      req.setAnioLectivoId((short) 2);
      req.setGradoId((short) 10);
      req.setSeccionId(37);
      req.setNumeroDocumentoEstudiante("75551111");
      req.setNombresEstudiante("Valeria");
      req.setApellidoPaternoEstudiante("Gomez");
      req.setApellidoMaternoEstudiante("Diaz");
      req.setFechaNacimientoEstudiante(LocalDate.of(2010, 5, 14));
      req.setGeneroEstudiante(Genero.F);
      req.setNumeroDocumentoApoderado("45551111");
      req.setNombresApoderado("Carlos");
      req.setApellidoPaternoApoderado("Gomez");
      req.setApellidoMaternoApoderado("Rios");
      req.setCelularApoderado("987654321");
      req.setEmailApoderado("carlos@example.test");
      req.setDireccionApoderado("Av. Principal 100");
      req.setParentesco(Parentesco.PADRE);
      req.setMontoEfectivo(new BigDecimal("350.00"));
      req.setPartida(new MockMultipartFile("partida", "partida.pdf", "application/pdf", MIN_PDF));
      req.setDni(new MockMultipartFile("dni", "dni.pdf", "application/pdf", MIN_PDF));
      req.setRecibo(new MockMultipartFile("recibo", "recibo.pdf", "application/pdf", MIN_PDF));
      req.setFoto(new MockMultipartFile("foto", "foto.png", "image/png", MIN_PNG));
      req.setConstanciaPago(null);

      var res = service.registrar(req);

      assertEquals(1001L, res.matriculaId());
      assertEquals("B001-00000088", res.comprobanteCodigo());
      assertEquals("EFECTIVO - CAJA", res.metodoPago());
      verify(almacenDocumentos, never())
          .guardar(any(), eq(TipoDocumentoSolicitud.CONSTANCIA_PAGO), any(), anyString());
    }
  }
}
