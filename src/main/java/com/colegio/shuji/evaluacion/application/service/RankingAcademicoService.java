package com.colegio.shuji.evaluacion.application.service;

import com.colegio.shuji.academico.application.port.out.GradoRepositoryPort;
import com.colegio.shuji.academico.application.port.out.NivelRepositoryPort;
import com.colegio.shuji.academico.application.port.out.SeccionRepositoryPort;
import com.colegio.shuji.academico.domain.model.Nivel;
import com.colegio.shuji.academico.domain.model.Seccion;
import com.colegio.shuji.asistencia.application.port.out.AsistenciaAulaRepositoryPort;
import com.colegio.shuji.asistencia.domain.enums.EstadoAsistenciaAula;
import com.colegio.shuji.asistencia.domain.model.AsistenciaAula;
import com.colegio.shuji.convivencia.application.port.out.IncidenciaRepositoryPort;
import com.colegio.shuji.convivencia.domain.model.IncidenciaConductual;
import com.colegio.shuji.curriculo.application.port.out.AsignacionRepositoryPort;
import com.colegio.shuji.curriculo.domain.model.AsignacionDocente;
import com.colegio.shuji.evaluacion.application.dto.out.CuadroHonorResponseDto;
import com.colegio.shuji.evaluacion.application.dto.out.RankingEstudianteResponseDto;
import com.colegio.shuji.evaluacion.application.port.in.ConsultarRankingAcademicoUseCase;
import com.colegio.shuji.evaluacion.application.port.out.CalificacionRepositoryPort;
import com.colegio.shuji.evaluacion.domain.enums.CalificacionCualitativa;
import com.colegio.shuji.evaluacion.domain.model.CalificacionCneb;
import com.colegio.shuji.matricula.application.port.out.EstudianteRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.MatriculaRepositoryPort;
import com.colegio.shuji.matricula.domain.enums.EstadoMatricula;
import com.colegio.shuji.matricula.domain.model.Estudiante;
import com.colegio.shuji.matricula.domain.model.Matricula;
import com.colegio.shuji.shared.application.port.out.ActorActualPort;
import com.colegio.shuji.shared.domain.exception.BusinessException;
import com.colegio.shuji.shared.domain.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RankingAcademicoService implements ConsultarRankingAcademicoUseCase {

  public static final String DISCLAIMER_INSTITUCIONAL =
      "El puntaje es un cálculo interno (AD=4, A=3, B=2, C=1), no una escala oficial del MINEDU. "
          + "El ranking no otorga becas ni beneficios económicos automáticos.";

  private final MatriculaRepositoryPort matriculas;
  private final EstudianteRepositoryPort estudiantes;
  private final SeccionRepositoryPort secciones;
  private final GradoRepositoryPort grados;
  private final NivelRepositoryPort niveles;
  private final CalificacionRepositoryPort calificaciones;
  private final AsistenciaAulaRepositoryPort asistencias;
  private final IncidenciaRepositoryPort incidencias;
  private final AsignacionRepositoryPort asignaciones;
  private final ActorActualPort actor;

  @Override
  public CuadroHonorResponseDto obtenerRankingSeccion(Integer seccionId) {
    var seccion = secciones.buscarPorId(seccionId)
        .orElseThrow(() -> new ResourceNotFoundException("Sección", "id", seccionId));
    verificarAlcanceSeccion(seccionId);
    var grado = grados.buscarPorId(seccion.getGradoId())
        .orElseThrow(() -> new ResourceNotFoundException("Grado", "id", seccion.getGradoId()));
    var nivelNombre = niveles.buscarPorId(grado.getNivelId())
        .map(Nivel::getNombre)
        .orElse("Nivel");

    List<Matricula> inscripciones = matriculas.buscarPorSeccionId(seccionId).stream()
        .filter(m -> m.getEstadoMatricula() == EstadoMatricula.MATRICULADO)
        .toList();
    return new CuadroHonorResponseDto(
        seccion.getAnioLectivoId(), seccion.getGradoId(), seccionId,
        nivelNombre, grado.getNombre(), seccion.getLetra(), DISCLAIMER_INSTITUCIONAL,
        calcularYOrdenarRanking(inscripciones));
  }

  @Override
  public CuadroHonorResponseDto obtenerCuadroHonor(Short gradoId, Short anioLectivoId) {
    if (anioLectivoId == null) {
      throw new BusinessException("El año lectivo es obligatorio para consultar el cuadro de honor");
    }
    String gradoNombre = "Todos los grados";
    String nivelNombre = "Nivel institucional";
    if (gradoId != null) {
      var grado = grados.buscarPorId(gradoId)
          .orElseThrow(() -> new ResourceNotFoundException("Grado", "id", gradoId));
      gradoNombre = grado.getNombre();
      nivelNombre = niveles.buscarPorId(grado.getNivelId())
          .map(Nivel::getNombre)
          .orElse("Nivel");
    }

    List<Seccion> candidatas = gradoId == null
        ? secciones.buscarPorAnioLectivoId(anioLectivoId)
        : secciones.buscarPorGradoId(gradoId);
    Set<Integer> asignadas = seccionesAsignadasSiRestringido();
    List<Seccion> alcance = candidatas.stream()
        .filter(s -> anioLectivoId == null || Objects.equals(s.getAnioLectivoId(), anioLectivoId))
        .filter(s -> gradoId == null || Objects.equals(s.getGradoId(), gradoId))
        .filter(s -> asignadas == null || asignadas.contains(s.getId()))
        .toList();
    List<Integer> idsSeccion = alcance.stream()
        .map(Seccion::getId)
        .filter(Objects::nonNull)
        .toList();
    List<Matricula> inscripciones = idsSeccion.isEmpty() ? List.of()
        : matriculas.buscarPorSeccionIds(idsSeccion).stream()
            .filter(m -> m.getEstadoMatricula() == EstadoMatricula.MATRICULADO)
            .toList();
    return new CuadroHonorResponseDto(
        anioLectivoId, gradoId, null, nivelNombre, gradoNombre, null,
        DISCLAIMER_INSTITUCIONAL, calcularYOrdenarRanking(inscripciones));
  }

  private void verificarAlcanceSeccion(Integer seccionId) {
    Set<Integer> asignadas = seccionesAsignadasSiRestringido();
    if (asignadas != null && !asignadas.contains(seccionId)) {
      throw new AccessDeniedException("No tiene una asignación vigente para esta sección");
    }
  }

  private Set<Integer> seccionesAsignadasSiRestringido() {
    if (actor.tieneRol("DIRECCION") || actor.tieneRol("SECRETARIA")) return null;
    if (!actor.tieneRol("DOCENTE") && !actor.tieneRol("TUTOR")) {
      throw new AccessDeniedException("No tiene acceso al ranking académico");
    }
    return asignaciones.buscarPorDocenteUsuarioId(actor.usuarioId()).stream()
        .map(AsignacionDocente::getSeccionId)
        .filter(Objects::nonNull)
        .collect(Collectors.toUnmodifiableSet());
  }

  private List<RankingEstudianteResponseDto> calcularYOrdenarRanking(List<Matricula> inscripciones) {
    if (inscripciones.isEmpty()) return List.of();
    List<Long> matriculaIds = inscripciones.stream()
        .map(Matricula::getId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    if (matriculaIds.isEmpty()) return List.of();

    Map<Long, Estudiante> estudiantesPorId = estudiantes.buscarPorIds(inscripciones.stream()
            .map(Matricula::getEstudianteId).filter(Objects::nonNull).distinct().toList())
        .stream().collect(Collectors.toMap(Estudiante::getId, Function.identity(), (a, b) -> a));
    Map<Long, List<CalificacionCneb>> calificacionesPorMatricula =
        calificaciones.buscarPorMatriculaIds(matriculaIds).stream()
            .collect(Collectors.groupingBy(CalificacionCneb::getMatriculaId));
    Map<Long, List<AsistenciaAula>> asistenciasPorMatricula =
        asistencias.buscarPorMatriculaIds(matriculaIds).stream()
            .collect(Collectors.groupingBy(AsistenciaAula::getMatriculaId));
    Map<Long, List<IncidenciaConductual>> incidenciasPorMatricula =
        incidencias.buscarPorMatriculaIds(matriculaIds).stream()
            .collect(Collectors.groupingBy(IncidenciaConductual::getMatriculaId));

    List<RankingBuilder> ranking = new java.util.ArrayList<>();
    for (Matricula matricula : inscripciones) {
      Estudiante estudiante = estudiantesPorId.get(matricula.getEstudianteId());
      if (estudiante == null) continue;
      var notas = calificacionesPorMatricula.getOrDefault(matricula.getId(), List.of()).stream()
          .filter(c -> c.getCalificacionCualitativa() != null)
          .toList();
      if (notas.isEmpty()) continue;

      int ad = 0;
      int a = 0;
      int b = 0;
      int c = 0;
      for (var nota : notas) {
        switch (nota.getCalificacionCualitativa()) {
          case AD -> ad++;
          case A -> a++;
          case B -> b++;
          case C -> c++;
        }
      }
      int totalNotas = ad + a + b + c;
      double promedioInterno = BigDecimal.valueOf(ad * 4L + a * 3L + b * 2L + c)
          .divide(BigDecimal.valueOf(totalNotas), 2, RoundingMode.HALF_UP).doubleValue();

      List<AsistenciaAula> asistenciasEstudiante =
          asistenciasPorMatricula.getOrDefault(matricula.getId(), List.of());
      long asistenciasValidas = asistenciasEstudiante.stream()
          .filter(x -> x.getEstado() == EstadoAsistenciaAula.PRESENTE
              || x.getEstado() == EstadoAsistenciaAula.TARDANZA
              || x.getEstado() == EstadoAsistenciaAula.FALTA_JUSTIFICADA)
          .count();
      double porcentajeAsistencia = asistenciasEstudiante.isEmpty() ? 0.0
          : BigDecimal.valueOf(asistenciasValidas).multiply(BigDecimal.valueOf(100))
              .divide(BigDecimal.valueOf(asistenciasEstudiante.size()), 2, RoundingMode.HALF_UP)
              .doubleValue();
      int totalIncidencias = incidenciasPorMatricula.getOrDefault(matricula.getId(), List.of()).size();
      String apellidos = (estudiante.getApellidoPaterno() + " "
          + (estudiante.getApellidoMaterno() == null ? "" : estudiante.getApellidoMaterno())).trim();
      ranking.add(new RankingBuilder(
          matricula.getId(), estudiante.getId(),
          estudiante.getTipoDocumento() == null ? "DNI" : estudiante.getTipoDocumento().name(),
          estudiante.getNumeroDocumento(), estudiante.getNombres(), apellidos,
          (estudiante.getNombres() + " " + apellidos).trim(), promedioInterno,
          porcentajeAsistencia, totalIncidencias, ad, a, b, c));
    }

    ranking.sort(Comparator.comparing(RankingBuilder::puntajePromedioInterno, Comparator.reverseOrder())
        .thenComparing(RankingBuilder::porcentajeAsistencia, Comparator.reverseOrder())
        .thenComparing(RankingBuilder::totalIncidencias)
        .thenComparing(RankingBuilder::matriculaId));

    List<RankingEstudianteResponseDto> resultado = new java.util.ArrayList<>(ranking.size());
    int puesto = 0;
    RankingBuilder anterior = null;
    for (int i = 0; i < ranking.size(); i++) {
      RankingBuilder actual = ranking.get(i);
      if (anterior == null || compararMerito(anterior, actual) != 0) puesto = i + 1;
      resultado.add(new RankingEstudianteResponseDto(
          puesto, actual.matriculaId(), actual.estudianteId(), actual.tipoDocumento(),
          actual.numeroDocumento(), actual.nombres(), actual.apellidos(), actual.nombreCompleto(),
          actual.puntajePromedioInterno(), actual.porcentajeAsistencia(), actual.totalIncidencias(),
          actual.cantAd(), actual.cantA(), actual.cantB(), actual.cantC(), mencion(puesto), null));
      anterior = actual;
    }
    return List.copyOf(resultado);
  }

  private static int compararMerito(RankingBuilder primero, RankingBuilder segundo) {
    int promedio = Double.compare(segundo.puntajePromedioInterno(), primero.puntajePromedioInterno());
    if (promedio != 0) return promedio;
    int asistencia = Double.compare(segundo.porcentajeAsistencia(), primero.porcentajeAsistencia());
    if (asistencia != 0) return asistencia;
    return Integer.compare(primero.totalIncidencias(), segundo.totalIncidencias());
  }

  private static String mencion(int puesto) {
    return switch (puesto) {
      case 1 -> "Primer puesto - reconocimiento académico";
      case 2 -> "Segundo puesto - reconocimiento académico";
      case 3 -> "Tercer puesto - reconocimiento académico";
      default -> "Reconocimiento académico";
    };
  }

  private record RankingBuilder(
      Long matriculaId,
      Long estudianteId,
      String tipoDocumento,
      String numeroDocumento,
      String nombres,
      String apellidos,
      String nombreCompleto,
      Double puntajePromedioInterno,
      Double porcentajeAsistencia,
      Integer totalIncidencias,
      Integer cantAd,
      Integer cantA,
      Integer cantB,
      Integer cantC) {}
}
