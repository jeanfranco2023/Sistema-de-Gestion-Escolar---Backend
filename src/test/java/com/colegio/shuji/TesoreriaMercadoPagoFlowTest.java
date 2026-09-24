package com.colegio.shuji;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.colegio.shuji.academico.application.port.out.AnioLectivoRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.ApoderadoRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.EstudianteApoderadoRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.MatriculaRepositoryPort;
import com.colegio.shuji.matricula.domain.model.Apoderado;
import com.colegio.shuji.matricula.domain.model.EstudianteApoderado;
import com.colegio.shuji.matricula.domain.model.Matricula;
import com.colegio.shuji.shared.application.port.out.ActorActualPort;
import com.colegio.shuji.tesoreria.application.dto.in.CrearPreferenciaMercadoPagoDto;
import com.colegio.shuji.tesoreria.application.dto.out.PreferenciaMercadoPagoResponseDto;
import com.colegio.shuji.tesoreria.application.mapper.TesoreriaMapper;
import com.colegio.shuji.tesoreria.application.port.in.EmitirComprobanteUseCase;
import com.colegio.shuji.tesoreria.application.port.out.ConceptoCobroRepositoryPort;
import com.colegio.shuji.tesoreria.application.port.out.MercadoPagoPort;
import com.colegio.shuji.tesoreria.application.port.out.ObligacionRepositoryPort;
import com.colegio.shuji.tesoreria.application.port.out.PagoRepositoryPort;
import com.colegio.shuji.tesoreria.application.port.out.VerificarPagoPort;
import com.colegio.shuji.tesoreria.application.service.TesoreriaService;
import com.colegio.shuji.tesoreria.domain.enums.EstadoObligacion;
import com.colegio.shuji.tesoreria.domain.model.ConceptoCobro;
import com.colegio.shuji.tesoreria.domain.model.ObligacionPago;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TesoreriaMercadoPagoFlowTest {
  private final TesoreriaMapper mapper = mock(TesoreriaMapper.class);
  private final ObligacionRepositoryPort obligaciones = mock(ObligacionRepositoryPort.class);
  private final ConceptoCobroRepositoryPort conceptos = mock(ConceptoCobroRepositoryPort.class);
  private final MatriculaRepositoryPort matriculas = mock(MatriculaRepositoryPort.class);
  private final MercadoPagoPort mercadoPago = mock(MercadoPagoPort.class);
  private final ActorActualPort actor = mock(ActorActualPort.class);
  private final ApoderadoRepositoryPort apoderados = mock(ApoderadoRepositoryPort.class);
  private final EstudianteApoderadoRepositoryPort vinculos = mock(EstudianteApoderadoRepositoryPort.class);
  private final TesoreriaService service = new TesoreriaService(
      mapper, obligaciones, mock(PagoRepositoryPort.class), conceptos, matriculas,
      mock(AnioLectivoRepositoryPort.class), mock(VerificarPagoPort.class), mercadoPago,
      mock(EmitirComprobanteUseCase.class), actor, apoderados, vinculos);

  @Test
  void apoderadoSoloConsultaObligacionesDeSuResponsabilidadEconomica() {
    when(actor.usuarioId()).thenReturn(9L);
    when(apoderados.buscarPorUsuarioId(9L))
        .thenReturn(List.of(Apoderado.builder().id(7L).usuarioId(9L).build()));
    when(vinculos.buscarPorApoderadoId(7L)).thenReturn(List.of(
        EstudianteApoderado.builder().estudianteId(10L).esResponsableEconomico(true).build(),
        EstudianteApoderado.builder().estudianteId(20L).esResponsableEconomico(false).build()));
    when(matriculas.buscarPorEstudianteId(10L))
        .thenReturn(List.of(Matricula.builder().id(30L).estudianteId(10L).build()));
    when(obligaciones.buscarPorMatriculaIds(List.of(30L)))
        .thenReturn(List.of(ObligacionPago.builder().id(40L).matriculaId(30L).build()));

    assertEquals(1, service.misObligaciones().size());
    verify(matriculas, never()).buscarPorEstudianteId(20L);
  }

  @Test
  void saldoParcialPuedeCrearPreferenciaPorElImporteRestante() {
    var obligacion = ObligacionPago.builder()
        .id(40L).matriculaId(30L).conceptoId((short) 2).numeroCuota((short) 1)
        .montoBase(new BigDecimal("100.00")).montoMora(BigDecimal.ZERO)
        .montoDescuento(BigDecimal.ZERO).totalPagado(new BigDecimal("40.00"))
        .estado(EstadoObligacion.PAGADO_PARCIAL).build();
    var preferencia = new PreferenciaMercadoPagoResponseDto("pref-1", "https://example.com", "https://example.com", "key");
    when(obligaciones.buscarPorId(40L)).thenReturn(Optional.of(obligacion));
    when(conceptos.buscarPorId((short) 2))
        .thenReturn(Optional.of(ConceptoCobro.builder().nombre("Pensión").build()));
    when(mercadoPago.crearPreferencia(any(), anyString(), any(), any())).thenReturn(preferencia);

    assertSame(preferencia, service.crearPreferenciaMercadoPago(new CrearPreferenciaMercadoPagoDto(40L, null, null)));
    verify(mercadoPago).crearPreferencia(obligacion, "Pensión - Cuota 1", null, null);
  }
}
