package com.colegio.shuji.evaluacion.application.service;

import static com.colegio.shuji.shared.domain.model.Reglas.exigir;
import static com.colegio.shuji.shared.domain.model.Reglas.horas;
import static com.colegio.shuji.shared.domain.model.Reglas.requerido;

import com.colegio.shuji.academico.application.port.out.AnioLectivoRepositoryPort;
import com.colegio.shuji.academico.application.port.out.AulaRepositoryPort;
import com.colegio.shuji.academico.application.port.out.PeriodoRepositoryPort;
import com.colegio.shuji.curriculo.application.port.out.AsignacionRepositoryPort;
import com.colegio.shuji.evaluacion.application.dto.in.ProgramarRefuerzoRequestDto;
import com.colegio.shuji.evaluacion.application.dto.in.RegistrarAsistenciaRefuerzoRequestDto;
import com.colegio.shuji.evaluacion.application.dto.out.InscripcionRefuerzoResponseDto;
import com.colegio.shuji.evaluacion.application.dto.out.SesionRefuerzoResponseDto;
import com.colegio.shuji.evaluacion.application.mapper.EvaluacionMapper;
import com.colegio.shuji.evaluacion.application.port.in.DerivarAlumnosRefuerzoUseCase;
import com.colegio.shuji.evaluacion.application.port.out.CalificacionRepositoryPort;
import com.colegio.shuji.evaluacion.application.port.out.InscripcionRefuerzoRepositoryPort;
import com.colegio.shuji.evaluacion.application.port.out.RefuerzoRepositoryPort;
import com.colegio.shuji.evaluacion.domain.enums.EstadoAsistenciaRefuerzo;
import com.colegio.shuji.evaluacion.domain.model.CalificacionCneb;
import com.colegio.shuji.evaluacion.domain.model.InscripcionRefuerzo;
import com.colegio.shuji.matricula.application.port.out.MatriculaRepositoryPort;
import com.colegio.shuji.matricula.domain.enums.EstadoMatricula;
import com.colegio.shuji.matricula.domain.model.Matricula;
import com.colegio.shuji.shared.application.port.out.ActorActualPort;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class RefuerzoService implements DerivarAlumnosRefuerzoUseCase {
  private final EvaluacionMapper mapper;
  private final RefuerzoRepositoryPort refuerzos;
  private final InscripcionRefuerzoRepositoryPort inscripciones;
  private final CalificacionRepositoryPort calificaciones;
  private final MatriculaRepositoryPort matriculas;
  private final PeriodoRepositoryPort periodos;
  private final AulaRepositoryPort aulas;
  private final AnioLectivoRepositoryPort anios;
  private final AsignacionRepositoryPort asignaciones;
  private final ActorActualPort actor;

  public SesionRefuerzoResponseDto programar(ProgramarRefuerzoRequestDto r) {
    requerido(anios.bloquearPorId(r.anioLectivoId())).verificarAbierto();
    var p = requerido(periodos.bloquearPorId(r.periodoAcademicoId()));
    p.verificarActivo();
    String codigoAula = null;
    if (r.aulaAsignada() != null && !r.aulaAsignada().isBlank()) {
      var aula = requerido(aulas.buscarPorCodigo(r.aulaAsignada().trim()));
      exigir(Boolean.TRUE.equals(aula.getActiva()), "El aula no está activa");
      codigoAula = aula.getCodigo();
    }
    exigir(p.getAnioLectivoId().equals(r.anioLectivoId()), "Período de otro año");
    horas(r.horaInicio(), r.horaFin());
    actor.verificarDocente(r.docenteUsuarioId());
    exigir(
        asignaciones.buscarPorDocenteUsuarioId(r.docenteUsuarioId()).stream()
            .anyMatch(
                a ->
                    a.getAnioLectivoId().equals(r.anioLectivoId())
                        && a.getAreaCurricularId().equals(r.areaCurricularId())),
        "Docente sin asignación para el área");
    String aulaValidada = codigoAula;
    exigir(
        refuerzos.buscarPorAnioLectivoId(r.anioLectivoId()).stream()
            .noneMatch(
                s ->
                    s.getFechaProgramada().equals(r.fechaProgramada())
                        && s.getHoraInicio().isBefore(r.horaFin())
                        && s.getHoraFin().isAfter(r.horaInicio())
                        && (s.getDocenteUsuarioId().equals(r.docenteUsuarioId())
                            || (aulaValidada != null
                                && aulaValidada.equals(s.getAulaAsignada())))),
        "Horario de refuerzo ocupado");
    var sesion = mapper.toDomain(r);
    sesion.setAulaAsignada(codigoAula);
    return mapper.toResponse(refuerzos.guardar(sesion));
  }

  public List<InscripcionRefuerzoResponseDto> derivar(Long sesionId) {
    var s = requerido(refuerzos.bloquearPorId(sesionId));
    actor.verificarDocente(s.getDocenteUsuarioId());
    var result = new ArrayList<InscripcionRefuerzoResponseDto>();
    var existentes = inscripciones.buscarPorSesionRefuerzoId(sesionId);
    var inscritos = new HashSet<Long>();
    existentes.forEach(i -> inscritos.add(i.getEstudianteId()));
    var candidatos =
        calificaciones.buscarPorPeriodoAcademicoId(s.getPeriodoAcademicoId()).stream()
            .filter(
                c ->
                    c.necesitaRefuerzo()
                        && c.getAreaCurricularId().equals(s.getAreaCurricularId())
                        && c.getDocenteUsuarioId().equals(s.getDocenteUsuarioId()))
            .toList();
    var matIds = candidatos.stream().map(CalificacionCneb::getMatriculaId).distinct().toList();
    var matriculasMap =
        matriculas.buscarPorIds(matIds).stream()
            .collect(Collectors.toMap(Matricula::getId, Function.identity()));
    for (var c : candidatos) {
      var m = matriculasMap.get(c.getMatriculaId());
      if (m == null
          || m.getEstadoMatricula() != EstadoMatricula.MATRICULADO
          || !inscritos.add(m.getEstudianteId())) continue;
      var i =
          InscripcionRefuerzo.builder()
              .sesionRefuerzoId(sesionId)
              .estudianteId(m.getEstudianteId())
              .calificacionOrigenId(c.getId())
              .estadoAsistencia(EstadoAsistenciaRefuerzo.PENDIENTE)
              .build();
      result.add(mapper.toResponse(inscripciones.guardar(i)));
    }
    return result;
  }

  public InscripcionRefuerzoResponseDto registrarAsistencia(
      RegistrarAsistenciaRefuerzoRequestDto r) {
    var i = requerido(inscripciones.bloquearPorId(r.inscripcionId()));
    var s = requerido(refuerzos.buscarPorId(i.getSesionRefuerzoId()));
    actor.verificarDocente(s.getDocenteUsuarioId());
    exigir(
        !s.getFechaProgramada().isAfter(LocalDate.now(ZoneId.of("America/Lima"))),
        "La sesión todavía no ocurre");
    exigir(r.estadoAsistencia() != EstadoAsistenciaRefuerzo.PENDIENTE, "Indique la asistencia");
    if (r.estadoAsistencia() == EstadoAsistenciaRefuerzo.JUSTIFICADO)
      exigir(r.observaciones() != null && !r.observaciones().isBlank(), "Indique justificación");
    i.registrarAsistencia(r.estadoAsistencia(), r.observaciones());
    return mapper.toResponse(inscripciones.guardar(i));
  }

  @Transactional(readOnly = true)
  public List<SesionRefuerzoResponseDto> sesiones(Short periodoId) {
    return refuerzos.buscarPorPeriodoAcademicoId(periodoId).stream()
        .filter(
            s ->
                actor.tieneRol("DIRECCION")
                    || actor.tieneRol("SECRETARIA")
                    || s.getDocenteUsuarioId().equals(actor.usuarioId()))
        .map(mapper::toResponse)
        .toList();
  }
}
