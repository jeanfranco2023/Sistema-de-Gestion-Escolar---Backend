package com.colegio.shuji;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.colegio.shuji.academico.application.port.out.AnioLectivoRepositoryPort;
import com.colegio.shuji.academico.application.port.out.SeccionRepositoryPort;
import com.colegio.shuji.academico.domain.model.AnioLectivo;
import com.colegio.shuji.academico.domain.model.Seccion;
import com.colegio.shuji.curriculo.application.port.out.AsignacionRepositoryPort;
import com.colegio.shuji.curriculo.application.port.out.CurriculoRepositoryPort;
import com.colegio.shuji.curriculo.domain.model.AreaCurricular;
import com.colegio.shuji.curriculo.domain.model.AsignacionDocente;
import com.colegio.shuji.evaluacion.application.dto.in.CalificacionItemRequestDto;
import com.colegio.shuji.evaluacion.application.dto.in.RegistrarCalificacionesMasivasRequestDto;
import com.colegio.shuji.evaluacion.application.dto.out.CalificacionResponseDto;
import com.colegio.shuji.evaluacion.application.port.in.RegistrarEvaluacionCnebUseCase;
import com.colegio.shuji.evaluacion.application.port.out.InscripcionRefuerzoRepositoryPort;
import com.colegio.shuji.evaluacion.application.port.out.RefuerzoRepositoryPort;
import com.colegio.shuji.evaluacion.application.service.AutoRefuerzoEvaluacionDecorator;
import com.colegio.shuji.evaluacion.domain.enums.CalificacionCualitativa;
import com.colegio.shuji.evaluacion.domain.enums.EstadoAsistenciaRefuerzo;
import com.colegio.shuji.evaluacion.domain.model.InscripcionRefuerzo;
import com.colegio.shuji.evaluacion.domain.model.SesionRefuerzo;
import com.colegio.shuji.matricula.application.port.in.GestionarPagoMatriculaPublicaUseCase;
import com.colegio.shuji.matricula.application.port.out.ApoderadoRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.EstudianteApoderadoRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.EstudianteRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.MatriculaRepositoryPort;
import com.colegio.shuji.matricula.domain.enums.EstadoMatricula;
import com.colegio.shuji.matricula.domain.model.Matricula;
import com.colegio.shuji.shared.application.port.out.ActorActualPort;
import com.colegio.shuji.shared.domain.exception.BusinessException;
import com.colegio.shuji.tesoreria.application.dto.in.CobroExtraordinarioRequest;
import com.colegio.shuji.tesoreria.application.dto.out.ComprobanteResponseDto;
import com.colegio.shuji.tesoreria.application.dto.out.ConceptoCobroResponseDto;
import com.colegio.shuji.tesoreria.application.dto.out.PreferenciaMercadoPagoResponseDto;
import com.colegio.shuji.tesoreria.application.dto.out.TransaccionResponseDto;
import com.colegio.shuji.tesoreria.application.mapper.TesoreriaMapper;
import com.colegio.shuji.tesoreria.application.port.in.EmitirComprobanteUseCase;
import com.colegio.shuji.tesoreria.application.port.out.ConceptoCobroRepositoryPort;
import com.colegio.shuji.tesoreria.application.port.out.MercadoPagoPort;
import com.colegio.shuji.tesoreria.application.port.out.ObligacionRepositoryPort;
import com.colegio.shuji.tesoreria.application.port.out.PagoRepositoryPort;
import com.colegio.shuji.tesoreria.application.port.out.VerificarPagoPort;
import com.colegio.shuji.tesoreria.application.service.TesoreriaService;
import com.colegio.shuji.tesoreria.domain.enums.EstadoComprobante;
import com.colegio.shuji.tesoreria.domain.enums.EstadoObligacion;
import com.colegio.shuji.tesoreria.domain.enums.EstadoPago;
import com.colegio.shuji.tesoreria.domain.enums.MetodoPago;
import com.colegio.shuji.tesoreria.domain.enums.PasarelaProveedor;
import com.colegio.shuji.tesoreria.domain.enums.TipoComprobante;
import com.colegio.shuji.tesoreria.domain.enums.TipoConcepto;
import com.colegio.shuji.tesoreria.domain.model.ConceptoCobro;
import com.colegio.shuji.tesoreria.domain.model.ObligacionPago;
import com.colegio.shuji.tesoreria.domain.model.PagoTransaccion;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class SandboxCheckoutYAutoRefuerzoTest {

  @Nested
  class AutoRefuerzoEvaluacionDecoratorTests {
    @Mock private RegistrarEvaluacionCnebUseCase delegate;
    @Mock private AsignacionRepositoryPort asignaciones;
    @Mock private CurriculoRepositoryPort curriculos;
    @Mock private MatriculaRepositoryPort matriculas;
    @Mock private SeccionRepositoryPort secciones;
    @Mock private RefuerzoRepositoryPort refuerzos;
    @Mock private InscripcionRefuerzoRepositoryPort inscripciones;

    private AutoRefuerzoEvaluacionDecorator decorator;

    @BeforeEach
    void setUp() {
      decorator =
          new AutoRefuerzoEvaluacionDecorator(
              delegate, asignaciones, curriculos, matriculas, secciones, refuerzos, inscripciones);
    }

    @Test
    void derivaEstudiantesConByCPriorizandoCSinDuplicarYCapTema40Chars() {
      var req =
          new RegistrarCalificacionesMasivasRequestDto(
              (short) 4,
              301L,
              List.of(
                  new CalificacionItemRequestDto(
                      91L, (short) 41, CalificacionCualitativa.B, "En proceso", false),
                  new CalificacionItemRequestDto(
                      91L, (short) 42, CalificacionCualitativa.C, "Requiere apoyo", false),
                  new CalificacionItemRequestDto(
                      92L, (short) 41, CalificacionCualitativa.A, "Esperado", false)));
      var now = OffsetDateTime.now();
      when(delegate.registrar(req))
          .thenReturn(
              List.of(
                  new CalificacionResponseDto(
                      1001L,
                      91L,
                      (short) 1,
                      37,
                      (short) 4,
                      301L,
                      (short) 16,
                      63L,
                      (short) 41,
                      CalificacionCualitativa.B,
                      "En proceso",
                      false,
                      true,
                      now,
                      now),
                  new CalificacionResponseDto(
                      1002L,
                      91L,
                      (short) 1,
                      37,
                      (short) 4,
                      301L,
                      (short) 16,
                      63L,
                      (short) 42,
                      CalificacionCualitativa.C,
                      "Requiere apoyo",
                      false,
                      true,
                      now,
                      now),
                  new CalificacionResponseDto(
                      1003L,
                      92L,
                      (short) 1,
                      37,
                      (short) 4,
                      301L,
                      (short) 16,
                      63L,
                      (short) 41,
                      CalificacionCualitativa.A,
                      "Esperado",
                      false,
                      false,
                      now,
                      now)));
      when(asignaciones.buscarPorId(301L))
          .thenReturn(
              Optional.of(
                  AsignacionDocente.builder()
                      .id(301L)
                      .anioLectivoId((short) 1)
                      .seccionId(37)
                      .nivelId((short) 3)
                      .areaCurricularId((short) 16)
                      .docenteUsuarioId(63L)
                      .build()));
      when(matriculas.buscarPorIds(List.of(91L)))
          .thenReturn(
              List.of(
                  Matricula.builder()
                      .id(91L)
                      .estudianteId(501L)
                      .seccionId(37)
                      .estadoMatricula(EstadoMatricula.MATRICULADO)
                      .build()));
      when(refuerzos.buscarPorPeriodoAcademicoId((short) 4)).thenReturn(List.of());
      when(secciones.buscarPorId(37))
          .thenReturn(Optional.of(Seccion.builder().id(37).codigoAula("SEC-5-A").build()));
      when(curriculos.buscarPorId((short) 16))
          .thenReturn(
              Optional.of(
                  AreaCurricular.builder()
                      .id((short) 16)
                      .nombre("Desarrollo Personal, Ciudadanía y Cívica Avanzado")
                      .build()));
      when(refuerzos.buscarPorAnioLectivoId((short) 1)).thenReturn(List.of());
      when(refuerzos.guardar(any()))
          .thenAnswer(
              inv -> {
                SesionRefuerzo s = inv.getArgument(0);
                s.setId(88L);
                return s;
              });
      when(inscripciones.buscarPorSesionRefuerzoId(88L)).thenReturn(List.of());

      decorator.registrar(req);

      var sesionCaptor = ArgumentCaptor.forClass(SesionRefuerzo.class);
      verify(refuerzos).guardar(sesionCaptor.capture());
      assertTrue(sesionCaptor.getValue().getTema().length() <= 40);
      assertTrue(sesionCaptor.getValue().getTema().startsWith("Refuerzo: "));
      assertEquals("SEC-5-A", sesionCaptor.getValue().getAulaAsignada());
      assertEquals(LocalTime.of(17, 0), sesionCaptor.getValue().getHoraInicio());
      assertEquals(LocalTime.of(17, 45), sesionCaptor.getValue().getHoraFin());
      assertFalse(sesionCaptor.getValue().getFechaProgramada().getDayOfWeek() == DayOfWeek.SATURDAY);
      assertFalse(sesionCaptor.getValue().getFechaProgramada().getDayOfWeek() == DayOfWeek.SUNDAY);

      var inscCaptor = ArgumentCaptor.forClass(InscripcionRefuerzo.class);
      verify(inscripciones).guardar(inscCaptor.capture());
      assertEquals(88L, inscCaptor.getValue().getSesionRefuerzoId());
      assertEquals(501L, inscCaptor.getValue().getEstudianteId());
      assertEquals(1002L, inscCaptor.getValue().getCalificacionOrigenId());
      assertEquals(EstadoAsistenciaRefuerzo.PENDIENTE, inscCaptor.getValue().getEstadoAsistencia());
    }

    @Test
    void reutilizaSesionExistenteYOmiteEstudianteYaInscrito() {
      var req =
          new RegistrarCalificacionesMasivasRequestDto(
              (short) 4,
              301L,
              List.of(
                  new CalificacionItemRequestDto(
                      91L, (short) 41, CalificacionCualitativa.B, "En proceso", false)));
      var now = OffsetDateTime.now();
      when(delegate.registrar(req))
          .thenReturn(
              List.of(
                  new CalificacionResponseDto(
                      1001L,
                      91L,
                      (short) 1,
                      37,
                      (short) 4,
                      301L,
                      (short) 16,
                      63L,
                      (short) 41,
                      CalificacionCualitativa.B,
                      "En proceso",
                      false,
                      true,
                      now,
                      now)));
      when(asignaciones.buscarPorId(301L))
          .thenReturn(
              Optional.of(
                  AsignacionDocente.builder()
                      .id(301L)
                      .anioLectivoId((short) 1)
                      .seccionId(37)
                      .areaCurricularId((short) 16)
                      .docenteUsuarioId(63L)
                      .build()));
      when(matriculas.buscarPorIds(List.of(91L)))
          .thenReturn(
              List.of(
                  Matricula.builder()
                      .id(91L)
                      .estudianteId(501L)
                      .seccionId(37)
                      .estadoMatricula(EstadoMatricula.MATRICULADO)
                      .build()));
      when(refuerzos.buscarPorPeriodoAcademicoId((short) 4))
          .thenReturn(
              List.of(
                  SesionRefuerzo.builder()
                      .id(77L)
                      .anioLectivoId((short) 1)
                      .periodoAcademicoId((short) 4)
                      .areaCurricularId((short) 16)
                      .docenteUsuarioId(63L)
                      .tema("Refuerzo: DPCC")
                      .fechaProgramada(LocalDate.now().plusDays(3))
                      .horaInicio(LocalTime.of(17, 0))
                      .horaFin(LocalTime.of(17, 45))
                      .build()));
      when(inscripciones.buscarPorSesionRefuerzoId(77L))
          .thenReturn(
              List.of(
                  InscripcionRefuerzo.builder()
                      .id(1L)
                      .sesionRefuerzoId(77L)
                      .estudianteId(501L)
                      .build()));

      decorator.registrar(req);

      verify(refuerzos, never()).guardar(any());
      verify(inscripciones, never()).guardar(any());
    }

    @Test
    void creaSesionPropiaPorSeccionCuandoDocenteYaTieneSesionEnOtraAula() {
      var req =
          new RegistrarCalificacionesMasivasRequestDto(
              (short) 4,
              302L,
              List.of(
                  new CalificacionItemRequestDto(
                      95L, (short) 41, CalificacionCualitativa.C, "Requiere apoyo obligatorio", false)));
      var now = OffsetDateTime.now();
      when(delegate.registrar(req))
          .thenReturn(
              List.of(
                  new CalificacionResponseDto(
                      2001L,
                      95L,
                      (short) 1,
                      38,
                      (short) 4,
                      302L,
                      (short) 16,
                      63L,
                      (short) 41,
                      CalificacionCualitativa.C,
                      "Requiere apoyo obligatorio",
                      false,
                      true,
                      now,
                      now)));
      when(asignaciones.buscarPorId(302L))
          .thenReturn(
              Optional.of(
                  AsignacionDocente.builder()
                      .id(302L)
                      .anioLectivoId((short) 1)
                      .seccionId(38)
                      .areaCurricularId((short) 16)
                      .docenteUsuarioId(63L)
                      .build()));
      when(matriculas.buscarPorIds(List.of(95L)))
          .thenReturn(
              List.of(
                  Matricula.builder()
                      .id(95L)
                      .estudianteId(505L)
                      .seccionId(38)
                      .estadoMatricula(EstadoMatricula.MATRICULADO)
                      .build()));
      when(secciones.buscarPorId(38))
          .thenReturn(Optional.of(Seccion.builder().id(38).codigoAula("SEC-5-B").build()));
      when(refuerzos.buscarPorPeriodoAcademicoId((short) 4))
          .thenReturn(
              List.of(
                  SesionRefuerzo.builder()
                      .id(77L)
                      .anioLectivoId((short) 1)
                      .periodoAcademicoId((short) 4)
                      .areaCurricularId((short) 16)
                      .docenteUsuarioId(63L)
                      .aulaAsignada("SEC-5-A")
                      .tema("Refuerzo: DPCC")
                      .fechaProgramada(LocalDate.now().plusDays(3))
                      .horaInicio(LocalTime.of(17, 0))
                      .horaFin(LocalTime.of(17, 45))
                      .build()));
      when(curriculos.buscarPorId((short) 16))
          .thenReturn(
              Optional.of(AreaCurricular.builder().id((short) 16).nombre("DPCC").build()));
      when(refuerzos.buscarPorAnioLectivoId((short) 1)).thenReturn(List.of());
      when(refuerzos.guardar(any()))
          .thenAnswer(
              inv -> {
                SesionRefuerzo s = inv.getArgument(0);
                s.setId(89L);
                return s;
              });
      when(inscripciones.buscarPorSesionRefuerzoId(89L)).thenReturn(List.of());

      decorator.registrar(req);

      var sesionCaptor = ArgumentCaptor.forClass(SesionRefuerzo.class);
      verify(refuerzos).guardar(sesionCaptor.capture());
      assertEquals("SEC-5-B", sesionCaptor.getValue().getAulaAsignada());

      var inscCaptor = ArgumentCaptor.forClass(InscripcionRefuerzo.class);
      verify(inscripciones).guardar(inscCaptor.capture());
      assertEquals(89L, inscCaptor.getValue().getSesionRefuerzoId());
      assertEquals(505L, inscCaptor.getValue().getEstudianteId());
      assertEquals(2001L, inscCaptor.getValue().getCalificacionOrigenId());
    }
  }

  @Nested
  class TesoreriaSandboxCheckoutTests {
    @Mock private TesoreriaMapper mapper;
    @Mock private ObligacionRepositoryPort obligaciones;
    @Mock private PagoRepositoryPort pagos;
    @Mock private ConceptoCobroRepositoryPort conceptos;
    @Mock private MatriculaRepositoryPort matriculas;
    @Mock private EstudianteRepositoryPort estudiantes;
    @Mock private AnioLectivoRepositoryPort anios;
    @Mock private VerificarPagoPort verificador;
    @Mock private MercadoPagoPort mercadoPagoPort;
    @Mock private EmitirComprobanteUseCase comprobantes;
    @Mock private ActorActualPort actor;
    @Mock private ApoderadoRepositoryPort apoderados;
    @Mock private EstudianteApoderadoRepositoryPort estudianteApoderados;
    @Mock private GestionarPagoMatriculaPublicaUseCase matriculasPublicas;

    @InjectMocks private TesoreriaService service;

    @Test
    void rechazaSimulacionConForbiddenCuandoSandboxDeshabilitado() {
      ReflectionTestUtils.setField(service, "mercadoPagoSandbox", false);
      var ex =
          assertThrows(
              ResponseStatusException.class,
              () -> service.simularPagoMercadoPago(40L, MetodoPago.YAPE, "YAPE-482910"));
      assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void rechazaEfectivoEnSimuladorEnLinea() {
      ReflectionTestUtils.setField(service, "mercadoPagoSandbox", true);
      assertThrows(
          BusinessException.class,
          () -> service.simularPagoMercadoPago(40L, MetodoPago.EFECTIVO, "EF-01"));
    }

    @Test
    void simulaPagoTarjetaYEmiteComprobanteCuandoSandboxActivo() {
      ReflectionTestUtils.setField(service, "mercadoPagoSandbox", true);
      var ob =
          ObligacionPago.builder()
              .id(40L)
              .matriculaId(91L)
              .conceptoId((short) 2)
              .tipoConcepto(TipoConcepto.PENSION)
              .numeroCuota((short) 8)
              .descripcion("Pensión Noviembre")
              .fechaVencimiento(LocalDate.of(2026, 11, 10))
              .montoBase(new BigDecimal("420.00"))
              .montoMora(BigDecimal.ZERO)
              .montoDescuento(BigDecimal.ZERO)
              .totalPagado(BigDecimal.ZERO)
              .saldoPendiente(new BigDecimal("420.00"))
              .estado(EstadoObligacion.PENDIENTE)
              .build();
      when(obligaciones.buscarPorId(40L)).thenReturn(Optional.of(ob));
      when(obligaciones.bloquearPorId(40L)).thenReturn(Optional.of(ob));
      when(actor.tieneRol("APODERADO")).thenReturn(false);
      when(pagos.guardar(any()))
          .thenAnswer(
              inv -> {
                PagoTransaccion p = inv.getArgument(0);
                p.setId(701L);
                return p;
              });
      when(comprobantes.emitirAutomaticoParaPago(701L, TipoComprobante.BOLETA, "B001"))
          .thenReturn(
              new ComprobanteResponseDto(
                  1L,
                  701L,
                  TipoComprobante.BOLETA,
                  "B001",
                  1,
                  OffsetDateTime.now(),
                  new BigDecimal("420.00"),
                  EstadoComprobante.EMITIDO,
                  null,
                  null,
                  null));
      var dto =
          new TransaccionResponseDto(
              701L,
              40L,
              PasarelaProveedor.MERCADO_PAGO,
              "SIM-OBL-40-1",
              MetodoPago.TARJETA_CREDITO,
              new BigDecimal("420.00"),
              OffsetDateTime.now(),
              EstadoPago.APROBADO,
              null);
      when(mapper.toResponse(any(PagoTransaccion.class))).thenReturn(dto);

      var resp = service.simularPagoMercadoPago(40L, MetodoPago.TARJETA_CREDITO, "TC-4557-8899");

      assertEquals(701L, resp.id());
      assertEquals(EstadoPago.APROBADO, resp.estadoPago());
      var pagoCaptor = ArgumentCaptor.forClass(PagoTransaccion.class);
      verify(pagos).guardar(pagoCaptor.capture());
      assertEquals(EstadoPago.APROBADO, pagoCaptor.getValue().getEstadoPago());
      assertEquals(MetodoPago.TARJETA_CREDITO, pagoCaptor.getValue().getMetodoPago());
      assertEquals(new BigDecimal("420.00"), pagoCaptor.getValue().getMontoPagado());
      assertTrue(pagoCaptor.getValue().getPasarelaTransaccionId().startsWith("SIM-TARJETA_CREDITO-"));
      verify(comprobantes).emitirAutomaticoParaPago(701L, TipoComprobante.BOLETA, "B001");
    }

    @Test
    void alCrearNuevoConceptoActivoGeneraCargosAutomaticosParaMatriculasConfirmadas() {
      var conceptoGuardado = ConceptoCobro.builder()
          .id((short) 15)
          .codigo("EXTRA_TEST")
          .nombre("Taller Robótica")
          .tipoConcepto(TipoConcepto.OTRO)
          .montoSugerido(new BigDecimal("85.00"))
          .activo(true)
          .build();
      when(conceptos.guardar(any())).thenReturn(conceptoGuardado);
      when(mapper.toResponse(conceptoGuardado))
          .thenReturn(new ConceptoCobroResponseDto((short) 15, "EXTRA_TEST", "Taller Robótica", TipoConcepto.OTRO, new BigDecimal("85.00"), true, (short) 20));
      when(anios.listar()).thenReturn(List.of(
          AnioLectivo.builder().id((short) 1).anio((short) 2026).abierto(true).build()));
      var matConfirmada = Matricula.builder()
          .id(301L).anioLectivoId((short) 1).seccionId(10).estadoMatricula(EstadoMatricula.MATRICULADO).build();
      var matCancelada = Matricula.builder()
          .id(302L).anioLectivoId((short) 1).seccionId(10).estadoMatricula(EstadoMatricula.CANCELADA).build();
      when(matriculas.listar()).thenReturn(List.of(matConfirmada, matCancelada));
      when(obligaciones.buscarPorMatriculaIds(List.of(301L))).thenReturn(List.of());
      when(obligaciones.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

      var resp = service.guardarConceptoExtraordinario(
          null,
          new CobroExtraordinarioRequest.Concepto("Taller Robótica", TipoConcepto.OTRO, new BigDecimal("85.00"), true, LocalDate.of(2026, 12, 15)));

      assertEquals((short) 15, resp.id());
      var cap = ArgumentCaptor.forClass(ObligacionPago.class);
      verify(obligaciones).guardar(cap.capture());
      assertEquals(301L, cap.getValue().getMatriculaId());
      assertEquals((short) 15, cap.getValue().getConceptoId());
      assertEquals(new BigDecimal("85.00"), cap.getValue().getMontoBase());
      assertEquals(LocalDate.of(2026, 12, 15), cap.getValue().getFechaVencimiento());
    }

    @Test
    void adelantoMercadoPagoCreaPreferenciaTotalYProcesaLoteEnSandbox() {
      ReflectionTestUtils.setField(service, "mercadoPagoSandbox", true);
      var o1 = ObligacionPago.builder().id(41L).matriculaId(91L).conceptoId((short) 2)
          .tipoConcepto(TipoConcepto.PENSION).numeroCuota((short) 9).descripcion("Pensión Noviembre")
          .montoBase(new BigDecimal("120.00")).montoMora(BigDecimal.ZERO).montoDescuento(BigDecimal.ZERO)
          .totalPagado(BigDecimal.ZERO).saldoPendiente(new BigDecimal("120.00")).estado(EstadoObligacion.PENDIENTE).build();
      var o2 = ObligacionPago.builder().id(42L).matriculaId(91L).conceptoId((short) 2)
          .tipoConcepto(TipoConcepto.PENSION).numeroCuota((short) 10).descripcion("Pensión Diciembre")
          .montoBase(new BigDecimal("120.00")).montoMora(BigDecimal.ZERO).montoDescuento(BigDecimal.ZERO)
          .totalPagado(BigDecimal.ZERO).saldoPendiente(new BigDecimal("120.00")).estado(EstadoObligacion.PENDIENTE).build();

      when(obligaciones.buscarPorId(41L)).thenReturn(Optional.of(o1));
      when(obligaciones.buscarPorId(42L)).thenReturn(Optional.of(o2));
      when(obligaciones.bloquearPorId(41L)).thenReturn(Optional.of(o1));
      when(obligaciones.bloquearPorId(42L)).thenReturn(Optional.of(o2));
      when(actor.tieneRol("APODERADO")).thenReturn(false);

      var prefEsperada = new PreferenciaMercadoPagoResponseDto("PREF-LOTE", "https://www.mercadopago.com.pe/checkout/lote", null, "pub");
      when(mercadoPagoPort.crearPreferenciaResumen(any(), any(), any(), any(), any())).thenReturn(prefEsperada);

      var pref = service.crearPreferenciaAdelantoMercadoPago(
          new CobroExtraordinarioRequest.PreferenciaAdelanto(List.of(41L, 42L), null, null));
      assertEquals("PREF-LOTE", pref.preferenceId());
      verify(mercadoPagoPort).crearPreferenciaResumen(
          "ADELANTO-41-42", new BigDecimal("240.00"), "Pago adelantado (2 cuotas)", null, null);

      when(pagos.guardar(any())).thenAnswer(inv -> {
        PagoTransaccion p = inv.getArgument(0);
        p.setId(p.getObligacionPagoId() + 800L);
        return p;
      });
      when(comprobantes.emitirAutomaticoParaPago(any(), any(), any()))
          .thenReturn(new ComprobanteResponseDto(1L, 841L, TipoComprobante.BOLETA, "B001", 1, OffsetDateTime.now(), new BigDecimal("120.00"), EstadoComprobante.EMITIDO, null, null, null));
      when(mapper.toResponse(any(PagoTransaccion.class))).thenAnswer(inv -> {
        PagoTransaccion p = inv.getArgument(0);
        return new TransaccionResponseDto(p.getId(), p.getObligacionPagoId(), PasarelaProveedor.MERCADO_PAGO,
            p.getPasarelaTransaccionId(), p.getMetodoPago(), p.getMontoPagado(), OffsetDateTime.now(), EstadoPago.APROBADO, null);
      });

      var resultados = service.simularPagoAdelantoMercadoPago(
          new CobroExtraordinarioRequest.SimularPagoAdelanto(List.of(41L, 42L), MetodoPago.YAPE, "YAPE-998811"));
      assertEquals(2, resultados.size());
      verify(comprobantes).emitirAutomaticoParaPago(841L, TipoComprobante.BOLETA, "B001");
      verify(comprobantes).emitirAutomaticoParaPago(842L, TipoComprobante.BOLETA, "B001");
    }
  }
}
