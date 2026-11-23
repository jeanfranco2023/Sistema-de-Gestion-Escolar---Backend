package com.colegio.shuji.shared.infrastructure.controller;

import com.colegio.shuji.academico.application.port.out.AnioLectivoRepositoryPort;
import com.colegio.shuji.academico.application.port.out.GradoRepositoryPort;
import com.colegio.shuji.academico.application.port.out.NivelRepositoryPort;
import com.colegio.shuji.academico.application.port.out.PeriodoRepositoryPort;
import com.colegio.shuji.academico.application.port.out.SeccionRepositoryPort;
import com.colegio.shuji.academico.domain.model.AnioLectivo;
import com.colegio.shuji.academico.domain.model.Grado;
import com.colegio.shuji.academico.domain.model.Nivel;
import com.colegio.shuji.academico.domain.model.PeriodoAcademico;
import com.colegio.shuji.academico.domain.model.Seccion;
import com.colegio.shuji.asistencia.application.port.out.AsistenciaAulaRepositoryPort;
import com.colegio.shuji.asistencia.domain.enums.EstadoAsistenciaAula;
import com.colegio.shuji.asistencia.domain.model.AsistenciaAula;
import com.colegio.shuji.evaluacion.application.port.out.CalificacionRepositoryPort;
import com.colegio.shuji.evaluacion.domain.enums.CalificacionCualitativa;
import com.colegio.shuji.evaluacion.domain.model.CalificacionCneb;
import com.colegio.shuji.matricula.application.port.out.EstudianteRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.MatriculaRepositoryPort;
import com.colegio.shuji.matricula.domain.enums.EstadoMatricula;
import com.colegio.shuji.matricula.domain.model.Estudiante;
import com.colegio.shuji.matricula.domain.model.Matricula;
import com.colegio.shuji.tesoreria.application.port.out.PagoRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
  private final AnioLectivoRepositoryPort anios;
  private final MatriculaRepositoryPort matriculas;
  private final AsistenciaAulaRepositoryPort asistencias;
  private final CalificacionRepositoryPort calificaciones;
  private final PagoRepositoryPort pagos;
  private final PeriodoRepositoryPort periodos;
  private final SeccionRepositoryPort secciones;
  private final GradoRepositoryPort grados;
  private final NivelRepositoryPort niveles;
  private final EstudianteRepositoryPort estudiantes;

  public DashboardController(
      AnioLectivoRepositoryPort anios,
      MatriculaRepositoryPort matriculas,
      AsistenciaAulaRepositoryPort asistencias,
      CalificacionRepositoryPort calificaciones,
      PagoRepositoryPort pagos) {
    this(anios, matriculas, asistencias, calificaciones, pagos, null, null, null, null, null);
  }

  @Autowired
  public DashboardController(
      AnioLectivoRepositoryPort anios,
      MatriculaRepositoryPort matriculas,
      AsistenciaAulaRepositoryPort asistencias,
      CalificacionRepositoryPort calificaciones,
      PagoRepositoryPort pagos,
      PeriodoRepositoryPort periodos,
      SeccionRepositoryPort secciones,
      GradoRepositoryPort grados,
      NivelRepositoryPort niveles,
      EstudianteRepositoryPort estudiantes) {
    this.anios = anios;
    this.matriculas = matriculas;
    this.asistencias = asistencias;
    this.calificaciones = calificaciones;
    this.pagos = pagos;
    this.periodos = periodos;
    this.secciones = secciones;
    this.grados = grados;
    this.niveles = niveles;
    this.estudiantes = estudiantes;
  }

  /** Cada metrica es null cuando no pudo calcularse; el cliente debe mostrarla como "sin dato". */
  public record Resumen(
      int anio,
      Long totalEstudiantes,
      Integer asistenciaHoy,
      BigDecimal recaudacionMes,
      Long alumnosRiesgo) {}

  public record EstudianteDesempenoItem(
      Long matriculaId,
      String estudianteNombre,
      String gradoSeccion,
      String nivelLogro,
      String promedioCualitativo) {}

  public record DesempenoIndicador(
      long destacados,
      long logroEsperado,
      long enProceso,
      long requierenAcompanamiento,
      long totalEvaluados,
      long sinEvaluacion,
      long totalMatriculados,
      String reglaClasificacion,
      List<EstudianteDesempenoItem> estudiantes) {}

  public record CoberturaIndicador(
      long evaluados, long matriculados, long sinEvaluacion, double porcentajeCobertura) {}

  public record SeccionCupoItem(
      Integer seccionId,
      String gradoNombre,
      String seccionNombre,
      String nivel,
      int matriculados,
      int cupoMaximo,
      int vacantesDisponibles) {}

  public record AsistenciaCuposIndicador(
      Double porcentajeAsistencia,
      Double variacionPeriodoAnterior,
      long presentes,
      long tardanzas,
      long inasistencias,
      long faltasJustificadas,
      long faltasInjustificadas,
      long totalMatriculadosAlcance,
      long totalVacantesAlcance,
      List<SeccionCupoItem> secciones) {}

  public record IndicadoresDireccionResponse(
      DesempenoIndicador desempeno,
      CoberturaIndicador cobertura,
      AsistenciaCuposIndicador asistenciaCupos) {}

  @GetMapping("/resumen")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','ADMINISTRADOR')")
  public Resumen resumen(Authentication authentication) {
    var hoy = LocalDate.now(ZoneId.of("America/Lima"));
    int anioActual = hoy.getYear();
    Long totalEstudiantes = null;
    Integer porcentajeAsistencia = null;
    BigDecimal recaudacion = null;
    Long alumnosRiesgo = null;

    try {
      var anioOpt = anios.listar().stream().filter(a -> a.getAnio() == hoy.getYear()).findFirst();
      if (anioOpt.isPresent()) {
        var anio = anioOpt.get();
        anioActual = (int) anio.getAnio();
        try {
          totalEstudiantes =
              matriculas.buscarPorAnioLectivoId(anio.getId()).stream()
                  .filter(m -> m.getEstadoMatricula() == EstadoMatricula.MATRICULADO)
                  .count();
        } catch (Exception e) {
          log.warn("Error calculando matriculas activas: {}", e.getMessage());
        }
        boolean direccion =
            authentication != null
                && authentication.getAuthorities().stream()
                    .anyMatch(
                        authority ->
                            "ROLE_DIRECCION".equals(authority.getAuthority())
                                || "ROLE_ADMINISTRADOR".equals(authority.getAuthority()));
        if (direccion) {
          try {
            PeriodoAcademico periodoVigente = resolverPeriodoVigente(anio.getId(), hoy);
            List<CalificacionCneb> notasRiesgoBase;
            if (periodoVigente != null && periodoVigente.getId() != null) {
              var porPeriodo =
                  calificaciones.buscarPorAnioYPeriodo(anio.getId(), periodoVigente.getId());
              notasRiesgoBase =
                  (porPeriodo != null && !porPeriodo.isEmpty())
                      ? porPeriodo
                      : calificaciones.buscarPorAnioLectivoId(anio.getId());
            } else {
              notasRiesgoBase = calificaciones.buscarPorAnioLectivoId(anio.getId());
            }
            alumnosRiesgo =
                notasRiesgoBase.stream()
                    .filter(
                        c ->
                            periodoVigente == null
                                || periodoVigente.getId() == null
                                || periodoVigente.getId().equals(c.getPeriodoAcademicoId()))
                    .filter(c -> c.getCalificacionCualitativa() == CalificacionCualitativa.C)
                    .map(CalificacionCneb::getMatriculaId)
                    .distinct()
                    .count();
          } catch (Exception e) {
            log.warn("Error calculando alumnos en riesgo: {}", e.getMessage());
          }
        }
      }
    } catch (Exception e) {
      log.warn("Error consultando anio lectivo: {}", e.getMessage());
    }

    try {
      var asistencia = asistencias.buscarPorFechaSesion(hoy);
      if (asistencia != null) {
        porcentajeAsistencia =
            asistencia.isEmpty()
                ? 0
                : (int)
                    Math.round(
                        100.0
                            * asistencia.stream().filter(AsistenciaAula::esPresente).count()
                            / asistencia.size());
      }
    } catch (Exception e) {
      log.warn("Error calculando asistencia: {}", e.getMessage());
    }

    try {
      var listaPagos = pagos.listar();
      if (listaPagos != null) {
        recaudacion =
            listaPagos.stream()
                .filter(
                    p ->
                        p != null
                            && p.estaAprobado()
                            && p.getFechaPago() != null
                            && p.getMontoPagado() != null
                            && p.getFechaPago()
                                    .atZoneSameInstant(ZoneId.of("America/Lima"))
                                    .getYear()
                                == hoy.getYear()
                            && p.getFechaPago()
                                    .atZoneSameInstant(ZoneId.of("America/Lima"))
                                    .getMonthValue()
                                == hoy.getMonthValue())
                .map(p -> p.getMontoPagado())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
      }
    } catch (Exception e) {
      log.warn("Error calculando recaudacion del mes: {}", e.getMessage());
    }

    return new Resumen(
        anioActual, totalEstudiantes, porcentajeAsistencia, recaudacion, alumnosRiesgo);
  }

  @GetMapping("/indicadores")
  @PreAuthorize("hasAnyRole('DIRECCION','ADMINISTRADOR','SECRETARIA')")
  @Transactional(readOnly = true)
  public IndicadoresDireccionResponse indicadores(
      @RequestParam(required = false) Long anioLectivoId,
      @RequestParam(required = false) Long periodoId,
      @RequestParam(required = false) String nivel,
      @RequestParam(required = false) Long gradoId,
      @RequestParam(required = false) Long seccionId) {

    String reglaExplicativa =
        "Cada estudiante matriculado cuenta una sola vez por bimestre según su nivel de logro predominante (promedio CNEB ponderado AD=4, A=3, B=2, C=1; la clasificación y el reforzamiento corresponden exclusivamente al bimestre evaluado y no arrastran recuperaciones de bimestres ya concluidos).";

    Short anioId = resolverAnioId(anioLectivoId);
    LocalDate hoyLima = LocalDate.now(ZoneId.of("America/Lima"));
    PeriodoAcademico periodoEfectivo =
        periodoId != null
            ? resolverPeriodo(anioId, periodoId)
            : resolverPeriodoVigente(anioId, hoyLima);
    Long periodoEfectivoId =
        periodoEfectivo != null && periodoEfectivo.getId() != null
            ? Long.valueOf(periodoEfectivo.getId().longValue())
            : periodoId;
    Map<Short, Nivel> mapaNiveles = cargarMapaNiveles();
    Map<Short, Grado> mapaGrados = cargarMapaGrados();
    List<Seccion> seccionesAlcance =
        filtrarSecciones(anioId, nivel, gradoId, seccionId, mapaNiveles, mapaGrados);
    Set<Integer> seccionIdsAlcance =
        seccionesAlcance.stream()
            .map(Seccion::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    Map<Integer, Seccion> mapaSecciones =
        seccionesAlcance.stream()
            .filter(s -> s.getId() != null)
            .collect(Collectors.toMap(Seccion::getId, s -> s, (a, b) -> a));

    List<Matricula> matriculasActivas = new ArrayList<>();
    if (anioId != null) {
      List<Matricula> todasAnio = matriculas.buscarPorAnioLectivoId(anioId);
      if (todasAnio != null) {
        boolean hayFiltroSeccion =
            (nivel != null && !nivel.isBlank())
                || gradoId != null
                || seccionId != null
                || !seccionIdsAlcance.isEmpty();
        for (Matricula m : todasAnio) {
          if (m != null && m.getEstadoMatricula() == EstadoMatricula.MATRICULADO) {
            if (!hayFiltroSeccion
                || (m.getSeccionId() != null && seccionIdsAlcance.contains(m.getSeccionId()))) {
              if (seccionId != null
                  && (m.getSeccionId() == null
                      || m.getSeccionId().longValue() != seccionId.longValue())) {
                continue;
              }
              matriculasActivas.add(m);
            }
          }
        }
      }
    }

    Set<Long> matriculaIds =
        matriculasActivas.stream()
            .map(Matricula::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    Map<Long, Estudiante> mapaEstudiantes = cargarMapaEstudiantes(matriculasActivas);

    List<CalificacionCneb> notasCorte = new ArrayList<>();
    if (anioId != null) {
      List<CalificacionCneb> notasAnio;
      if (periodoEfectivoId != null) {
        List<CalificacionCneb> porPeriodo =
            calificaciones.buscarPorAnioYPeriodo(anioId, periodoEfectivoId.shortValue());
        notasAnio =
            (porPeriodo != null && !porPeriodo.isEmpty())
                ? porPeriodo
                : calificaciones.buscarPorAnioLectivoId(anioId);
      } else {
        notasAnio = calificaciones.buscarPorAnioLectivoId(anioId);
      }
      if (notasAnio != null) {
        for (CalificacionCneb c : notasAnio) {
          if (c == null || c.getMatriculaId() == null || !matriculaIds.contains(c.getMatriculaId())) {
            continue;
          }
          if (periodoEfectivoId != null
              && (c.getPeriodoAcademicoId() == null
                  || c.getPeriodoAcademicoId().longValue() != periodoEfectivoId.longValue())) {
            continue;
          }
          notasCorte.add(c);
        }
      }
    }

    Map<Long, List<CalificacionCneb>> notasPorMatricula =
        notasCorte.stream().collect(Collectors.groupingBy(CalificacionCneb::getMatriculaId));

    long destacados = 0;
    long logroEsperado = 0;
    long enProceso = 0;
    long requierenAcompanamiento = 0;
    List<EstudianteDesempenoItem> detalleEstudiantes = new ArrayList<>();

    for (Matricula m : matriculasActivas) {
      List<CalificacionCneb> notasAlumno = notasPorMatricula.get(m.getId());
      Estudiante est = mapaEstudiantes.get(m.getEstudianteId());
      String nombreEst =
          est != null ? est.nombreCompleto().trim() : "Matrícula #" + m.getId();
      String etiquetaGradoSeccion =
          formatearGradoSeccion(m.getSeccionId(), mapaSecciones, mapaGrados);
      if (notasAlumno == null || notasAlumno.isEmpty()) {
        detalleEstudiantes.add(new EstudianteDesempenoItem(
            m.getId(), nombreEst, etiquetaGradoSeccion, "SIN_EVALUACION", null));
        continue;
      }
      CalificacionCualitativa nivelCorte = clasificarEstudianteUnico(notasAlumno);
      switch (nivelCorte) {
        case AD -> destacados++;
        case A -> logroEsperado++;
        case B -> enProceso++;
        case C -> requierenAcompanamiento++;
      }
      detalleEstudiantes.add(
          new EstudianteDesempenoItem(
              m.getId(), nombreEst, etiquetaGradoSeccion, nivelCorte.name(), nivelCorte.name()));
    }

    long totalMatriculados = matriculasActivas.size();
    long totalEvaluados = destacados + logroEsperado + enProceso + requierenAcompanamiento;
    long sinEvaluacion = Math.max(0L, totalMatriculados - totalEvaluados);
    double porcentajeCobertura =
        totalMatriculados == 0
            ? 0.0
            : Math.round((totalEvaluados * 1000.0) / totalMatriculados) / 10.0;

    DesempenoIndicador desempeno =
        new DesempenoIndicador(
            destacados,
            logroEsperado,
            enProceso,
            requierenAcompanamiento,
            totalEvaluados,
            sinEvaluacion,
            totalMatriculados,
            reglaExplicativa,
            detalleEstudiantes);

    CoberturaIndicador cobertura =
        new CoberturaIndicador(
            totalEvaluados, totalMatriculados, sinEvaluacion, porcentajeCobertura);

    AsistenciaCuposIndicador asistenciaCupos =
        calcularAsistenciaYCupos(
            anioId,
            periodoEfectivoId,
            matriculaIds,
            matriculasActivas,
            seccionesAlcance,
            mapaGrados,
            mapaNiveles);

    return new IndicadoresDireccionResponse(desempeno, cobertura, asistenciaCupos);
  }

  private CalificacionCualitativa clasificarEstudianteUnico(List<CalificacionCneb> notas) {
    boolean tieneC =
        notas.stream().anyMatch(n -> n.getCalificacionCualitativa() == CalificacionCualitativa.C);
    double suma = 0.0;
    int cuenta = 0;
    for (CalificacionCneb n : notas) {
      if (n.getCalificacionCualitativa() == null) continue;
      cuenta++;
      suma +=
          switch (n.getCalificacionCualitativa()) {
            case AD -> 4.0;
            case A -> 3.0;
            case B -> 2.0;
            case C -> 1.0;
          };
    }
    if (cuenta == 0) {
      return CalificacionCualitativa.B;
    }
    double promedio = suma / cuenta;
    if (tieneC && promedio < 2.5) {
      return CalificacionCualitativa.C;
    }
    if (promedio >= 3.5) {
      return CalificacionCualitativa.AD;
    }
    if (promedio >= 2.5) {
      return CalificacionCualitativa.A;
    }
    if (promedio >= 1.5) {
      return CalificacionCualitativa.B;
    }
    return CalificacionCualitativa.C;
  }

  private AsistenciaCuposIndicador calcularAsistenciaYCupos(
      Short anioId,
      Long periodoId,
      Set<Long> matriculaIds,
      List<Matricula> matriculasActivas,
      List<Seccion> seccionesAlcance,
      Map<Short, Grado> mapaGrados,
      Map<Short, Nivel> mapaNiveles) {

    List<AsistenciaAula> registros = new ArrayList<>();
    if (!matriculaIds.isEmpty()) {
      List<AsistenciaAula> porMatriculas = asistencias.buscarPorMatriculaIds(matriculaIds);
      if (porMatriculas != null) {
        registros.addAll(porMatriculas);
      }
    }

    PeriodoAcademico periodoSeleccionado = resolverPeriodo(anioId, periodoId);
    List<AsistenciaAula> registrosCorte =
        filtrarAsistenciasPorPeriodo(registros, periodoSeleccionado);

    long presentes =
        registrosCorte.stream()
            .filter(a -> a != null && a.getEstado() == EstadoAsistenciaAula.PRESENTE)
            .count();
    long tardanzas =
        registrosCorte.stream()
            .filter(a -> a != null && a.getEstado() == EstadoAsistenciaAula.TARDANZA)
            .count();
    long inasistencias =
        registrosCorte.stream().filter(a -> a != null && a.esFalta()).count();
    long totalRegistros = presentes + tardanzas + inasistencias;
    Double porcentajeAsistencia =
        totalRegistros == 0
            ? null
            : Math.round(((presentes + tardanzas) * 1000.0) / totalRegistros) / 10.0;

    Double variacionPeriodoAnterior = porcentajeAsistencia == null ? null
        : calcularVariacionPeriodoAnterior(anioId, periodoSeleccionado, registros, porcentajeAsistencia);

    Map<Integer, Long> matriculadosPorSeccion =
        matriculasActivas.stream()
            .filter(m -> m.getSeccionId() != null)
            .collect(Collectors.groupingBy(Matricula::getSeccionId, Collectors.counting()));

    List<SeccionCupoItem> itemsSeccion = new ArrayList<>();
    long totalVacantesAlcance = 0;

    for (Seccion s : seccionesAlcance) {
      Grado g = mapaGrados.get(s.getGradoId());
      Nivel n = mapaNiveles.get(s.getNivelId());
      String gradoNombre = g != null && g.getNombre() != null ? g.getNombre() : "Grado " + s.getGradoId();
      String nivelCodigo =
          n != null && n.getCodigo() != null ? n.getCodigo().name() : "GENERAL";
      int matSeccion = matriculadosPorSeccion.getOrDefault(s.getId(), 0L).intValue();
      int cupoMax = s.getCupoMaximo() != null ? s.getCupoMaximo() : 0;
      int ocupadas = s.getVacantesOcupadas() != null ? s.getVacantesOcupadas() : matSeccion;
      int vacantesDisp = Math.max(0, cupoMax - Math.max(ocupadas, matSeccion));
      totalVacantesAlcance += vacantesDisp;

      itemsSeccion.add(
          new SeccionCupoItem(
              s.getId(),
              gradoNombre,
              s.getLetra(),
              nivelCodigo,
              matSeccion,
              cupoMax,
              vacantesDisp));
    }

    return new AsistenciaCuposIndicador(
        porcentajeAsistencia,
        variacionPeriodoAnterior,
        presentes,
        tardanzas,
        inasistencias,
        registrosCorte.stream().filter(a -> a != null && a.getEstado() == EstadoAsistenciaAula.FALTA_JUSTIFICADA).count(),
        registrosCorte.stream().filter(a -> a != null && a.getEstado() == EstadoAsistenciaAula.FALTA_INJUSTIFICADA).count(),
        matriculasActivas.size(),
        totalVacantesAlcance,
        itemsSeccion);
  }

  private List<AsistenciaAula> filtrarAsistenciasPorPeriodo(
      List<AsistenciaAula> registros, PeriodoAcademico periodo) {
    if (periodo == null || periodo.getFechaInicio() == null || periodo.getFechaFin() == null) {
      return registros;
    }
    return registros.stream()
        .filter(
            a ->
                a != null
                    && a.getFechaSesion() != null
                    && !a.getFechaSesion().isBefore(periodo.getFechaInicio())
                    && !a.getFechaSesion().isAfter(periodo.getFechaFin()))
        .toList();
  }

  private Double calcularVariacionPeriodoAnterior(
      Short anioId,
      PeriodoAcademico periodoActual,
      List<AsistenciaAula> todosRegistros,
      double porcentajeActual) {
    if (periodos == null
        || anioId == null
        || periodoActual == null
        || periodoActual.getNumeroPeriodo() == null
        || periodoActual.getNumeroPeriodo() <= 1) {
      return null;
    }
    try {
      short numAnterior = (short) (periodoActual.getNumeroPeriodo() - 1);
      PeriodoAcademico anterior =
          periodos.buscarPorAnioLectivoId(anioId).stream()
              .filter(p -> p.getNumeroPeriodo() != null && p.getNumeroPeriodo() == numAnterior)
              .findFirst()
              .orElse(null);
      if (anterior == null) {
        return null;
      }
      List<AsistenciaAula> regAnterior = filtrarAsistenciasPorPeriodo(todosRegistros, anterior);
      if (regAnterior.isEmpty()) {
        return null;
      }
      long asisAnt = regAnterior.stream().filter(AsistenciaAula::esPresente).count();
      double porcAnt = Math.round((asisAnt * 1000.0) / regAnterior.size()) / 10.0;
      return Math.round((porcentajeActual - porcAnt) * 10.0) / 10.0;
    } catch (Exception e) {
      return null;
    }
  }

  private Short resolverAnioId(Long anioLectivoIdParam) {
    if (anioLectivoIdParam != null) {
      return anioLectivoIdParam.shortValue();
    }
    try {
      int anioHoy = LocalDate.now(ZoneId.of("America/Lima")).getYear();
      List<AnioLectivo> lista = anios.listar();
      if (lista == null || lista.isEmpty()) {
        return null;
      }
      return lista.stream()
          .filter(a -> a.getAnio() != null && a.getAnio() == anioHoy)
          .map(AnioLectivo::getId)
          .findFirst()
          .orElseGet(() -> lista.get(0).getId());
    } catch (Exception e) {
      return null;
    }
  }

  private PeriodoAcademico resolverPeriodo(Short anioId, Long periodoId) {
    if (periodos == null || periodoId == null || anioId == null) {
      return null;
    }
    try {
      return periodos.buscarPorAnioLectivoId(anioId).stream()
          .filter(p -> p.getId() != null && p.getId().longValue() == periodoId.longValue())
          .findFirst()
          .orElse(null);
    } catch (Exception e) {
      return null;
    }
  }

  private PeriodoAcademico resolverPeriodoVigente(Short anioId, LocalDate hoy) {
    if (periodos == null || anioId == null) {
      return null;
    }
    try {
      List<PeriodoAcademico> lista = periodos.buscarPorAnioLectivoId(anioId);
      if (lista == null || lista.isEmpty()) {
        return null;
      }
      List<PeriodoAcademico> ordenados =
          lista.stream()
              .filter(Objects::nonNull)
              .sorted(
                  Comparator.comparing(
                          PeriodoAcademico::getNumeroPeriodo,
                          Comparator.nullsLast(Comparator.naturalOrder()))
                      .thenComparing(
                          PeriodoAcademico::getId,
                          Comparator.nullsLast(Comparator.naturalOrder())))
              .toList();
      if (ordenados.isEmpty()) {
        return null;
      }
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
          .orElseGet(() -> ordenados.get(ordenados.size() - 1));
    } catch (Exception e) {
      return null;
    }
  }

  private Map<Short, Nivel> cargarMapaNiveles() {
    if (niveles == null) return Map.of();
    try {
      List<Nivel> lista = niveles.listar();
      if (lista == null) return Map.of();
      return lista.stream()
          .filter(n -> n != null && n.getId() != null)
          .collect(Collectors.toMap(Nivel::getId, n -> n, (a, b) -> a));
    } catch (Exception e) {
      return Map.of();
    }
  }

  private Map<Short, Grado> cargarMapaGrados() {
    if (grados == null) return Map.of();
    try {
      List<Grado> lista = grados.listar();
      if (lista == null) return Map.of();
      return lista.stream()
          .filter(g -> g != null && g.getId() != null)
          .collect(Collectors.toMap(Grado::getId, g -> g, (a, b) -> a));
    } catch (Exception e) {
      return Map.of();
    }
  }

  private List<Seccion> filtrarSecciones(
      Short anioId,
      String nivelFiltro,
      Long gradoIdFiltro,
      Long seccionIdFiltro,
      Map<Short, Nivel> mapaNiveles,
      Map<Short, Grado> mapaGrados) {
    if (secciones == null || anioId == null) return List.of();
    try {
      List<Seccion> lista = secciones.buscarPorAnioLectivoId(anioId);
      if (lista == null) return List.of();
      return lista.stream()
          .filter(Objects::nonNull)
          .filter(
              s -> {
                if (seccionIdFiltro != null
                    && (s.getId() == null || s.getId().longValue() != seccionIdFiltro.longValue())) {
                  return false;
                }
                if (gradoIdFiltro != null
                    && (s.getGradoId() == null
                        || s.getGradoId().longValue() != gradoIdFiltro.longValue())) {
                  return false;
                }
                if (nivelFiltro != null && !nivelFiltro.isBlank()) {
                  Nivel niv = mapaNiveles.get(s.getNivelId());
                  if (niv == null && s.getGradoId() != null) {
                    Grado gr = mapaGrados.get(s.getGradoId());
                    if (gr != null) niv = mapaNiveles.get(gr.getNivelId());
                  }
                  String cod =
                      niv != null && niv.getCodigo() != null ? niv.getCodigo().name() : "";
                  String nom = niv != null && niv.getNombre() != null ? niv.getNombre() : "";
                  if (!cod.equalsIgnoreCase(nivelFiltro.trim())
                      && !nom.equalsIgnoreCase(nivelFiltro.trim())
                      && (s.getNivelId() == null
                          || !String.valueOf(s.getNivelId()).equals(nivelFiltro.trim()))) {
                    return false;
                  }
                }
                return true;
              })
          .sorted(
              Comparator.comparing(
                      (Seccion s) -> s.getGradoId() != null ? s.getGradoId() : (short) 0)
                  .thenComparing(s -> s.getLetra() != null ? s.getLetra() : ""))
          .toList();
    } catch (Exception e) {
      return List.of();
    }
  }

  private Map<Long, Estudiante> cargarMapaEstudiantes(List<Matricula> matriculasActivas) {
    if (estudiantes == null || matriculasActivas.isEmpty()) return Map.of();
    try {
      Set<Long> estIds =
          matriculasActivas.stream()
              .map(Matricula::getEstudianteId)
              .filter(Objects::nonNull)
              .collect(Collectors.toSet());
      if (estIds.isEmpty()) return Map.of();
      List<Estudiante> encontrados = estudiantes.buscarPorIds(estIds);
      if (encontrados == null) return Map.of();
      Map<Long, Estudiante> map = new HashMap<>();
      for (Estudiante e : encontrados) {
        if (e != null && e.getId() != null) {
          map.put(e.getId(), e);
        }
      }
      return map;
    } catch (Exception e) {
      return Map.of();
    }
  }

  private String formatearGradoSeccion(
      Integer seccionId, Map<Integer, Seccion> mapaSecciones, Map<Short, Grado> mapaGrados) {
    if (seccionId == null) return "Sin sección";
    Seccion s = mapaSecciones.get(seccionId);
    if (s == null) return "Sección #" + seccionId;
    Grado g = mapaGrados.get(s.getGradoId());
    String nombreGrado = g != null && g.getNombre() != null ? g.getNombre() : "Grado " + s.getGradoId();
    return nombreGrado + " \"" + (s.getLetra() != null ? s.getLetra() : "") + "\"";
  }
}
