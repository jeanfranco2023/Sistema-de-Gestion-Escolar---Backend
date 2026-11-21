package com.colegio.shuji;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.colegio.shuji.academico.application.port.out.AnioLectivoRepositoryPort;
import com.colegio.shuji.academico.domain.model.AnioLectivo;
import com.colegio.shuji.matricula.application.port.in.GestionarPagoMatriculaPublicaUseCase;
import com.colegio.shuji.matricula.application.port.out.ApoderadoRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.EstudianteApoderadoRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.EstudianteRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.MatriculaRepositoryPort;
import com.colegio.shuji.matricula.domain.enums.EstadoMatricula;
import com.colegio.shuji.matricula.domain.model.Estudiante;
import com.colegio.shuji.matricula.domain.model.EstudianteApoderado;
import com.colegio.shuji.matricula.domain.model.Matricula;
import com.colegio.shuji.shared.application.port.out.ActorActualPort;
import com.colegio.shuji.tesoreria.application.dto.in.GenerarObligacionMatriculaRequestDto;
import com.colegio.shuji.tesoreria.application.dto.in.RegistrarPagoCajaRequestDto;
import com.colegio.shuji.tesoreria.application.dto.out.TransaccionResponseDto;
import com.colegio.shuji.tesoreria.domain.enums.MetodoPago;
import com.colegio.shuji.tesoreria.domain.enums.TipoComprobante;
import com.colegio.shuji.tesoreria.domain.model.PagoTransaccion;
import org.springframework.security.access.AccessDeniedException;
import com.colegio.shuji.tesoreria.application.dto.out.ObligacionResponseDto;
import com.colegio.shuji.tesoreria.application.mapper.TesoreriaMapper;
import com.colegio.shuji.tesoreria.application.port.in.EmitirComprobanteUseCase;
import com.colegio.shuji.tesoreria.application.port.out.ConceptoCobroRepositoryPort;
import com.colegio.shuji.tesoreria.application.port.out.MercadoPagoPort;
import com.colegio.shuji.tesoreria.application.port.out.ObligacionRepositoryPort;
import com.colegio.shuji.tesoreria.application.port.out.PagoRepositoryPort;
import com.colegio.shuji.tesoreria.application.port.out.VerificarPagoPort;
import com.colegio.shuji.tesoreria.application.service.TesoreriaService;
import com.colegio.shuji.tesoreria.domain.enums.EstadoObligacion;
import com.colegio.shuji.tesoreria.domain.enums.TipoConcepto;
import com.colegio.shuji.tesoreria.domain.model.ConceptoCobro;
import com.colegio.shuji.tesoreria.domain.model.ObligacionPago;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.colegio.shuji.shared.domain.exception.BusinessException;

class TesoreriaObligacionMatriculaTest {
  private final TesoreriaMapper mapper = mock(TesoreriaMapper.class);
  private final ObligacionRepositoryPort obligaciones = mock(ObligacionRepositoryPort.class);
  private final PagoRepositoryPort pagos = mock(PagoRepositoryPort.class);
  private final ConceptoCobroRepositoryPort conceptos = mock(ConceptoCobroRepositoryPort.class);
  private final MatriculaRepositoryPort matriculas = mock(MatriculaRepositoryPort.class);
  private final EstudianteRepositoryPort estudiantes = mock(EstudianteRepositoryPort.class);
  private final AnioLectivoRepositoryPort anios = mock(AnioLectivoRepositoryPort.class);
  private final VerificarPagoPort verificador = mock(VerificarPagoPort.class);
  private final MercadoPagoPort mercadoPago = mock(MercadoPagoPort.class);
  private final EmitirComprobanteUseCase comprobantes = mock(EmitirComprobanteUseCase.class);
  private final ActorActualPort actor = mock(ActorActualPort.class);
  private final ApoderadoRepositoryPort apoderados = mock(ApoderadoRepositoryPort.class);
  private final EstudianteApoderadoRepositoryPort vinculos = mock(EstudianteApoderadoRepositoryPort.class);
  private final GestionarPagoMatriculaPublicaUseCase matriculasPublicas =
      mock(GestionarPagoMatriculaPublicaUseCase.class);
  private final TesoreriaService service = new TesoreriaService(
      mapper, obligaciones, pagos, conceptos, matriculas, estudiantes, anios, verificador,
      mercadoPago, comprobantes, actor, apoderados, vinculos, matriculasPublicas);

  private final Matricula matricula = Matricula.builder()
      .id(281L).estudianteId(90L).anioLectivoId((short) 2026)
      .estadoMatricula(EstadoMatricula.SOLICITADA).build();
  private final ConceptoCobro concepto = ConceptoCobro.builder()
      .id((short) 4).codigo("MAT").nombre("Matrícula").tipoConcepto(TipoConcepto.MATRICULA)
      .montoSugerido(new BigDecimal("350.00")).build();

  @BeforeEach
  void prepararDatosValidos() {
    when(matriculas.bloquearPorId(281L)).thenReturn(Optional.of(matricula));
    when(anios.buscarPorId((short) 2026)).thenReturn(Optional.of(
        AnioLectivo.builder().id((short) 2026).anio((short) 2026).abierto(true).build()));
    when(vinculos.buscarPorEstudianteId(90L)).thenReturn(List.of(
        EstudianteApoderado.builder().estudianteId(90L).esResponsableEconomico(true).build()));
    when(conceptos.listar()).thenReturn(List.of(concepto));
    when(matriculas.buscarPorIds(anyCollection())).thenReturn(List.of(matricula));
    when(estudiantes.buscarPorIds(anyCollection())).thenReturn(List.of(
        Estudiante.builder().id(90L).nombres("Ana").apellidoPaterno("Pérez").apellidoMaterno("Díaz").build()));
    when(mapper.toResponse(any(ObligacionPago.class)))
        .thenAnswer(inv -> toResponse(inv.getArgument(0)));
  }

  @Test
  void auxiliarSoloPuedeCobrarUnExtraordinarioYElPagoCompletoEmiteComprobante() {
    when(actor.tieneRol("AUXILIAR")).thenReturn(true);
    when(actor.usuarioId()).thenReturn(41L);
    when(matriculas.buscarPorId(281L)).thenReturn(Optional.of(matricula));
    var cargo = ObligacionPago.programar(281L, concepto, (short) 0, LocalDate.of(2026, 12, 10));
    when(obligaciones.bloquearPorId(900L)).thenReturn(Optional.of(cargo));
    var total = new BigDecimal("350.00");
    var solicitud = new RegistrarPagoCajaRequestDto(900L, "CAJA-OPERACION", MetodoPago.EFECTIVO, total);
    when(mapper.toDomain(any(RegistrarPagoCajaRequestDto.class))).thenAnswer(inv -> {
      RegistrarPagoCajaRequestDto r = inv.getArgument(0);
      return PagoTransaccion.builder().obligacionPagoId(r.obligacionPagoId())
          .pasarelaTransaccionId(r.pasarelaTransaccionId()).metodoPago(r.metodoPago())
          .montoPagado(r.montoPagado()).build();
    });

    assertThrows(AccessDeniedException.class, () -> service.registrarCaja(solicitud));
    cargo.setTipoConcepto(TipoConcepto.OTRO);
    var parcial = new RegistrarPagoCajaRequestDto(900L, "CAJA-PARCIAL", MetodoPago.EFECTIVO, BigDecimal.ONE);
    assertThrows(BusinessException.class, () -> service.registrarCaja(parcial));

    when(pagos.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
    when(mapper.toResponse(any(PagoTransaccion.class))).thenAnswer(inv -> {
      PagoTransaccion p = inv.getArgument(0);
      assertEquals(41L, p.getPayloadWebhook().get("usuarioCajaId"));
      return new TransaccionResponseDto(55L, 900L, p.getPasarelaProveedor(),
          p.getPasarelaTransaccionId(), p.getMetodoPago(), p.getMontoPagado(),
          p.getFechaPago(), p.getEstadoPago(), p.getPayloadWebhook());
    });
    assertEquals(55L, service.registrarCaja(solicitud).id());
    verify(comprobantes).emitirAutomaticoParaPago(55L, TipoComprobante.BOLETA, "B001");
  }

  @Test
  void creaSoloUnaCuotaCeroConElMontoDelConceptoEnReintentos() {
    AtomicReference<ObligacionPago> guardada = new AtomicReference<>();
    when(obligaciones.buscarPorMatriculaId(281L)).thenAnswer(inv ->
        guardada.get() == null ? List.of() : List.of(guardada.get()));
    when(obligaciones.guardar(any())).thenAnswer(inv -> {
      ObligacionPago nueva = inv.getArgument(0);
      nueva.setId(991L);
      guardada.set(nueva);
      return nueva;
    });

    var request = new GenerarObligacionMatriculaRequestDto(281L, LocalDate.of(2026, 11, 30));
    var primera = service.generarObligacionMatricula(request);
    var reintento = service.generarObligacionMatricula(request);

    assertEquals((short) 0, guardada.get().getNumeroCuota());
    assertEquals(new BigDecimal("350.00"), guardada.get().getMontoBase());
    assertEquals(primera.id(), reintento.id());
    assertEquals((short) 0, primera.numeroCuota());
    assertEquals(LocalDate.of(2026, 11, 30), primera.fechaVencimiento());
    verify(obligaciones, times(1)).guardar(any());
  }

  @Test
  void rechazaSiElCatalogoNoTieneConceptoDeMatricula() {
    when(conceptos.listar()).thenReturn(List.of(ConceptoCobro.builder()
        .id((short) 8).tipoConcepto(TipoConcepto.PENSION)
        .montoSugerido(new BigDecimal("500.00")).build()));

    assertThrows(BusinessException.class, () -> service.generarObligacionMatricula(
        new GenerarObligacionMatriculaRequestDto(281L, LocalDate.of(2026, 11, 30))));
    verify(obligaciones, times(0)).guardar(any());
  }

  private static ObligacionResponseDto toResponse(ObligacionPago obligacion) {
    return new ObligacionResponseDto(
        obligacion.getId(), obligacion.getMatriculaId(), obligacion.getConceptoId(),
        obligacion.getTipoConcepto(), obligacion.getNumeroCuota(), obligacion.getDescripcion(),
        obligacion.getFechaVencimiento(), obligacion.getMontoBase(), obligacion.getMontoMora(),
        obligacion.getMontoDescuento(), obligacion.getTotalPagado(), obligacion.saldo(),
        obligacion.getEstado(), null);
  }
}
