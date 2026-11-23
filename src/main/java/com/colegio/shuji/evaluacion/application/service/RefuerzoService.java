package com.colegio.shuji.evaluacion.application.service;

import static com.colegio.shuji.shared.domain.model.Reglas.exigir;
import static com.colegio.shuji.shared.domain.model.Reglas.horas;
import static com.colegio.shuji.shared.domain.model.Reglas.requerido;

import com.colegio.shuji.academico.application.port.out.AnioLectivoRepositoryPort;
import com.colegio.shuji.academico.application.port.out.AulaRepositoryPort;
import com.colegio.shuji.academico.application.port.out.GradoRepositoryPort;
import com.colegio.shuji.academico.application.port.out.NivelRepositoryPort;
import com.colegio.shuji.academico.application.port.out.PeriodoRepositoryPort;
import com.colegio.shuji.academico.application.port.out.SeccionRepositoryPort;
import com.colegio.shuji.academico.domain.model.AnioLectivo;
import com.colegio.shuji.academico.domain.model.Grado;
import com.colegio.shuji.academico.domain.model.Nivel;
import com.colegio.shuji.academico.domain.model.PeriodoAcademico;
import com.colegio.shuji.academico.domain.model.Seccion;
import com.colegio.shuji.curriculo.application.port.out.AsignacionRepositoryPort;
import com.colegio.shuji.curriculo.application.port.out.CompetenciaRepositoryPort;
import com.colegio.shuji.curriculo.application.port.out.CurriculoRepositoryPort;
import com.colegio.shuji.curriculo.domain.model.AreaCurricular;
import com.colegio.shuji.curriculo.domain.model.Competencia;
import com.colegio.shuji.evaluacion.application.dto.in.ProgramarRefuerzoRequestDto;
import com.colegio.shuji.evaluacion.application.dto.in.RegistrarAsistenciaRefuerzoRequestDto;
import com.colegio.shuji.evaluacion.application.dto.out.EstudianteRiesgoRefuerzoResponseDto;
import com.colegio.shuji.evaluacion.application.dto.out.InscripcionRefuerzoDetalleResponseDto;
import com.colegio.shuji.evaluacion.application.dto.out.InscripcionRefuerzoResponseDto;
import com.colegio.shuji.evaluacion.application.dto.out.SesionRefuerzoDetalleResponseDto;
import com.colegio.shuji.evaluacion.application.dto.out.SesionRefuerzoResponseDto;
import com.colegio.shuji.evaluacion.application.dto.out.TableroRefuerzoResponseDto;
import com.colegio.shuji.evaluacion.application.mapper.EvaluacionMapper;
import com.colegio.shuji.evaluacion.application.port.in.DerivarAlumnosRefuerzoUseCase;
import com.colegio.shuji.evaluacion.application.port.out.CalificacionRepositoryPort;
import com.colegio.shuji.evaluacion.application.port.out.InscripcionRefuerzoRepositoryPort;
import com.colegio.shuji.evaluacion.application.port.out.RefuerzoRepositoryPort;
import com.colegio.shuji.evaluacion.domain.enums.CalificacionCualitativa;
import com.colegio.shuji.evaluacion.domain.enums.EstadoAsistenciaRefuerzo;
import com.colegio.shuji.evaluacion.domain.model.CalificacionCneb;
import com.colegio.shuji.evaluacion.domain.model.InscripcionRefuerzo;
import com.colegio.shuji.evaluacion.domain.model.SesionRefuerzo;
import com.colegio.shuji.matricula.application.port.out.EstudianteRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.MatriculaRepositoryPort;
import com.colegio.shuji.matricula.domain.enums.EstadoMatricula;
import com.colegio.shuji.matricula.domain.model.Estudiante;
import com.colegio.shuji.matricula.domain.model.Matricula;
import com.colegio.shuji.shared.application.port.out.ActorActualPort;
import com.colegio.shuji.usuario.application.port.out.UserRepositoryPort;
import com.colegio.shuji.usuario.domain.model.Usuario;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class RefuerzoService implements DerivarAlumnosRefuerzoUseCase {
  private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

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
  private final EstudianteRepositoryPort estudiantes;
  private final SeccionRepositoryPort secciones;
  private final GradoRepositoryPort grados;
  private final NivelRepositoryPort niveles;
  private final CurriculoRepositoryPort curriculos;
  private final CompetenciaRepositoryPort competencias;
  private final UserRepositoryPort usuarios;

  private record ContextoAcademico(
      Map<Short, PeriodoAcademico> periodos,
      Map<Short, AreaCurricular> areas,
      Map<Short, Competencia> competencias,
      Map<Long, String> docentes,
      Map<Integer, Seccion> secciones,
      Map<String, Seccion> seccionesPorAula,
      Map<Short, Grado> grados,
      Map<Short, Nivel> niveles) {}

  private record ClaveRiesgo(Long estudianteId, Short periodoId, Short areaId) {}

  @Override
  @Transactional(readOnly = true)
  public List<InscripcionRefuerzoDetalleResponseDto> inscripciones(Long sesionId) {
    var sesion = requerido(refuerzos.buscarPorId(sesionId));
    actor.verificarDocente(sesion.getDocenteUsuarioId());
    var filas = inscripciones.buscarPorSesionRefuerzoId(sesionId);
    if (filas.isEmpty()) return List.of();

    var estIds = filas.stream().map(InscripcionRefuerzo::getEstudianteId).distinct().toList();
    var estMap =
        estudiantes.buscarPorIds(estIds).stream()
            .collect(Collectors.toMap(Estudiante::getId, Function.identity()));

    var califIds =
        filas.stream()
            .map(InscripcionRefuerzo::getCalificacionOrigenId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    var califMap =
        calificaciones.buscarPorIds(califIds).stream()
            .collect(Collectors.toMap(CalificacionCneb::getId, Function.identity()));

    var matIds =
        califMap.values().stream().map(CalificacionCneb::getMatriculaId).distinct().toList();
    var matMap =
        matriculas.buscarPorIds(matIds).stream()
            .collect(Collectors.toMap(Matricula::getId, Function.identity()));

    Set<Long> matEstIds =
        matMap.values().stream().map(Matricula::getEstudianteId).collect(Collectors.toSet());
    var estSinMat = estIds.stream().filter(id -> !matEstIds.contains(id)).toList();
    Map<Long, Matricula> matPorEst = new HashMap<>();
    if (!estSinMat.isEmpty()) {
      for (var m : matriculas.buscarPorEstudianteIds(estSinMat)) {
        if (Objects.equals(m.getAnioLectivoId(), sesion.getAnioLectivoId())) {
          matPorEst.putIfAbsent(m.getEstudianteId(), m);
        }
      }
    }

    var compIds =
        califMap.values().stream()
            .map(CalificacionCneb::getCompetenciaId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    var compMap =
        compIds.isEmpty()
            ? Map.<Short, Competencia>of()
            : competencias.buscarPorIds(compIds).stream()
                .collect(Collectors.toMap(Competencia::getId, Function.identity()));
    var ctx = cargarContexto(sesion.getAnioLectivoId(), compMap);
    var secSesion = resolverSeccionPorAula(sesion.getAulaAsignada(), ctx);

    return filas.stream()
        .map(
            i -> {
              var est = estMap.get(i.getEstudianteId());
              var cal =
                  i.getCalificacionOrigenId() == null
                      ? null
                      : califMap.get(i.getCalificacionOrigenId());
              var mat =
                  cal != null ? matMap.get(cal.getMatriculaId()) : matPorEst.get(i.getEstudianteId());
              var sec =
                  mat != null && mat.getSeccionId() != null
                      ? ctx.secciones().get(mat.getSeccionId())
                      : secSesion;
              var comp =
                  cal != null && cal.getCompetenciaId() != null
                      ? ctx.competencias().get(cal.getCompetenciaId())
                      : null;
              return new InscripcionRefuerzoDetalleResponseDto(
                  i.getId(),
                  i.getSesionRefuerzoId(),
                  i.getEstudianteId(),
                  i.getCalificacionOrigenId(),
                  i.getEstadoAsistencia(),
                  i.getObservaciones(),
                  est != null ? est.nombreCompleto().trim() : "Estudiante #" + i.getEstudianteId(),
                  est != null ? est.getNumeroDocumento() : null,
                  etiquetaGradoSeccion(sec, ctx),
                  cal != null && cal.getCalificacionCualitativa() != null
                      ? cal.getCalificacionCualitativa().name()
                      : null,
                  comp != null ? comp.getNombre() : null,
                  cal != null ? cal.getConclusionDescriptiva() : null);
            })
        .sorted(
            Comparator.comparing(
                InscripcionRefuerzoDetalleResponseDto::nombreEstudiante,
                String.CASE_INSENSITIVE_ORDER))
        .toList();
  }

  @Override
  public SesionRefuerzoResponseDto programar(ProgramarRefuerzoRequestDto r) {
    requerido(anios.bloquearPorId(r.anioLectivoId())).verificarAbierto();
    var p = requerido(periodos.bloquearPorId(r.periodoAcademicoId()));
    p.verificarActivo();
    p.verificarAbierto();
    exigir(
        !esPeriodoConcluidoParaRefuerzo(p, r.anioLectivoId(), LocalDate.now(ZONA_LIMA)),
        "El bimestre seleccionado ya concluyó; el reforzamiento solo aplica al bimestre pendiente actual");
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
    sesion.asignarAula(codigoAula);
    return mapper.toResponse(refuerzos.guardar(sesion));
  }

  @Override
  public List<InscripcionRefuerzoResponseDto> derivar(Long sesionId) {
    var s = requerido(refuerzos.bloquearPorId(sesionId));
    actor.verificarDocente(s.getDocenteUsuarioId());
    periodos
        .buscarPorId(s.getPeriodoAcademicoId())
        .ifPresent(
            p ->
                exigir(
                    !esPeriodoConcluidoParaRefuerzo(p, s.getAnioLectivoId(), LocalDate.now(ZONA_LIMA)),
                    "El bimestre de esta sesión ya concluyó; el reforzamiento solo aplica al bimestre pendiente actual"));
    var result = new ArrayList<InscripcionRefuerzoResponseDto>();
    var existentes = inscripciones.buscarPorSesionRefuerzoId(sesionId);
    var inscritos = new HashSet<Long>();
    existentes.forEach(i -> inscritos.add(i.getEstudianteId()));

    // Si el aula corresponde a una sección específica del año, solo derivamos estudiantes de esa sección.
    Integer seccionObjetivoId = resolverSeccionObjetivo(s).map(Seccion::getId).orElse(null);

    var candidatos =
        calificaciones.buscarRiesgosPorPeriodo(s.getPeriodoAcademicoId()).stream()
            .filter(
                c ->
                    (c.necesitaRefuerzo()
                            || c.getCalificacionCualitativa() == CalificacionCualitativa.B)
                        && c.getAreaCurricularId().equals(s.getAreaCurricularId())
                        && c.getDocenteUsuarioId().equals(s.getDocenteUsuarioId()))
            .toList();
    var matIds = candidatos.stream().map(CalificacionCneb::getMatriculaId).distinct().toList();
    var matriculasMap =
        matriculas.buscarPorIds(matIds).stream()
            .collect(Collectors.toMap(Matricula::getId, Function.identity()));
    for (var c : candidatos) {
      var m = matriculasMap.get(c.getMatriculaId());
      if (m == null || m.getEstadoMatricula() != EstadoMatricula.MATRICULADO) continue;
      if (seccionObjetivoId != null && !seccionObjetivoId.equals(m.getSeccionId())) continue;
      if (!inscritos.add(m.getEstudianteId())) continue;
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

  @Override
  public InscripcionRefuerzoResponseDto registrarAsistencia(
      RegistrarAsistenciaRefuerzoRequestDto r) {
    var i = requerido(inscripciones.bloquearPorId(r.inscripcionId()));
    var s = requerido(refuerzos.buscarPorId(i.getSesionRefuerzoId()));
    actor.verificarDocente(s.getDocenteUsuarioId());
    exigir(
        !s.getFechaProgramada().isAfter(LocalDate.now(ZONA_LIMA)),
        "La sesión todavía no ocurre");
    exigir(r.estadoAsistencia() != EstadoAsistenciaRefuerzo.PENDIENTE, "Indique la asistencia");
    if (r.estadoAsistencia() == EstadoAsistenciaRefuerzo.JUSTIFICADO)
      exigir(r.observaciones() != null && !r.observaciones().isBlank(), "Indique justificación");
    i.registrarAsistencia(r.estadoAsistencia(), r.observaciones());
    return mapper.toResponse(inscripciones.guardar(i));
  }

  @Override
  @Transactional(readOnly = true)
  public List<SesionRefuerzoResponseDto> sesiones(Short periodoId) {
    return refuerzos.buscarPorPeriodoAcademicoId(periodoId).stream()
        .filter(this::sesionVisible)
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public TableroRefuerzoResponseDto tablero(Short anioId, Short periodoId) {
    Short targetAnioId = resolverAnioObjetivo(anioId, periodoId);
    LocalDate hoy = LocalDate.now(ZONA_LIMA);
    Short periodoEfectivoId =
        periodoId != null
            ? periodoId
            : resolverPeriodoVigente(targetAnioId, hoy).map(PeriodoAcademico::getId).orElse(null);
    List<SesionRefuerzo> visibles =
        buscarSesionesObjetivo(targetAnioId, periodoId).stream()
            .filter(this::sesionVisible)
            .filter(
                s ->
                    periodoEfectivoId == null
                        || Objects.equals(s.getPeriodoAcademicoId(), periodoEfectivoId))
            .sorted(
                Comparator.comparing(
                        SesionRefuerzo::getFechaProgramada,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(
                        SesionRefuerzo::getHoraInicio,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(
                        SesionRefuerzo::getId, Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();

    var sesionIds = visibles.stream().map(SesionRefuerzo::getId).filter(Objects::nonNull).toList();
    var todasInscripciones =
        sesionIds.isEmpty() ? List.<InscripcionRefuerzo>of() : inscripciones.buscarPorSesionRefuerzoIds(sesionIds);

    var calificacionesRiesgo =
        buscarCalificacionesScope(targetAnioId, periodoId).stream()
            .filter(
                c ->
                    periodoEfectivoId == null
                        || Objects.equals(c.getPeriodoAcademicoId(), periodoEfectivoId))
            .filter(
                c ->
                    c.getCalificacionCualitativa() == CalificacionCualitativa.C
                        || c.getCalificacionCualitativa() == CalificacionCualitativa.B)
            .filter(
                c ->
                    esGestionGeneral()
                        || (c.getDocenteUsuarioId() != null
                            && c.getDocenteUsuarioId().equals(actor.usuarioId())))
            .toList();

    Map<Long, CalificacionCneb> califMap =
        calificacionesRiesgo.stream()
            .collect(Collectors.toMap(CalificacionCneb::getId, Function.identity(), (a, b) -> a));
    var missingCalifIds =
        todasInscripciones.stream()
            .map(InscripcionRefuerzo::getCalificacionOrigenId)
            .filter(id -> id != null && !califMap.containsKey(id))
            .distinct()
            .toList();
    if (!missingCalifIds.isEmpty()) {
      calificaciones.buscarPorIds(missingCalifIds).forEach(c -> califMap.put(c.getId(), c));
    }

    var compMap =
        competencias.listar().stream()
            .collect(Collectors.toMap(Competencia::getId, Function.identity(), (a, b) -> a));
    var docenteIds = new HashSet<Long>();
    visibles.stream().map(SesionRefuerzo::getDocenteUsuarioId).filter(Objects::nonNull).forEach(docenteIds::add);
    califMap.values().stream().map(CalificacionCneb::getDocenteUsuarioId).filter(Objects::nonNull).forEach(docenteIds::add);
    var ctx = cargarContexto(targetAnioId, compMap, docenteIds);

    var matIds =
        califMap.values().stream()
            .map(CalificacionCneb::getMatriculaId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    var matMap =
        matIds.isEmpty()
            ? Map.<Long, Matricula>of()
            : matriculas.buscarPorIds(matIds).stream()
                .collect(Collectors.toMap(Matricula::getId, Function.identity()));

    var estIds = new HashSet<Long>();
    todasInscripciones.forEach(i -> estIds.add(i.getEstudianteId()));
    matMap.values().forEach(m -> estIds.add(m.getEstudianteId()));
    var estMap =
        estIds.isEmpty()
            ? Map.<Long, Estudiante>of()
            : estudiantes.buscarPorIds(estIds).stream()
                .collect(Collectors.toMap(Estudiante::getId, Function.identity()));

    var inscPorSesion =
        todasInscripciones.stream()
            .collect(Collectors.groupingBy(InscripcionRefuerzo::getSesionRefuerzoId));
    var sesionPorId =
        visibles.stream().collect(Collectors.toMap(SesionRefuerzo::getId, Function.identity()));

    Map<Long, InscripcionRefuerzo> inscPorCalId = new HashMap<>();
    Map<ClaveRiesgo, InscripcionRefuerzo> inscPorClave = new HashMap<>();
    for (var insc : todasInscripciones) {
      if (insc.getCalificacionOrigenId() != null) {
        inscPorCalId.putIfAbsent(insc.getCalificacionOrigenId(), insc);
      }
      var ses = sesionPorId.get(insc.getSesionRefuerzoId());
      if (ses != null) {
        inscPorClave.putIfAbsent(
            new ClaveRiesgo(
                insc.getEstudianteId(), ses.getPeriodoAcademicoId(), ses.getAreaCurricularId()),
            insc);
      }
    }

    LocalDate hoyTabla = LocalDate.now(ZONA_LIMA);
    List<SesionRefuerzoDetalleResponseDto> sesionesDto =
        visibles.stream()
            .map(
                s ->
                    mapearSesionDetalle(
                        s, inscPorSesion.getOrDefault(s.getId(), List.of()), califMap, ctx, hoyTabla))
            .toList();

    List<EstudianteRiesgoRefuerzoResponseDto> riesgosDto =
        calificacionesRiesgo.stream()
            .map(
                c -> {
                  var m = matMap.get(c.getMatriculaId());
                  if (m == null || m.getEstadoMatricula() != EstadoMatricula.MATRICULADO) return null;
                  var est = estMap.get(m.getEstudianteId());
                  if (est == null) return null;
                  var insc =
                      Optional.ofNullable(inscPorCalId.get(c.getId()))
                          .orElseGet(
                              () ->
                                  inscPorClave.get(
                                      new ClaveRiesgo(
                                          est.getId(),
                                          c.getPeriodoAcademicoId(),
                                          c.getAreaCurricularId())));
                  var ses = insc == null ? null : sesionPorId.get(insc.getSesionRefuerzoId());
                  return mapearRiesgo(c, m, est, insc, ses, ctx);
                })
            .filter(Objects::nonNull)
            .sorted(
                Comparator.comparing(EstudianteRiesgoRefuerzoResponseDto::derivado)
                    .thenComparing(
                        r -> "C".equals(r.calificacionCualitativa()) ? 0 : 1)
                    .thenComparing(
                        EstudianteRiesgoRefuerzoResponseDto::periodoAcademicoId,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(
                        r -> Objects.toString(r.gradoSeccion(), ""),
                        String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(
                        r -> Objects.toString(r.nombreEstudiante(), ""),
                        String.CASE_INSENSITIVE_ORDER))
            .toList();

    long pendientes =
        sesionesDto.stream().mapToLong(SesionRefuerzoDetalleResponseDto::pendientes).sum();
    long asistieron =
        sesionesDto.stream().mapToLong(SesionRefuerzoDetalleResponseDto::asistieron).sum();
    long faltaron =
        sesionesDto.stream().mapToLong(SesionRefuerzoDetalleResponseDto::faltaron).sum();
    long justificados =
        sesionesDto.stream().mapToLong(SesionRefuerzoDetalleResponseDto::justificados).sum();
    long estudiantesUnicos =
        todasInscripciones.stream().map(InscripcionRefuerzo::getEstudianteId).distinct().count();
    long sinDerivar = riesgosDto.stream().filter(r -> !r.derivado()).count();

    return new TableroRefuerzoResponseDto(
        targetAnioId,
        periodoEfectivoId,
        sesionesDto.size(),
        todasInscripciones.size(),
        pendientes,
        asistieron,
        faltaron,
        justificados,
        estudiantesUnicos,
        riesgosDto.size(),
        sinDerivar,
        sesionesDto,
        riesgosDto);
  }

  private boolean esGestionGeneral() {
    return actor.tieneRol("DIRECCION") || actor.tieneRol("ADMINISTRADOR");
  }

  private boolean sesionVisible(SesionRefuerzo s) {
    return esGestionGeneral()
        || (s.getDocenteUsuarioId() != null && s.getDocenteUsuarioId().equals(actor.usuarioId()));
  }

  private Optional<PeriodoAcademico> resolverPeriodoVigente(Short anioId, LocalDate hoy) {
    if (anioId == null) return Optional.empty();
    var lista = periodos.buscarPorAnioLectivoId(anioId);
    if (lista == null || lista.isEmpty()) return Optional.empty();
    var ordenados =
        lista.stream()
            .filter(Objects::nonNull)
            .sorted(
                Comparator.comparing(
                        PeriodoAcademico::getNumeroPeriodo,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(
                        PeriodoAcademico::getId, Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();
    if (ordenados.isEmpty()) return Optional.empty();
    return ordenados.stream()
        .filter(p -> !Boolean.TRUE.equals(p.getCerrado()) && !Boolean.FALSE.equals(p.getActivo()))
        .filter(
            p ->
                p.getFechaInicio() != null
                    && p.getFechaFin() != null
                    && !hoy.isBefore(p.getFechaInicio())
                    && !hoy.isAfter(p.getFechaFin()))
        .findFirst()
        .or(
            () ->
                ordenados.stream()
                    .filter(
                        p ->
                            !Boolean.TRUE.equals(p.getCerrado())
                                && !Boolean.FALSE.equals(p.getActivo()))
                    .filter(p -> p.getFechaInicio() != null && !hoy.isBefore(p.getFechaInicio()))
                    .reduce((first, second) -> second))
        .or(
            () ->
                ordenados.stream()
                    .filter(
                        p ->
                            !Boolean.TRUE.equals(p.getCerrado())
                                && !Boolean.FALSE.equals(p.getActivo()))
                    .findFirst())
        .or(() -> Optional.of(ordenados.get(ordenados.size() - 1)));
  }

  private boolean esPeriodoConcluidoParaRefuerzo(
      PeriodoAcademico periodo, Short anioId, LocalDate hoy) {
    if (periodo == null) return false;
    if (Boolean.TRUE.equals(periodo.getCerrado()) || Boolean.FALSE.equals(periodo.getActivo())) {
      return true;
    }
    var vigenteOpt = resolverPeriodoVigente(anioId != null ? anioId : periodo.getAnioLectivoId(), hoy);
    if (vigenteOpt.isPresent()) {
      var vigente = vigenteOpt.get();
      if (Objects.equals(vigente.getId(), periodo.getId())) {
        return false;
      }
      if (periodo.getNumeroPeriodo() != null && vigente.getNumeroPeriodo() != null) {
        return periodo.getNumeroPeriodo() < vigente.getNumeroPeriodo();
      }
    }
    return periodo.getFechaFin() != null && periodo.getFechaFin().isBefore(hoy);
  }

  private Short resolverAnioObjetivo(Short anioId, Short periodoId) {
    if (anioId != null) return anioId;
    if (periodoId != null) {
      var per = periodos.buscarPorId(periodoId);
      if (per.isPresent()) return per.get().getAnioLectivoId();
    }
    var lista = anios.listar();
    return lista.stream()
        .filter(AnioLectivo::estaAbierto)
        .findFirst()
        .or(() -> lista.stream().max(Comparator.comparing(AnioLectivo::getAnio)))
        .map(AnioLectivo::getId)
        .orElse(null);
  }

  private List<SesionRefuerzo> buscarSesionesObjetivo(Short anioId, Short periodoId) {
    if (periodoId != null) return refuerzos.buscarPorPeriodoAcademicoId(periodoId);
    if (anioId != null) return refuerzos.buscarPorAnioLectivoId(anioId);
    return refuerzos.listar();
  }

  private List<CalificacionCneb> buscarCalificacionesScope(Short anioId, Short periodoId) {
    if (anioId != null && periodoId != null) {
      return calificaciones.buscarRiesgosPorAnioYPeriodo(anioId, periodoId);
    }
    if (periodoId != null) return calificaciones.buscarRiesgosPorPeriodo(periodoId);
    if (anioId != null) return calificaciones.buscarRiesgosPorAnio(anioId);
    return List.of();
  }

  private Optional<Seccion> resolverSeccionObjetivo(SesionRefuerzo s) {
    if (s.getAulaAsignada() == null || s.getAulaAsignada().isBlank()) return Optional.empty();
    String clave = s.getAulaAsignada().trim().toUpperCase(Locale.ROOT);
    return secciones.buscarPorAnioLectivoId(s.getAnioLectivoId()).stream()
        .filter(
            sec ->
                sec.getCodigoAula() != null
                    && sec.getCodigoAula().trim().toUpperCase(Locale.ROOT).equals(clave))
        .findFirst();
  }

  private ContextoAcademico cargarContexto(Short anioId, Map<Short, Competencia> compMap) {
    return cargarContexto(anioId, compMap, null);
  }

  private ContextoAcademico cargarContexto(
      Short anioId, Map<Short, Competencia> compMap, java.util.Collection<Long> docenteIds) {
    var listaPeriodos =
        anioId != null ? periodos.buscarPorAnioLectivoId(anioId) : periodos.listar();
    var periodosMap =
        listaPeriodos.stream()
            .collect(Collectors.toMap(PeriodoAcademico::getId, Function.identity(), (a, b) -> a));
    var areasMap =
        curriculos.listar().stream()
            .collect(Collectors.toMap(AreaCurricular::getId, Function.identity(), (a, b) -> a));
    var listaDocentes =
        docenteIds == null
            ? usuarios.findAll()
            : (docenteIds.isEmpty() ? List.<Usuario>of() : usuarios.findByIds(docenteIds));
    var docentesMap =
        listaDocentes.stream()
            .collect(Collectors.toMap(Usuario::getId, this::nombreDocente, (a, b) -> a));
    var listaSecciones =
        anioId != null ? secciones.buscarPorAnioLectivoId(anioId) : secciones.listar();
    var seccionesMap =
        listaSecciones.stream()
            .collect(Collectors.toMap(Seccion::getId, Function.identity(), (a, b) -> a));
    var seccionesAula =
        listaSecciones.stream()
            .filter(s -> s.getCodigoAula() != null && !s.getCodigoAula().isBlank())
            .collect(
                Collectors.toMap(
                    s -> s.getCodigoAula().trim().toUpperCase(Locale.ROOT),
                    Function.identity(),
                    (a, b) -> a));
    var gradosMap =
        grados.listar().stream()
            .collect(Collectors.toMap(Grado::getId, Function.identity(), (a, b) -> a));
    var nivelesMap =
        niveles.listar().stream()
            .collect(Collectors.toMap(Nivel::getId, Function.identity(), (a, b) -> a));
    return new ContextoAcademico(
        periodosMap, areasMap, compMap, docentesMap, seccionesMap, seccionesAula, gradosMap, nivelesMap);
  }

  private String nombreDocente(Usuario u) {
    if (u.getNombreCompleto() != null && !u.getNombreCompleto().isBlank()) {
      return u.getNombreCompleto().trim();
    }
    return u.getUsername();
  }

  private Seccion resolverSeccionPorAula(String codigoAula, ContextoAcademico ctx) {
    if (codigoAula == null || codigoAula.isBlank()) return null;
    return ctx.seccionesPorAula().get(codigoAula.trim().toUpperCase(Locale.ROOT));
  }

  private String etiquetaGradoSeccion(Seccion sec, ContextoAcademico ctx) {
    if (sec == null) return null;
    var grado = ctx.grados().get(sec.getGradoId());
    var nivel = ctx.niveles().get(sec.getNivelId());
    if (grado == null) {
      return sec.getLetra();
    }
    String gNombre = grado.getNombre() == null ? "" : grado.getNombre().trim();
    String nNombre = nivel != null && nivel.getNombre() != null ? nivel.getNombre().trim() : "";
    String base =
        !nNombre.isEmpty()
                && !gNombre.toLowerCase(Locale.ROOT).contains(nNombre.toLowerCase(Locale.ROOT))
            ? gNombre + " " + nNombre
            : gNombre;
    return sec.getLetra() == null || sec.getLetra().isBlank()
        ? base
        : base + " · " + sec.getLetra();
  }

  private SesionRefuerzoDetalleResponseDto mapearSesionDetalle(
      SesionRefuerzo s,
      List<InscripcionRefuerzo> lista,
      Map<Long, CalificacionCneb> califMap,
      ContextoAcademico ctx,
      LocalDate hoy) {
    var per = ctx.periodos().get(s.getPeriodoAcademicoId());
    var area = ctx.areas().get(s.getAreaCurricularId());
    var sec = resolverSeccionPorAula(s.getAulaAsignada(), ctx);
    var grado = sec != null ? ctx.grados().get(sec.getGradoId()) : null;
    var nivel = sec != null ? ctx.niveles().get(sec.getNivelId()) : null;

    long pendientes =
        lista.stream()
            .filter(i -> i.getEstadoAsistencia() == EstadoAsistenciaRefuerzo.PENDIENTE)
            .count();
    long asistieron =
        lista.stream()
            .filter(i -> i.getEstadoAsistencia() == EstadoAsistenciaRefuerzo.ASISTIO)
            .count();
    long faltaron =
        lista.stream().filter(i -> i.getEstadoAsistencia() == EstadoAsistenciaRefuerzo.FALTO).count();
    long justificados =
        lista.stream()
            .filter(i -> i.getEstadoAsistencia() == EstadoAsistenciaRefuerzo.JUSTIFICADO)
            .count();

    long casosC = 0;
    long casosB = 0;
    for (var i : lista) {
      var c = i.getCalificacionOrigenId() == null ? null : califMap.get(i.getCalificacionOrigenId());
      if (c == null) continue;
      if (c.getCalificacionCualitativa() == CalificacionCualitativa.C) casosC++;
      else if (c.getCalificacionCualitativa() == CalificacionCualitativa.B) casosB++;
    }

    String estadoSesion;
    if (lista.isEmpty()) estadoSesion = "SIN_DERIVADOS";
    else if (pendientes == 0) estadoSesion = "COMPLETADA";
    else if (s.getFechaProgramada() != null && s.getFechaProgramada().isAfter(hoy))
      estadoSesion = "PROGRAMADA";
    else estadoSesion = "PENDIENTE_ASISTENCIA";

    return new SesionRefuerzoDetalleResponseDto(
        s.getId(),
        s.getAnioLectivoId(),
        s.getPeriodoAcademicoId(),
        per != null ? per.getNombre() : null,
        s.getAreaCurricularId(),
        area != null ? area.getNombre() : null,
        s.getDocenteUsuarioId(),
        ctx.docentes().getOrDefault(s.getDocenteUsuarioId(), "Docente #" + s.getDocenteUsuarioId()),
        s.getTema(),
        s.getFechaProgramada(),
        s.getHoraInicio(),
        s.getHoraFin(),
        s.getAulaAsignada(),
        sec != null ? sec.getId() : null,
        nivel != null ? nivel.getNombre() : null,
        grado != null ? grado.getNombre() : null,
        sec != null ? sec.getLetra() : null,
        etiquetaGradoSeccion(sec, ctx),
        lista.size(),
        pendientes,
        asistieron,
        faltaron,
        justificados,
        casosC,
        casosB,
        estadoSesion);
  }

  private EstudianteRiesgoRefuerzoResponseDto mapearRiesgo(
      CalificacionCneb c,
      Matricula m,
      Estudiante est,
      InscripcionRefuerzo insc,
      SesionRefuerzo ses,
      ContextoAcademico ctx) {
    var per = ctx.periodos().get(c.getPeriodoAcademicoId());
    var area = ctx.areas().get(c.getAreaCurricularId());
    var comp = ctx.competencias().get(c.getCompetenciaId());
    var sec = m.getSeccionId() != null ? ctx.secciones().get(m.getSeccionId()) : null;
    var grado = sec != null ? ctx.grados().get(sec.getGradoId()) : null;
    var nivel = sec != null ? ctx.niveles().get(sec.getNivelId()) : null;

    return new EstudianteRiesgoRefuerzoResponseDto(
        c.getId(),
        m.getId(),
        est.getId(),
        est.nombreCompleto().trim(),
        est.getNumeroDocumento(),
        c.getAnioLectivoId(),
        c.getPeriodoAcademicoId(),
        per != null ? per.getNombre() : null,
        sec != null ? sec.getId() : m.getSeccionId(),
        nivel != null ? nivel.getNombre() : null,
        grado != null ? grado.getNombre() : null,
        sec != null ? sec.getLetra() : null,
        etiquetaGradoSeccion(sec, ctx),
        c.getAreaCurricularId(),
        area != null ? area.getNombre() : null,
        c.getDocenteUsuarioId(),
        ctx.docentes().getOrDefault(c.getDocenteUsuarioId(), "Docente #" + c.getDocenteUsuarioId()),
        c.getCompetenciaId(),
        comp != null ? comp.getNombre() : null,
        c.getCalificacionCualitativa() != null ? c.getCalificacionCualitativa().name() : null,
        c.getConclusionDescriptiva(),
        insc != null,
        ses != null ? ses.getId() : null,
        ses != null ? ses.getTema() : null,
        insc != null && insc.getEstadoAsistencia() != null
            ? insc.getEstadoAsistencia().name()
            : null);
  }
}
