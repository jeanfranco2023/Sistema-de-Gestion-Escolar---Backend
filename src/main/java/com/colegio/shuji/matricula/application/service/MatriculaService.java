package com.colegio.shuji.matricula.application.service;

import com.colegio.shuji.matricula.domain.service.PoliticaAdmision;
import static com.colegio.shuji.shared.domain.model.Reglas.exigir;
import static com.colegio.shuji.shared.domain.model.Reglas.requerido;

import com.colegio.shuji.academico.application.port.out.AnioLectivoRepositoryPort;
import com.colegio.shuji.academico.application.port.out.SeccionRepositoryPort;
import com.colegio.shuji.matricula.application.dto.in.ConfirmarMatriculaRequestDto;
import com.colegio.shuji.matricula.application.dto.in.CambiarEstadoMatriculaRequestDto;
import com.colegio.shuji.matricula.application.dto.in.SolicitarMatriculaRequestDto;
import com.colegio.shuji.matricula.application.dto.out.ApoderadoFichaResponseDto;
import com.colegio.shuji.matricula.application.dto.out.FichaMatriculaResponseDto;
import com.colegio.shuji.matricula.application.dto.out.EstadoPagoMatriculaFicha;
import com.colegio.shuji.matricula.application.dto.out.MatriculaResponseDto;
import com.colegio.shuji.matricula.application.mapper.MatriculaMapper;
import com.colegio.shuji.matricula.application.port.in.LiberarReservasVencidasUseCase;
import com.colegio.shuji.matricula.application.port.in.ProcesarMatriculaUseCase;
import com.colegio.shuji.matricula.application.port.out.ApoderadoRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.EstudianteApoderadoRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.EstudianteRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.MatriculaRepositoryPort;
import com.colegio.shuji.matricula.domain.enums.EstadoMatricula;
import com.colegio.shuji.matricula.domain.model.Apoderado;
import com.colegio.shuji.matricula.domain.model.Matricula;
import com.colegio.shuji.shared.domain.exception.BusinessException;
import com.colegio.shuji.tesoreria.application.port.out.ObligacionRepositoryPort;
import com.colegio.shuji.tesoreria.domain.model.ObligacionPago;
import com.colegio.shuji.tesoreria.domain.enums.TipoConcepto;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class MatriculaService implements ProcesarMatriculaUseCase, LiberarReservasVencidasUseCase {
  private static final Set<String> OBSERVACIONES_PAGO_OBSOLETAS =
      Set.of(
          "pendiente de pago", "pago pendiente", "matricula pendiente de pago",
          "pendiente de pago de matricula");

  private final MatriculaMapper mapper;
  private final MatriculaRepositoryPort matriculas;
  private final EstudianteRepositoryPort estudiantes;
  private final ApoderadoRepositoryPort apoderados;
  private final EstudianteApoderadoRepositoryPort vinculos;
  private final ObligacionRepositoryPort obligaciones;
  private final SeccionRepositoryPort secciones;
  private final AnioLectivoRepositoryPort anios;

  public MatriculaResponseDto solicitar(SolicitarMatriculaRequestDto r) {
    var anio = requerido(anios.bloquearPorId(r.anioLectivoId()));
    anio.verificarAbierto();
    exigir(PoliticaAdmision.admiteNuevasMatriculas(anio.getAnio()),
        PoliticaAdmision.mensajeAnioNoDisponible());
    var e = requerido(estudiantes.bloquearPorId(r.estudianteId()));
    exigir(Boolean.TRUE.equals(e.getActivo()), "Estudiante inactivo");
    if (matriculas.existePorEstudianteIdYAnioLectivoId(r.estudianteId(), r.anioLectivoId()))
      throw new com.colegio.shuji.matricula.domain.exception.EstudianteYaMatriculadoException(
          "El estudiante ya tiene una matrícula registrada para el año lectivo seleccionado.");
    var s = requerido(secciones.bloquearPorId(r.seccionId()));
    exigir(s.getAnioLectivoId().equals(r.anioLectivoId()), "Sección de otro año");
    s.verificarCupo();
    var m = mapper.toDomain(r);
    m.iniciarSolicitud();
    if (r.fechaExpiracionReserva() != null) {
      m.reservar(r.fechaExpiracionReserva());
    }
    return mapper.toResponse(matriculas.guardar(m));
  }

  public MatriculaResponseDto confirmar(ConfirmarMatriculaRequestDto r) {
    var snapshot = requerido(matriculas.buscarPorId(r.matriculaId()));
    exigir(snapshot.getEstadoMatricula() != EstadoMatricula.PENDIENTE_ASIGNACION
        && snapshot.getSeccionId() != null,
        "Secretaría debe asignar el grado y aula antes de confirmar la matrícula.");
    requerido(anios.bloquearPorId(snapshot.getAnioLectivoId())).verificarAbierto();
    var s = requerido(secciones.bloquearPorId(snapshot.getSeccionId()));
    var m = requerido(matriculas.bloquearPorId(r.matriculaId()));
    if (m.getEstadoMatricula() == EstadoMatricula.SOLICITADA) s.verificarCupo();
    exigir(
        vinculos.buscarPorEstudianteId(m.getEstudianteId()).stream()
            .anyMatch(v -> Boolean.TRUE.equals(v.getEsResponsableEconomico())),
        "Se requiere responsable económico");
    if (m.getEstadoMatricula() != EstadoMatricula.MATRICULADO) {
      validarPagoMatricula(m.getId());
    }
    m.confirmar(OffsetDateTime.now());
    return mapper.toResponse(matriculas.guardar(m));
  }

  public MatriculaResponseDto cambiarEstado(Long matriculaId, CambiarEstadoMatriculaRequestDto r) {
    var m = requerido(matriculas.bloquearPorId(matriculaId));
    var motivo = r.motivo().trim();
    exigir(!motivo.isBlank(), "El motivo del cambio de estado es obligatorio");

    switch (r.estado()) {
      case TRASLADADO -> {
        exigir(m.getEstadoMatricula() == EstadoMatricula.MATRICULADO,
            "Solo una matrícula confirmada puede registrarse como trasladada");
        m.trasladar(motivo);
      }
      case RETIRADO -> {
        exigir(m.getEstadoMatricula() == EstadoMatricula.MATRICULADO,
            "Solo un estudiante matriculado puede registrarse como retirado");
        m.retirar(motivo);
      }
      case CANCELADA -> {
        exigir(m.getEstadoMatricula() == EstadoMatricula.SOLICITADA
                || m.getEstadoMatricula() == EstadoMatricula.RESERVADA_TEMPORAL,
            "Solo una solicitud o reserva puede cancelarse");
        m.cancelar(motivo);
      }
      default -> throw new BusinessException("El estado solicitado no es una transición permitida");
    }

    return mapper.toResponse(matriculas.guardar(m));
  }

  private void validarPagoMatricula(Long matriculaId) {
    var cuotasMatricula =
        obligaciones.buscarPorMatriculaId(matriculaId).stream()
            .filter(
                o ->
                    o.getTipoConcepto() == TipoConcepto.MATRICULA
                        && o.getNumeroCuota() != null
                        && o.getNumeroCuota() == 0)
            .map(o -> requerido(obligaciones.bloquearPorId(o.getId())))
            .toList();

    exigir(
        !cuotasMatricula.isEmpty(),
        "Genere la obligación de matrícula en Tesorería antes de confirmarla.");
    exigir(
        cuotasMatricula.stream()
            .allMatch(
                o ->
                    o.estaPagadoTotal()
                        && o.saldo().compareTo(java.math.BigDecimal.ZERO) == 0),
        "La cuota de matrícula debe estar pagada por completo antes de confirmar.");
  }

  public int liberarVencidas() {
    int total = 0;
    var ahora = OffsetDateTime.now();
    var vencidas =
        matriculas.buscarPorEstadoMatricula(EstadoMatricula.RESERVADA_TEMPORAL).stream()
            .filter(m -> !m.getFechaExpiracionReserva().isAfter(ahora))
            .toList();
    secciones.bloquearPorIds(vencidas.stream().map(m -> m.getSeccionId()).distinct().toList());
    var bloqueadas =
        matriculas.bloquearPorIds(vencidas.stream().map(m -> m.getId()).toList());
    for (var actual : bloqueadas) {
      if (actual.liberarSiVencida(ahora)) {
        matriculas.guardar(actual);
        total++;
      }
    }
    return total;
  }

  @Transactional(readOnly = true)
  public FichaMatriculaResponseDto ficha(Long id) {
    var m = requerido(matriculas.buscarPorId(id));
    var estadoPago = estadoPagoMatricula(m);
    var matriculaDto = mapper.toResponse(m);
    if (esObservacionPagoObsoleta(m.getObservaciones())) {
      matriculaDto = new MatriculaResponseDto(
          matriculaDto.id(), matriculaDto.anioLectivoId(), matriculaDto.estudianteId(),
          matriculaDto.seccionId(), matriculaDto.fechaMatricula(), matriculaDto.estadoMatricula(),
          matriculaDto.fechaExpiracionReserva(), null, estadoPago,
          matriculaDto.modalidadIngreso(), matriculaDto.colegioProcedencia());
    } else {
      matriculaDto = conEstadoPago(matriculaDto, estadoPago);
    }
    var relaciones = vinculos.buscarPorEstudianteId(m.getEstudianteId());
    var apoderadosMap =
        apoderados.buscarPorIds(relaciones.stream().map(v -> v.getApoderadoId()).toList()).stream()
            .collect(java.util.stream.Collectors.toMap(
                Apoderado::getId,
                java.util.function.Function.identity()));
    return new FichaMatriculaResponseDto(
        matriculaDto,
        mapper.toResponse(requerido(estudiantes.buscarPorId(m.getEstudianteId()))),
        relaciones.stream()
            .map(v -> new ApoderadoFichaResponseDto(
                mapper.toResponse(requerido(java.util.Optional.ofNullable(apoderadosMap.get(v.getApoderadoId())))),
                v.getParentesco(),
                v.getEsResponsableEconomico()))
            .toList(),
        estadoPago);
  }

  private EstadoPagoMatriculaFicha estadoPagoMatricula(Matricula matricula) {
    List<ObligacionPago> cuotas = obligaciones.buscarPorMatriculaId(matricula.getId()).stream()
        .filter(o -> o.getTipoConcepto() == TipoConcepto.MATRICULA
            && o.getNumeroCuota() != null && o.getNumeroCuota() == 0)
        .toList();
    return estadoPagoMatricula(matricula, cuotas);
  }
  private EstadoPagoMatriculaFicha estadoPagoMatriculaUnused(Matricula matricula, List<ObligacionPago> cuotas) {
    var filtradas = cuotas.stream()

        .filter(o -> o.getTipoConcepto() == TipoConcepto.MATRICULA
            && o.getNumeroCuota() != null && o.getNumeroCuota() == 0)
        .toList();
    if (cuotas.isEmpty()) return estadoPagoMatricula(matricula, cuotas);
    if (cuotas.stream().allMatch(o -> o.estaPagadoTotal() && o.saldo().signum() == 0))
      return EstadoPagoMatriculaFicha.PAGADO;
    if (cuotas.stream().anyMatch(o -> o.getTotalPagado() != null
        && o.getTotalPagado().compareTo(BigDecimal.ZERO) > 0))
      return EstadoPagoMatriculaFicha.PARCIAL;
    return EstadoPagoMatriculaFicha.PENDIENTE;
  }

  private List<MatriculaResponseDto> conEstadosPago(List<Matricula> registros) {
    if (registros.isEmpty()) return List.of();
    var ids = registros.stream().map(Matricula::getId).toList();
    Map<Long, List<ObligacionPago>> cuotasPorMatricula = obligaciones.buscarPorMatriculaIds(ids).stream()
        .filter(o -> o.getTipoConcepto() == TipoConcepto.MATRICULA
            && o.getNumeroCuota() != null && o.getNumeroCuota() == 0)
        .collect(Collectors.groupingBy(ObligacionPago::getMatriculaId));
    return registros.stream().map(registro -> {
      var response = mapper.toResponse(registro);
      return conEstadoPago(response, estadoPagoMatricula(registro, cuotasPorMatricula.get(registro.getId())));
    }).toList();
  }

  private EstadoPagoMatriculaFicha estadoPagoMatricula(Matricula matricula, List<ObligacionPago> cuotas) {
    if (matricula != null && matricula.getEstadoMatricula() == EstadoMatricula.MATRICULADO) return EstadoPagoMatriculaFicha.PAGADO;
    if (cuotas == null || cuotas.isEmpty()) return EstadoPagoMatriculaFicha.PENDIENTE;
    if (cuotas.stream().allMatch(o -> o.estaPagadoTotal() && o.saldo().signum() == 0))
      return EstadoPagoMatriculaFicha.PAGADO;
    if (cuotas.stream().anyMatch(o -> o.getTotalPagado() != null
        && o.getTotalPagado().compareTo(BigDecimal.ZERO) > 0))
      return EstadoPagoMatriculaFicha.PARCIAL;
    return EstadoPagoMatriculaFicha.PENDIENTE;
  }

  private MatriculaResponseDto conEstadoPago(
      MatriculaResponseDto matricula, EstadoPagoMatriculaFicha estadoPago) {
    return new MatriculaResponseDto(
        matricula.id(), matricula.anioLectivoId(), matricula.estudianteId(), matricula.seccionId(),
        matricula.fechaMatricula(), matricula.estadoMatricula(), matricula.fechaExpiracionReserva(),
        matricula.observaciones(), estadoPago,
        matricula.modalidadIngreso(), matricula.colegioProcedencia());
  }

  private boolean esObservacionPagoObsoleta(String observaciones) {
    if (observaciones == null) return false;
    var normalizada = Normalizer.normalize(
        observaciones.strip().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "");
    int fin = normalizada.length();
    while (fin > 0 && ".!;:,".indexOf(normalizada.charAt(fin - 1)) >= 0) fin--;
    normalizada = normalizada.substring(0, fin);
    return OBSERVACIONES_PAGO_OBSOLETAS.contains(normalizada);
  }

  @Transactional(readOnly = true)
  public java.util.List<MatriculaResponseDto> listarPorSeccion(Integer seccionId, int page, int size) {
    var todas = matriculas.buscarPorSeccionId(seccionId);
    if (todas == null || todas.isEmpty()) {
      return java.util.List.of();
    }
    int start = Math.min(Math.max(0, page) * Math.max(1, size), todas.size());
    int end = Math.min(start + Math.max(1, size), todas.size());
    return conEstadosPago(todas.subList(start, end));
  }

  @Transactional(readOnly = true)
  public java.util.List<MatriculaResponseDto> listarPorAnio(Short anioLectivoId) {
    return conEstadosPago(matriculas.buscarPorAnioLectivoId(anioLectivoId));
  }

  @Transactional(readOnly = true)
  public java.util.List<Long> listarEstudianteIdsMatriculados(Short anioLectivoId) {
    return matriculas.buscarEstudianteIdsPorAnioLectivoId(anioLectivoId);
  }
}
