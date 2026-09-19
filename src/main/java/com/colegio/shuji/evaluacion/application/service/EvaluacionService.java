package com.colegio.shuji.evaluacion.application.service;

import static com.colegio.shuji.shared.domain.model.Reglas.exigir;
import static com.colegio.shuji.shared.domain.model.Reglas.requerido;

import com.colegio.shuji.academico.application.port.out.AnioLectivoRepositoryPort;
import com.colegio.shuji.academico.application.port.out.PeriodoRepositoryPort;
import com.colegio.shuji.curriculo.application.port.out.AsignacionRepositoryPort;
import com.colegio.shuji.curriculo.application.port.out.CompetenciaRepositoryPort;
import com.colegio.shuji.evaluacion.application.dto.in.RegistrarCalificacionesMasivasRequestDto;
import com.colegio.shuji.evaluacion.application.dto.out.CalificacionResponseDto;
import com.colegio.shuji.evaluacion.application.dto.out.EstudiantesEnRiesgoResponseDto;
import com.colegio.shuji.evaluacion.application.dto.out.LibretaNotasResponseDto;
import com.colegio.shuji.evaluacion.application.mapper.EvaluacionMapper;
import com.colegio.shuji.evaluacion.application.port.in.ConsultarLibretaNotasUseCase;
import com.colegio.shuji.evaluacion.application.port.in.RegistrarEvaluacionCnebUseCase;
import com.colegio.shuji.evaluacion.application.port.out.CalificacionRepositoryPort;
import com.colegio.shuji.curriculo.domain.model.Competencia;
import com.colegio.shuji.evaluacion.domain.model.CalificacionCneb;
import com.colegio.shuji.matricula.application.port.out.MatriculaRepositoryPort;
import com.colegio.shuji.matricula.domain.enums.EstadoMatricula;
import com.colegio.shuji.matricula.domain.model.Matricula;
import com.colegio.shuji.shared.application.port.out.ActorActualPort;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class EvaluacionService
    implements RegistrarEvaluacionCnebUseCase, ConsultarLibretaNotasUseCase {
  private final EvaluacionMapper mapper;
  private final CalificacionRepositoryPort calificaciones;
  private final PeriodoRepositoryPort periodos;
  private final AnioLectivoRepositoryPort anios;
  private final AsignacionRepositoryPort asignaciones;
  private final MatriculaRepositoryPort matriculas;
  private final CompetenciaRepositoryPort competencias;
  private final ActorActualPort actor;

  public List<CalificacionResponseDto> registrar(RegistrarCalificacionesMasivasRequestDto r) {
    var snapshot = requerido(periodos.buscarPorId(r.periodoAcademicoId()));
    requerido(anios.bloquearPorId(snapshot.getAnioLectivoId())).verificarAbierto();
    var periodo = requerido(periodos.bloquearPorId(r.periodoAcademicoId()));
    try {
      periodo.verificarAbierto();
    } catch (com.colegio.shuji.shared.domain.exception.BusinessException ex) {
      throw new com.colegio.shuji.evaluacion.domain.exception.PeriodoEvaluacionCerradoException(
          ex.getMessage());
    }
    var a = requerido(asignaciones.buscarPorId(r.asignacionDocenteId()));
    actor.verificarDocente(a.getDocenteUsuarioId());
    exigir(a.getAnioLectivoId().equals(periodo.getAnioLectivoId()), "Asignación de otro año");
    var result = new ArrayList<CalificacionResponseDto>();
    var claves = new HashSet<String>();
    var matriculasCache = new HashMap<Long, Matricula>();
    var competenciasCache = new HashMap<Short, Competencia>();
    for (var item : r.calificaciones()) {
      exigir(
          claves.add(item.matriculaId() + "/" + item.competenciaId()),
          "Calificación repetida en el lote");
      var m =
          matriculasCache.computeIfAbsent(
              item.matriculaId(), id -> requerido(matriculas.buscarPorId(id)));
      var comp =
          competenciasCache.computeIfAbsent(
              item.competenciaId(), id -> requerido(competencias.buscarPorId(id)));
      if (!m.getAnioLectivoId().equals(a.getAnioLectivoId())
          || !m.getSeccionId().equals(a.getSeccionId())
          || !comp.getAreaId().equals(a.getAreaCurricularId())
          || m.getEstadoMatricula() != EstadoMatricula.MATRICULADO)
        throw new com.colegio.shuji.evaluacion.domain.exception.CalificacionInvalidaException(
            "La matrícula o competencia no corresponde a la asignación");
      var c =
          calificaciones.buscarPorMatriculaId(m.getId()).stream()
              .filter(
                  v ->
                      v.getPeriodoAcademicoId().equals(periodo.getId())
                          && v.getCompetenciaId().equals(comp.getId()))
              .findFirst()
              .orElseGet(
                  () ->
                      CalificacionCneb.builder()
                          .matriculaId(m.getId())
                          .anioLectivoId(a.getAnioLectivoId())
                          .seccionId(a.getSeccionId())
                          .periodoAcademicoId(periodo.getId())
                          .asignacionDocenteId(a.getId())
                          .areaCurricularId(a.getAreaCurricularId())
                          .docenteUsuarioId(a.getDocenteUsuarioId())
                          .competenciaId(comp.getId())
                          .build());
      c.actualizar(
          item.calificacionCualitativa(),
          item.conclusionDescriptiva(),
          item.sugerenciaIaUtilizada());
      result.add(mapper.toResponse(calificaciones.guardar(c)));
    }
    return result;
  }

  @Transactional(readOnly = true)
  public LibretaNotasResponseDto libreta(Long matriculaId) {
    var notas = calificaciones.buscarPorMatriculaId(matriculaId);
    if (actor.tieneRol("DOCENTE") && !actor.tieneRol("DIRECCION") && !actor.tieneRol("SECRETARIA"))
      notas =
          notas.stream().filter(c -> c.getDocenteUsuarioId().equals(actor.usuarioId())).toList();
    return new LibretaNotasResponseDto(
        matriculaId, notas.stream().map(mapper::toResponse).toList());
  }

  @Transactional(readOnly = true)
  public EstudiantesEnRiesgoResponseDto riesgo(Short periodoId) {
    return new EstudiantesEnRiesgoResponseDto(
        periodoId,
        calificaciones.buscarPorPeriodoAcademicoId(periodoId).stream()
            .filter(CalificacionCneb::necesitaRefuerzo)
            .filter(
                c ->
                    actor.tieneRol("DIRECCION")
                        || actor.tieneRol("SECRETARIA")
                        || c.getDocenteUsuarioId().equals(actor.usuarioId()))
            .map(mapper::toResponse)
            .toList());
  }
}
