package com.colegio.shuji.matricula.application.service;

import static com.colegio.shuji.shared.domain.model.Reglas.exigir;
import static com.colegio.shuji.shared.domain.model.Reglas.requerido;

import com.colegio.shuji.academico.application.port.out.AnioLectivoRepositoryPort;
import com.colegio.shuji.academico.application.port.out.SeccionRepositoryPort;
import com.colegio.shuji.matricula.application.dto.in.ConfirmarMatriculaRequestDto;
import com.colegio.shuji.matricula.application.dto.in.SolicitarMatriculaRequestDto;
import com.colegio.shuji.matricula.application.dto.out.FichaMatriculaResponseDto;
import com.colegio.shuji.matricula.application.dto.out.MatriculaResponseDto;
import com.colegio.shuji.matricula.application.mapper.MatriculaMapper;
import com.colegio.shuji.matricula.application.port.in.LiberarReservasVencidasUseCase;
import com.colegio.shuji.matricula.application.port.in.ProcesarMatriculaUseCase;
import com.colegio.shuji.matricula.application.port.out.ApoderadoRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.EstudianteApoderadoRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.EstudianteRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.MatriculaRepositoryPort;
import com.colegio.shuji.matricula.domain.enums.EstadoMatricula;
import com.colegio.shuji.shared.domain.exception.BusinessException;
import com.colegio.shuji.tesoreria.application.port.out.ObligacionRepositoryPort;
import com.colegio.shuji.tesoreria.domain.enums.TipoConcepto;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class MatriculaService implements ProcesarMatriculaUseCase, LiberarReservasVencidasUseCase {
  private final MatriculaMapper mapper;
  private final MatriculaRepositoryPort matriculas;
  private final EstudianteRepositoryPort estudiantes;
  private final ApoderadoRepositoryPort apoderados;
  private final EstudianteApoderadoRepositoryPort vinculos;
  private final ObligacionRepositoryPort obligaciones;
  private final SeccionRepositoryPort secciones;
  private final AnioLectivoRepositoryPort anios;

  public MatriculaResponseDto solicitar(SolicitarMatriculaRequestDto r) {
    requerido(anios.bloquearPorId(r.anioLectivoId())).verificarAbierto();
    var s = requerido(secciones.bloquearPorId(r.seccionId()));
    exigir(s.getAnioLectivoId().equals(r.anioLectivoId()), "Sección de otro año");
    var e = requerido(estudiantes.bloquearPorId(r.estudianteId()));
    exigir(Boolean.TRUE.equals(e.getActivo()), "Estudiante inactivo");
    if (matriculas.buscarPorEstudianteId(r.estudianteId()).stream()
        .anyMatch(m -> m.getAnioLectivoId().equals(r.anioLectivoId())))
      throw new com.colegio.shuji.matricula.domain.exception.EstudianteYaMatriculadoException(
          "Ya existe matrícula para este año");
    var m = mapper.toDomain(r);
    m.iniciarSolicitud();
    if (r.reservaExpiraAt() != null) {
      s.verificarCupo();
      m.reservar(r.reservaExpiraAt());
    }
    return mapper.toResponse(matriculas.guardar(m));
  }

  public MatriculaResponseDto confirmar(ConfirmarMatriculaRequestDto r) {
    var snapshot = requerido(matriculas.buscarPorId(r.matriculaId()));
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
            .filter(m -> !m.getReservaExpiraAt().isAfter(ahora))
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
    var relaciones = vinculos.buscarPorEstudianteId(m.getEstudianteId());
    var apoderadosMap =
        apoderados.buscarPorIds(relaciones.stream().map(v -> v.getApoderadoId()).toList()).stream()
            .collect(java.util.stream.Collectors.toMap(
                com.colegio.shuji.matricula.domain.model.Apoderado::getId,
                java.util.function.Function.identity()));
    return new FichaMatriculaResponseDto(
        mapper.toResponse(m),
        mapper.toResponse(requerido(estudiantes.buscarPorId(m.getEstudianteId()))),
        relaciones.stream()
            .map(v -> mapper.toResponse(requerido(java.util.Optional.ofNullable(apoderadosMap.get(v.getApoderadoId())))))
            .toList());
  }

  @Transactional(readOnly = true)
  public java.util.List<MatriculaResponseDto> listarPorSeccion(Integer seccionId, int page, int size) {
    var todas = matriculas.buscarPorSeccionId(seccionId);
    if (todas == null || todas.isEmpty()) {
      return java.util.List.of();
    }
    int start = Math.min(Math.max(0, page) * Math.max(1, size), todas.size());
    int end = Math.min(start + Math.max(1, size), todas.size());
    return todas.subList(start, end).stream().map(mapper::toResponse).toList();
  }
}
