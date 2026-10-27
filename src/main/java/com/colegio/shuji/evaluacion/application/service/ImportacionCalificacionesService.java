package com.colegio.shuji.evaluacion.application.service;

import static com.colegio.shuji.shared.domain.model.Reglas.exigir;
import static com.colegio.shuji.shared.domain.model.Reglas.requerido;

import com.colegio.shuji.academico.application.port.out.PeriodoRepositoryPort;
import com.colegio.shuji.curriculo.application.port.out.AsignacionRepositoryPort;
import com.colegio.shuji.curriculo.application.port.out.CompetenciaRepositoryPort;
import com.colegio.shuji.evaluacion.application.dto.in.CalificacionItemRequestDto;
import com.colegio.shuji.evaluacion.application.dto.in.RegistrarCalificacionesMasivasRequestDto;
import com.colegio.shuji.evaluacion.application.dto.out.ImportacionCalificacionesResponseDto;
import com.colegio.shuji.evaluacion.application.port.in.ImportarCalificacionesUseCase;
import com.colegio.shuji.evaluacion.application.port.in.RegistrarEvaluacionCnebUseCase;
import com.colegio.shuji.evaluacion.domain.enums.CalificacionCualitativa;
import com.colegio.shuji.evaluacion.domain.exception.ImportacionCalificacionesException;
import com.colegio.shuji.matricula.application.port.out.EstudianteRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.MatriculaRepositoryPort;
import com.colegio.shuji.matricula.domain.enums.EstadoMatricula;
import com.colegio.shuji.shared.application.port.out.ActorActualPort;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ImportacionCalificacionesService implements ImportarCalificacionesUseCase {
  public static final List<String> CABECERAS = List.of("DNI_ESTUDIANTE", "COMPETENCIA_ID", "NOTA_CNEB", "CONCLUSION_DESCRIPTIVA");
  private final PeriodoRepositoryPort periodos;
  private final AsignacionRepositoryPort asignaciones;
  private final CompetenciaRepositoryPort competencias;
  private final MatriculaRepositoryPort matriculas;
  private final EstudianteRepositoryPort estudiantes;
  private final ActorActualPort actor;
  private final RegistrarEvaluacionCnebUseCase registro;

  public ImportacionCalificacionesResponseDto importar(List<List<String>> filas, Short periodoId, Integer seccionId,
      Long asignacionId, boolean confirmar) {
    var asignacion = requerido(asignaciones.buscarPorId(asignacionId));
    actor.verificarDocente(asignacion.getDocenteUsuarioId());
    var periodo = requerido(periodos.buscarPorId(periodoId));
    exigir(asignacion.getSeccionId().equals(seccionId) && asignacion.getAnioLectivoId().equals(periodo.getAnioLectivoId()),
        "La sección o el periodo no corresponde a la asignación");
    periodo.verificarActivo(); periodo.verificarAbierto();
    if (filas.isEmpty() || !filas.getFirst().equals(CABECERAS))
      throw new ImportacionCalificacionesException(java.util.Map.of("cabecera", "Usa la plantilla oficial, con sus cuatro columnas y en el mismo orden"));
    if (filas.size() < 2 || filas.size() > 501)
      throw new ImportacionCalificacionesException(java.util.Map.of("archivo", "Se requieren entre 1 y 500 filas"));
    var errores = new LinkedHashMap<String, String>();
    var alumnos = estudiantes.buscarPorNumerosDocumento(filas.stream().skip(1).filter(f -> !f.isEmpty())
        .map(f -> f.getFirst().strip()).filter(d -> d.matches("[0-9]{8}")).distinct().toList()).stream()
        .collect(Collectors.toMap(e -> e.getNumeroDocumento(), Function.identity()));
    var matriculados = matriculas.buscarPorSeccionId(seccionId).stream()
        .filter(m -> m.getAnioLectivoId().equals(periodo.getAnioLectivoId()) && m.getEstadoMatricula() == EstadoMatricula.MATRICULADO)
        .collect(Collectors.toMap(m -> m.getEstudianteId(), Function.identity()));
    var competenciasValidas = competencias.buscarPorAreaId(asignacion.getAreaCurricularId()).stream()
        .map(c -> c.getId()).collect(Collectors.toSet());
    var claves = new HashSet<String>();
    var items = new ArrayList<CalificacionItemRequestDto>();
    var vista = new ArrayList<ImportacionCalificacionesResponseDto.Fila>();
    for (int i = 1; i < filas.size(); i++) {
      var fila = filas.get(i); String prefijo = "fila[" + (i + 1) + "].";
      if (fila.size() != 4) { errores.put(prefijo + "columnas", "Se esperan cuatro columnas"); continue; }
      String dni = fila.get(0).strip(), textoCompetencia = fila.get(1).strip(), nota = fila.get(2).strip();
      String conclusion = fila.get(3).strip();
      var estudiante = alumnos.get(dni);
      var matricula = estudiante == null ? null : matriculados.get(estudiante.getId());
      if (!dni.matches("[0-9]{8}") || matricula == null || !Boolean.TRUE.equals(estudiante.getActivo()))
        errores.put(prefijo + "DNI_ESTUDIANTE", "DNI de ocho dígitos de un estudiante activo y matriculado en esta sección");
      Short competencia = null;
      try { competencia = Short.valueOf(textoCompetencia); } catch (NumberFormatException ignored) {}
      if (competencia == null || !competenciasValidas.contains(competencia))
        errores.put(prefijo + "COMPETENCIA_ID", "La competencia no pertenece al área de la asignación");
      if (!nota.matches("AD|A|B|C")) errores.put(prefijo + "NOTA_CNEB", "Solo AD, A, B o C");
      if (conclusion.length() > 4000) errores.put(prefijo + "CONCLUSION_DESCRIPTIVA", "Máximo 4000 caracteres");
      if (!claves.add(dni + "/" + textoCompetencia)) errores.put(prefijo + "duplicado", "DNI y competencia repetidos");
      if (errores.keySet().stream().anyMatch(k -> k.startsWith(prefijo))) continue;
      items.add(new CalificacionItemRequestDto(matricula.getId(), competencia, CalificacionCualitativa.valueOf(nota), conclusion, false));
      vista.add(new ImportacionCalificacionesResponseDto.Fila(i + 1, dni, estudiante.nombreCompleto(), competencia, nota, conclusion));
    }
    if (!errores.isEmpty()) throw new ImportacionCalificacionesException(errores);
    if (confirmar) registro.registrar(new RegistrarCalificacionesMasivasRequestDto(periodoId, asignacionId, items));
    return new ImportacionCalificacionesResponseDto(items.size(), confirmar ? items.size() : 0, vista);
  }
}
