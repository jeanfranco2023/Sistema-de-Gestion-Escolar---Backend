package com.colegio.shuji.matricula.application.service;

import static com.colegio.shuji.shared.domain.model.Reglas.requerido;

import com.colegio.shuji.matricula.application.port.in.ConsultarPortalFamiliarUseCase;
import com.colegio.shuji.matricula.application.port.out.ConsultaPortalDatosPort;
import com.colegio.shuji.matricula.application.port.out.AccesoFamiliarPort;
import com.colegio.shuji.matricula.application.port.out.MatriculaRepositoryPort;
import com.colegio.shuji.matricula.application.dto.out.HijoPortalResponseDto;
import com.colegio.shuji.academico.application.dto.out.PeriodoResponseDto;
import com.colegio.shuji.academico.application.port.in.GestionarAnioLectivoUseCase;
import com.colegio.shuji.asistencia.application.dto.out.AsistenciaAulaResponseDto;
import com.colegio.shuji.shared.application.port.out.ActorActualPort;
import com.colegio.shuji.academico.application.port.out.AnioLectivoRepositoryPort;
import com.colegio.shuji.curriculo.application.port.out.AsignacionRepositoryPort;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortalFamiliarService implements ConsultarPortalFamiliarUseCase {
  private final ConsultaPortalDatosPort datos;
  private final AccesoFamiliarPort acceso;
  private final MatriculaRepositoryPort matriculas;
  private final GestionarAnioLectivoUseCase calendario;
  private final ActorActualPort actor;
  private final AsignacionRepositoryPort asignaciones;
  private final AnioLectivoRepositoryPort anios;

  private boolean gestion() { return actor.tieneRol("DIRECCION"); }

  public List<HijoPortalResponseDto> hijos() { return datos.hijos(gestion() ? null : actor.usuarioId()); }
  public List<HijoPortalResponseDto> hijosPropios() { return datos.hijos(actor.usuarioId()); }

  public List<HijoPortalResponseDto> alumnosSeguimiento() {
    if (!gestion() && !actor.tieneRol("TUTOR") && !actor.tieneRol("AUXILIAR")) throw new AccessDeniedException("Se requiere un rol de seguimiento escolar");
    if (actor.tieneRol("TUTOR")) {
      var secciones = seccionesAsignadas();
      if (secciones.isEmpty()) return List.of();
      return datos.hijosDeSecciones(secciones.keySet()).stream()
          .filter(h -> secciones.getOrDefault(h.seccionId(), Set.of()).contains(h.anioLectivoId()))
          .toList();
    }
    return datos.hijos(null);
  }

  public List<PeriodoResponseDto> periodosSeguimiento(Short anioId) {
    if (actor.tieneRol("TUTOR")
        && seccionesAsignadas().values().stream().noneMatch(anios -> anios.contains(anioId))) {
      throw new AccessDeniedException("El año lectivo no pertenece a una sección asignada al tutor");
    }
    return calendario.listarPeriodos(anioId);
  }

  public List<com.colegio.shuji.evaluacion.application.dto.out.CalificacionRiesgoTutorResponseDto>
      calificacionesRiesgoTutor() {
    if (!actor.tieneRol("TUTOR")) throw new AccessDeniedException("Se requiere el rol TUTOR");
    var ids = alumnosSeguimiento().stream().map(HijoPortalResponseDto::matriculaId).distinct().toList();
    return datos.calificacionesRiesgo(ids);
  }

  private Map<Integer, Set<Short>> seccionesAsignadas() {
    var aniosAbiertos =
        anios.listar().stream().filter(a -> a.estaAbierto()).map(a -> a.getId()).collect(Collectors.toSet());
    return asignaciones.buscarPorDocenteUsuarioId(actor.usuarioId()).stream()
        .filter(a -> a.getSeccionId() != null && aniosAbiertos.contains(a.getAnioLectivoId()))
        .collect(Collectors.groupingBy(
            a -> a.getSeccionId(),
            Collectors.mapping(a -> a.getAnioLectivoId(), Collectors.toSet())));
  }

  private com.colegio.shuji.matricula.domain.model.Matricula autorizar(Long id) {
    var matricula = requerido(matriculas.buscarPorId(id));
    if (!gestion() && !acceso.tieneCustodia(matricula.getEstudianteId(), actor.usuarioId()))
      throw new AccessDeniedException("La matrícula no corresponde a un hijo bajo tu custodia");
    return matricula;
  }

  public List<AsistenciaAulaResponseDto> asistencia(Long matriculaId) {
    autorizar(matriculaId); return datos.asistencia(matriculaId);
  }

  public List<PeriodoResponseDto> periodos(Long matriculaId) {
    var matricula = autorizar(matriculaId); return calendario.listarPeriodos(matricula.getAnioLectivoId());
  }

  public List<com.colegio.shuji.evaluacion.application.dto.out.CalificacionPortalResponseDto> calificaciones(Long matriculaId) {
    autorizar(matriculaId); return datos.calificaciones(matriculaId);
  }
}
