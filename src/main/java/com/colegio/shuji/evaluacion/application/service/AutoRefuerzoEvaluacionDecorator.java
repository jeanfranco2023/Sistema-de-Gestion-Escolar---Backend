package com.colegio.shuji.evaluacion.application.service;

import com.colegio.shuji.academico.application.port.out.SeccionRepositoryPort;
import com.colegio.shuji.academico.domain.model.Seccion;
import com.colegio.shuji.curriculo.application.port.out.AsignacionRepositoryPort;
import com.colegio.shuji.curriculo.application.port.out.CurriculoRepositoryPort;
import com.colegio.shuji.evaluacion.application.dto.in.RegistrarCalificacionesMasivasRequestDto;
import com.colegio.shuji.evaluacion.application.dto.out.CalificacionResponseDto;
import com.colegio.shuji.evaluacion.application.dto.out.FilaCalificacionResponseDto;
import com.colegio.shuji.evaluacion.application.port.in.RegistrarEvaluacionCnebUseCase;
import com.colegio.shuji.evaluacion.application.port.out.InscripcionRefuerzoRepositoryPort;
import com.colegio.shuji.evaluacion.application.port.out.RefuerzoRepositoryPort;
import com.colegio.shuji.evaluacion.domain.enums.CalificacionCualitativa;
import com.colegio.shuji.evaluacion.domain.enums.EstadoAsistenciaRefuerzo;
import com.colegio.shuji.evaluacion.domain.model.InscripcionRefuerzo;
import com.colegio.shuji.evaluacion.domain.model.SesionRefuerzo;
import com.colegio.shuji.matricula.application.port.out.MatriculaRepositoryPort;
import com.colegio.shuji.matricula.domain.enums.EstadoMatricula;
import com.colegio.shuji.matricula.domain.model.Matricula;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Envuelve el registro/importación de calificaciones CNEB para generar o reutilizar
 * automáticamente sesiones de refuerzo y derivar a los estudiantes con nivel B o C.
 */
@Primary
@Service
@Transactional
public class AutoRefuerzoEvaluacionDecorator implements RegistrarEvaluacionCnebUseCase {
  private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");
  private static final LocalTime HORA_INICIO_REFUERZO = LocalTime.of(17, 0);
  private static final LocalTime HORA_FIN_REFUERZO = LocalTime.of(17, 45);
  private static final int MAX_LEN_TEMA = 40;

  private final RegistrarEvaluacionCnebUseCase delegate;
  private final AsignacionRepositoryPort asignaciones;
  private final CurriculoRepositoryPort curriculos;
  private final MatriculaRepositoryPort matriculas;
  private final SeccionRepositoryPort secciones;
  private final RefuerzoRepositoryPort refuerzos;
  private final InscripcionRefuerzoRepositoryPort inscripciones;

  public AutoRefuerzoEvaluacionDecorator(
      @Qualifier("evaluacionService") RegistrarEvaluacionCnebUseCase delegate,
      AsignacionRepositoryPort asignaciones,
      CurriculoRepositoryPort curriculos,
      MatriculaRepositoryPort matriculas,
      SeccionRepositoryPort secciones,
      RefuerzoRepositoryPort refuerzos,
      InscripcionRefuerzoRepositoryPort inscripciones) {
    this.delegate = delegate;
    this.asignaciones = asignaciones;
    this.curriculos = curriculos;
    this.matriculas = matriculas;
    this.secciones = secciones;
    this.refuerzos = refuerzos;
    this.inscripciones = inscripciones;
  }

  @Override
  public List<CalificacionResponseDto> registrar(RegistrarCalificacionesMasivasRequestDto r) {
    var guardadas = delegate.registrar(r);
    derivarAutomaticamente(r, guardadas);
    return guardadas;
  }

  @Override
  @Transactional(readOnly = true)
  public List<CalificacionResponseDto> listar(Long asignacionDocenteId, Short periodoAcademicoId) {
    return delegate.listar(asignacionDocenteId, periodoAcademicoId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<FilaCalificacionResponseDto> listarMatriz(
      Long asignacionDocenteId, Short periodoAcademicoId, Short competenciaId) {
    return delegate.listarMatriz(asignacionDocenteId, periodoAcademicoId, competenciaId);
  }

  private void derivarAutomaticamente(
      RegistrarCalificacionesMasivasRequestDto request, List<CalificacionResponseDto> guardadas) {
    var riesgos =
        guardadas.stream()
            .filter(
                c ->
                    c.calificacionCualitativa() == CalificacionCualitativa.C
                        || c.calificacionCualitativa() == CalificacionCualitativa.B)
            .toList();
    if (riesgos.isEmpty()) {
      return;
    }

    var asignacion = asignaciones.buscarPorId(request.asignacionDocenteId()).orElse(null);
    if (asignacion == null) {
      return;
    }

    var matIds = riesgos.stream().map(CalificacionResponseDto::matriculaId).distinct().toList();
    Map<Long, Matricula> matriculasMap =
        matriculas.buscarPorIds(matIds).stream()
            .collect(Collectors.toMap(Matricula::getId, Function.identity()));

    Map<Long, CalificacionResponseDto> califPorEstudiante = new HashMap<>();
    for (var calif : riesgos) {
      var mat = matriculasMap.get(calif.matriculaId());
      if (mat == null || mat.getEstadoMatricula() != EstadoMatricula.MATRICULADO) {
        continue;
      }
      var previa = califPorEstudiante.get(mat.getEstudianteId());
      if (previa == null
          || (previa.calificacionCualitativa() == CalificacionCualitativa.B
              && calif.calificacionCualitativa() == CalificacionCualitativa.C)) {
        califPorEstudiante.put(mat.getEstudianteId(), calif);
      }
    }
    if (califPorEstudiante.isEmpty()) {
      return;
    }

    var sesion = obtenerOCrearSesion(asignacion.getAnioLectivoId(), request.periodoAcademicoId(),
        asignacion.getAreaCurricularId(), asignacion.getDocenteUsuarioId(), asignacion.getSeccionId());

    Set<Long> yaInscritos = new HashSet<>();
    inscripciones.buscarPorSesionRefuerzoId(sesion.getId()).forEach(i -> yaInscritos.add(i.getEstudianteId()));

    for (var entry : califPorEstudiante.entrySet()) {
      Long estudianteId = entry.getKey();
      if (!yaInscritos.add(estudianteId)) {
        continue;
      }
      CalificacionResponseDto origen = entry.getValue();
      String obs =
          "Derivación automática ("
              + origen.calificacionCualitativa().name()
              + ")";
      inscripciones.guardar(
          InscripcionRefuerzo.builder()
              .sesionRefuerzoId(sesion.getId())
              .estudianteId(estudianteId)
              .calificacionOrigenId(origen.id())
              .estadoAsistencia(EstadoAsistenciaRefuerzo.PENDIENTE)
              .observaciones(obs)
              .build());
    }
  }

  private SesionRefuerzo obtenerOCrearSesion(
      Short anioId, Short periodoId, Short areaId, Long docenteId, Integer seccionId) {
    String aulaObjetivo =
        seccionId == null
            ? null
            : secciones.buscarPorId(seccionId).map(s -> s.getCodigoAula()).orElse(null);
    var existentes =
        refuerzos.buscarPorPeriodoAcademicoId(periodoId).stream()
            .filter(
                s ->
                    s.getAnioLectivoId().equals(anioId)
                        && s.getAreaCurricularId().equals(areaId)
                        && s.getDocenteUsuarioId().equals(docenteId)
                        && (aulaObjetivo == null
                            || s.getAulaAsignada() == null
                            || aulaObjetivo.equals(s.getAulaAsignada())))
            .min(Comparator.comparing(SesionRefuerzo::getId));
    if (existentes.isPresent()) {
      return existentes.get();
    }

    String codigoAula =
        secciones
            .buscarPorId(seccionId)
            .map(Seccion::getCodigoAula)
            .filter(c -> c != null && !c.isBlank())
            .orElse(null);

    String nombreArea =
        curriculos.buscarPorId(areaId).map(a -> a.getNombre()).orElse("Académico");
    String tema = construirTema(nombreArea);

    var sesionesAnio = refuerzos.buscarPorAnioLectivoId(anioId);
    LocalDate fecha = siguienteDiaHabil(LocalDate.now(ZONA_LIMA).plusDays(2));
    for (int intento = 0; intento < 30; intento++) {
      LocalDate candidata = fecha;
      boolean ocupado =
          sesionesAnio.stream()
              .anyMatch(
                  s ->
                      s.getFechaProgramada().equals(candidata)
                          && s.getHoraInicio().isBefore(HORA_FIN_REFUERZO)
                          && s.getHoraFin().isAfter(HORA_INICIO_REFUERZO)
                          && (s.getDocenteUsuarioId().equals(docenteId)
                              || (codigoAula != null && codigoAula.equals(s.getAulaAsignada()))));
      if (!ocupado) {
        break;
      }
      fecha = siguienteDiaHabil(fecha.plusDays(1));
    }

    var nueva =
        SesionRefuerzo.builder()
            .anioLectivoId(anioId)
            .periodoAcademicoId(periodoId)
            .areaCurricularId(areaId)
            .docenteUsuarioId(docenteId)
            .tema(tema)
            .fechaProgramada(fecha)
            .horaInicio(HORA_INICIO_REFUERZO)
            .horaFin(HORA_FIN_REFUERZO)
            .build();
    nueva.asignarAula(codigoAula);
    return refuerzos.guardar(nueva);
  }

  private static LocalDate siguienteDiaHabil(LocalDate fecha) {
    while (fecha.getDayOfWeek() == DayOfWeek.SATURDAY || fecha.getDayOfWeek() == DayOfWeek.SUNDAY) {
      fecha = fecha.plusDays(1);
    }
    return fecha;
  }

  private static String construirTema(String nombreArea) {
    String base = "Refuerzo: " + (nombreArea == null ? "Área" : nombreArea.trim());
    return base.length() <= MAX_LEN_TEMA ? base : base.substring(0, MAX_LEN_TEMA);
  }
}
