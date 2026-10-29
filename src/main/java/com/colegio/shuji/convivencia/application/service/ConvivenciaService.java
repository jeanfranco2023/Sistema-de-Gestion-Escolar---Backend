package com.colegio.shuji.convivencia.application.service;

import static com.colegio.shuji.shared.domain.model.Reglas.exigir;
import static com.colegio.shuji.shared.domain.model.Reglas.requerido;

import com.colegio.shuji.convivencia.application.dto.in.ActualizarEstadoIncidenciaRequestDto;
import com.colegio.shuji.convivencia.application.dto.in.RegistrarIncidenciaRequestDto;
import com.colegio.shuji.convivencia.application.dto.out.HistorialConductualEstudianteResponseDto;
import com.colegio.shuji.convivencia.application.dto.out.IncidenciaResponseDto;
import com.colegio.shuji.convivencia.application.mapper.ConvivenciaMapper;
import com.colegio.shuji.convivencia.application.port.in.GestionarIncidenciasUseCase;
import com.colegio.shuji.convivencia.application.port.in.RegistrarIncidenciaUseCase;
import com.colegio.shuji.convivencia.application.port.out.IncidenciaRepositoryPort;
import com.colegio.shuji.convivencia.domain.enums.EstadoIncidencia;
import com.colegio.shuji.convivencia.domain.enums.TipoFalta;
import com.colegio.shuji.matricula.application.port.out.MatriculaRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.EstudianteRepositoryPort;
import com.colegio.shuji.matricula.domain.enums.EstadoMatricula;
import com.colegio.shuji.academico.application.port.out.AnioLectivoRepositoryPort;
import com.colegio.shuji.curriculo.application.port.out.AsignacionRepositoryPort;
import com.colegio.shuji.matricula.domain.model.Matricula;
import com.colegio.shuji.shared.application.port.out.ActorActualPort;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ConvivenciaService implements RegistrarIncidenciaUseCase, GestionarIncidenciasUseCase {
  private final ConvivenciaMapper mapper;
  private final IncidenciaRepositoryPort incidencias;
  private final MatriculaRepositoryPort matriculas;
  private final EstudianteRepositoryPort estudiantes;
  private final ActorActualPort actor;
  private final AsignacionRepositoryPort asignaciones;
  private final AnioLectivoRepositoryPort anios;

  @Transactional(readOnly = true)
  public List<IncidenciaResponseDto> listar() {
    if (actor.tieneRol("TUTOR")) {
      var ids = matriculasAsignadasDelTutor().stream().map(Matricula::getId).toList();
      return conNombres(incidencias.buscarPorMatriculaIds(ids));
    }
    return conNombres(incidencias.listar());
  }

  public IncidenciaResponseDto registrar(RegistrarIncidenciaRequestDto r) {
    var m = requerido(matriculas.buscarPorId(r.matriculaId()));
    autorizarSiTutor(m);
    exigir(m.getEstadoMatricula() == EstadoMatricula.MATRICULADO, "Matrícula no activa");
    var i = mapper.toDomain(r);
    i.abrir(actor.usuarioId());
    return conNombres(List.of(incidencias.guardar(i))).getFirst();
  }

  public IncidenciaResponseDto actualizar(Long id, ActualizarEstadoIncidenciaRequestDto r) {
    var i =
        incidencias
            .bloquearPorId(id)
            .orElseThrow(
                () ->
                    new com.colegio.shuji.convivencia.domain.exception
                        .IncidenciaNoEncontradaException("Incidencia no encontrada"));
    if (actor.tieneRol("TUTOR")) {
      autorizarSiTutor(requerido(matriculas.buscarPorId(i.getMatriculaId())));
    }
    i.cambiarEstado(r.estado());
    return conNombres(List.of(incidencias.guardar(i))).getFirst();
  }

  @Transactional(readOnly = true)
  public HistorialConductualEstudianteResponseDto historial(Long matriculaId) {
    if (actor.tieneRol("TUTOR")) autorizarSiTutor(requerido(matriculas.buscarPorId(matriculaId)));
    return new HistorialConductualEstudianteResponseDto(
        matriculaId,
        conNombres(incidencias.buscarPorMatriculaId(matriculaId)));
  }

  @Transactional(readOnly = true)
  public List<IncidenciaResponseDto> citaciones() {
    var candidatas = actor.tieneRol("TUTOR")
        ? incidencias.buscarPorMatriculaIds(
            matriculasAsignadasDelTutor().stream().map(Matricula::getId).toList())
        : incidencias.buscarPorEstado(EstadoIncidencia.ABIERTA);
    return conNombres(candidatas.stream()
        .filter(i -> i.getEstado() == EstadoIncidencia.ABIERTA)
        .filter(i -> Boolean.TRUE.equals(i.getRequiereCitacion()) && i.getTipoFalta() != TipoFalta.LEVE)
        .toList());
  }

  private List<com.colegio.shuji.convivencia.application.dto.out.IncidenciaResponseDto> conNombres(
      List<com.colegio.shuji.convivencia.domain.model.IncidenciaConductual> registros) {
    if (registros.isEmpty()) return List.of();
    var matriculasPorId = matriculas.buscarPorIds(registros.stream()
        .map(com.colegio.shuji.convivencia.domain.model.IncidenciaConductual::getMatriculaId).distinct().toList())
        .stream().collect(Collectors.toMap(Matricula::getId, java.util.function.Function.identity()));
    var estudiantesPorId = estudiantes.buscarPorIds(matriculasPorId.values().stream()
        .map(Matricula::getEstudianteId).distinct().toList()).stream()
        .collect(Collectors.toMap(com.colegio.shuji.matricula.domain.model.Estudiante::getId,
            com.colegio.shuji.matricula.domain.model.Estudiante::nombreCompleto));
    return registros.stream().map(registro -> {
      var dto = mapper.toResponse(registro);
      var matricula = matriculasPorId.get(registro.getMatriculaId());
      String nombre = matricula == null ? null : estudiantesPorId.get(matricula.getEstudianteId());
      return new com.colegio.shuji.convivencia.application.dto.out.IncidenciaResponseDto(
          dto.id(), dto.matriculaId(), dto.fechaIncidencia(), dto.tipoFalta(), dto.descripcion(),
          dto.reportadoPorUsuarioId(), dto.requiereCitacion(), dto.estado(), dto.fechaCreacion(),
          nombre == null ? "Estudiante sin datos" : nombre);
    }).toList();
  }

  private void autorizarSiTutor(Matricula matricula) {
    if (!actor.tieneRol("TUTOR")) return;
    var aniosAbiertos =
        anios.listar().stream().filter(a -> a.estaAbierto()).map(a -> a.getId()).collect(Collectors.toSet());
    boolean asignada = asignaciones.buscarPorDocenteUsuarioId(actor.usuarioId()).stream()
        .anyMatch(a -> a.getSeccionId() != null
            && a.getSeccionId().equals(matricula.getSeccionId())
            && a.getAnioLectivoId().equals(matricula.getAnioLectivoId())
            && aniosAbiertos.contains(a.getAnioLectivoId()));
    if (!asignada) throw new AccessDeniedException("La matrícula no pertenece a una sección asignada al tutor");
  }

  private List<Matricula> matriculasAsignadasDelTutor() {
    var aniosAbiertos =
        anios.listar().stream().filter(a -> a.estaAbierto()).map(a -> a.getId()).collect(Collectors.toSet());
    var secciones = asignaciones.buscarPorDocenteUsuarioId(actor.usuarioId()).stream()
        .filter(a -> a.getSeccionId() != null && aniosAbiertos.contains(a.getAnioLectivoId()))
        .collect(Collectors.groupingBy(a -> a.getSeccionId(),
            Collectors.mapping(a -> a.getAnioLectivoId(), Collectors.toSet())));
    if (secciones.isEmpty()) return List.of();
    return matriculas.buscarPorSeccionIds(secciones.keySet()).stream()
        .filter(Matricula::estaMatriculado)
        .filter(m -> secciones.getOrDefault(m.getSeccionId(), Set.of()).contains(m.getAnioLectivoId()))
        .toList();
  }
}
