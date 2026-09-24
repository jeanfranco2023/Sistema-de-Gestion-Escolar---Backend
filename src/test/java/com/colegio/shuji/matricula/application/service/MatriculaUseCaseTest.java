package com.colegio.shuji.matricula.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.colegio.shuji.academico.application.port.out.AnioLectivoRepositoryPort;
import com.colegio.shuji.academico.application.port.out.SeccionRepositoryPort;
import com.colegio.shuji.academico.domain.exception.CupoAgotadoException;
import com.colegio.shuji.academico.domain.model.AnioLectivo;
import com.colegio.shuji.academico.domain.model.Seccion;
import com.colegio.shuji.matricula.application.dto.in.ConfirmarMatriculaRequestDto;
import com.colegio.shuji.matricula.application.mapper.MatriculaMapper;
import com.colegio.shuji.matricula.application.port.out.ApoderadoRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.EstudianteApoderadoRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.EstudianteRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.MatriculaRepositoryPort;
import com.colegio.shuji.matricula.domain.enums.EstadoMatricula;
import com.colegio.shuji.matricula.domain.enums.Parentesco;
import com.colegio.shuji.matricula.domain.model.EstudianteApoderado;
import com.colegio.shuji.matricula.domain.model.Matricula;
import com.colegio.shuji.shared.domain.exception.BusinessException;
import com.colegio.shuji.tesoreria.application.port.out.ObligacionRepositoryPort;
import com.colegio.shuji.tesoreria.domain.enums.EstadoObligacion;
import com.colegio.shuji.tesoreria.domain.enums.TipoConcepto;
import com.colegio.shuji.tesoreria.domain.model.ObligacionPago;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MatriculaUseCaseTest {
  @Mock private MatriculaMapper mapper;
  @Mock private MatriculaRepositoryPort matriculas;
  @Mock private EstudianteRepositoryPort estudiantes;
  @Mock private ApoderadoRepositoryPort apoderados;
  @Mock private EstudianteApoderadoRepositoryPort vinculos;
  @Mock private ObligacionRepositoryPort obligaciones;
  @Mock private SeccionRepositoryPort secciones;
  @Mock private AnioLectivoRepositoryPort anios;

  private MatriculaService useCase;
  private Matricula solicitud;

  @BeforeEach
  void configurar() {
    useCase = new MatriculaService(mapper, matriculas, estudiantes, apoderados, vinculos, obligaciones, secciones, anios);
    solicitud = Matricula.builder().id(10L).anioLectivoId((short) 2).estudianteId(20L)
        .seccionId(30).estadoMatricula(EstadoMatricula.SOLICITADA).build();
    lenient().when(matriculas.buscarPorId(10L)).thenReturn(Optional.of(solicitud));
    lenient().when(anios.bloquearPorId((short) 2)).thenReturn(Optional.of(AnioLectivo.builder().id((short) 2).abierto(true).build()));
    lenient().when(secciones.bloquearPorId(30)).thenReturn(Optional.of(Seccion.builder().id(30).cupoMaximo((short) 25).vacantesOcupadas((short) 24).build()));
    lenient().when(matriculas.bloquearPorId(10L)).thenReturn(Optional.of(solicitud));
    lenient().when(vinculos.buscarPorEstudianteId(20L)).thenReturn(List.of(EstudianteApoderado.builder()
        .estudianteId(20L).apoderadoId(40L).parentesco(Parentesco.MADRE).esResponsableEconomico(true).build()));
    lenient().when(matriculas.guardar(any(Matricula.class))).thenAnswer(call -> call.getArgument(0));
    var cuotaMatricula = ObligacionPago.builder()
        .id(100L).matriculaId(10L).tipoConcepto(TipoConcepto.MATRICULA).numeroCuota((short) 0)
        .montoBase(new BigDecimal("100.00")).montoMora(BigDecimal.ZERO)
        .montoDescuento(BigDecimal.ZERO).totalPagado(new BigDecimal("100.00"))
        .estado(EstadoObligacion.PAGADO_TOTAL).build();
    lenient().when(obligaciones.buscarPorMatriculaId(10L)).thenReturn(List.of(cuotaMatricula));
    lenient().when(obligaciones.bloquearPorId(100L)).thenReturn(Optional.of(cuotaMatricula));
    lenient().when(mapper.toResponse(any(Matricula.class))).thenAnswer(call -> {
      Matricula saved = call.getArgument(0);
      return new com.colegio.shuji.matricula.application.dto.out.MatriculaResponseDto(
          saved.getId(), null, saved.getAnioLectivoId(), saved.getEstudianteId(), saved.getSeccionId(),
          saved.getFechaMatricula(), saved.getEstadoMatricula(), saved.getReservaExpiraAt(), saved.getObservaciones());
    });
  }

  @Test
  void confirmaTransicionDeSolicitadaAMatriculadoCuandoHayCupo() {
    var result = useCase.confirmar(new ConfirmarMatriculaRequestDto(10L));

    assertEquals(EstadoMatricula.MATRICULADO, result.estadoMatricula());
    verify(matriculas).guardar(solicitud);
  }

  @Test
  void impideConfirmarCuandoSeAgotaronLasVacantes() {
    when(secciones.bloquearPorId(30)).thenReturn(Optional.of(Seccion.builder().id(30)
        .cupoMaximo((short) 25).vacantesOcupadas((short) 25).build()));
    clearInvocations(matriculas);

    assertThrows(CupoAgotadoException.class,
        () -> useCase.confirmar(new ConfirmarMatriculaRequestDto(10L)));
    assertEquals(EstadoMatricula.SOLICITADA, solicitud.getEstadoMatricula());
    verify(matriculas, never()).guardar(any(Matricula.class));
  }

  @Test
  void impideConfirmarSiNoSeGeneroLaObligacionDeMatricula() {
    when(obligaciones.buscarPorMatriculaId(10L)).thenReturn(List.of());

    assertThrows(BusinessException.class,
        () -> useCase.confirmar(new ConfirmarMatriculaRequestDto(10L)));
    assertEquals(EstadoMatricula.SOLICITADA, solicitud.getEstadoMatricula());
    verify(matriculas, never()).guardar(any(Matricula.class));
  }

  @Test
  void impideConfirmarSiLaCuotaDeMatriculaNoEstaPagadaCompleta() {
    var cuotaPendiente = ObligacionPago.builder()
        .id(100L).matriculaId(10L).tipoConcepto(TipoConcepto.MATRICULA).numeroCuota((short) 0)
        .montoBase(new BigDecimal("100.00")).montoMora(BigDecimal.ZERO)
        .montoDescuento(BigDecimal.ZERO).totalPagado(new BigDecimal("50.00"))
        .estado(EstadoObligacion.PAGADO_PARCIAL).build();
    when(obligaciones.buscarPorMatriculaId(10L)).thenReturn(List.of(cuotaPendiente));
    when(obligaciones.bloquearPorId(100L)).thenReturn(Optional.of(cuotaPendiente));

    assertThrows(BusinessException.class,
        () -> useCase.confirmar(new ConfirmarMatriculaRequestDto(10L)));
    assertEquals(EstadoMatricula.SOLICITADA, solicitud.getEstadoMatricula());
    verify(matriculas, never()).guardar(any(Matricula.class));
  }
}
