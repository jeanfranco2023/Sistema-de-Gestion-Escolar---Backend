package com.colegio.shuji.curriculo.application.service;

import static com.colegio.shuji.shared.domain.model.Reglas.exigir;
import static com.colegio.shuji.shared.domain.model.Reglas.horas;
import static com.colegio.shuji.shared.domain.model.Reglas.requerido;

import com.colegio.shuji.academico.application.port.out.AnioLectivoRepositoryPort;
import com.colegio.shuji.academico.application.port.out.NivelRepositoryPort;
import com.colegio.shuji.academico.application.port.out.SeccionRepositoryPort;
import com.colegio.shuji.curriculo.application.dto.in.AsignarDocenteRequestDto;
import com.colegio.shuji.curriculo.application.dto.in.CrearAreaRequestDto;
import com.colegio.shuji.curriculo.application.dto.in.CrearBloqueRequestDto;
import com.colegio.shuji.curriculo.application.dto.in.CrearCompetenciaRequestDto;
import com.colegio.shuji.curriculo.application.dto.out.AreaCurricularResponseDto;
import com.colegio.shuji.curriculo.application.dto.out.AsignacionDocenteResponseDto;
import com.colegio.shuji.curriculo.application.dto.out.BloqueHorarioResponseDto;
import com.colegio.shuji.curriculo.application.dto.out.CompetenciaResponseDto;
import com.colegio.shuji.curriculo.application.dto.out.DocenteOpcionResponseDto;
import com.colegio.shuji.curriculo.application.mapper.CurriculoMapper;
import com.colegio.shuji.curriculo.application.port.in.AsignarCargaDocenteUseCase;
import com.colegio.shuji.curriculo.application.port.in.GestionarCurriculoUseCase;
import com.colegio.shuji.curriculo.application.port.out.AsignacionRepositoryPort;
import com.colegio.shuji.curriculo.application.port.out.BloqueHorarioRepositoryPort;
import com.colegio.shuji.curriculo.application.port.out.CompetenciaRepositoryPort;
import com.colegio.shuji.curriculo.application.port.out.CurriculoRepositoryPort;
import com.colegio.shuji.curriculo.application.port.out.DocenteHabilitadoPort;
import com.colegio.shuji.curriculo.application.port.out.HorarioRepositoryPort;
import com.colegio.shuji.curriculo.domain.exception.ColisionHorarioDocenteException;
import com.colegio.shuji.curriculo.domain.model.BloqueHorario;
import com.colegio.shuji.curriculo.domain.model.HorarioSeccion;
import com.colegio.shuji.shared.application.port.out.ActorActualPort;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class CurriculoService implements GestionarCurriculoUseCase, AsignarCargaDocenteUseCase {
  private final CurriculoMapper mapper;
  private final CurriculoRepositoryPort areas;
  private final CompetenciaRepositoryPort competencias;
  private final BloqueHorarioRepositoryPort bloques;
  private final AsignacionRepositoryPort asignaciones;
  private final SeccionRepositoryPort secciones;
  private final AnioLectivoRepositoryPort anios;
  private final NivelRepositoryPort niveles;
  private final DocenteHabilitadoPort docentes;
  private final ActorActualPort actor;

  @Autowired(required = false)
  private HorarioRepositoryPort horarios;

  public void setHorarios(HorarioRepositoryPort horarios) {
    this.horarios = horarios;
  }

  private static final long TTL_CATALOGO_MS = 60_000L;

  private record EntradaCache<T>(T valor, long expiraEnMs) {
    boolean vigente() {
      return System.currentTimeMillis() < expiraEnMs;
    }
  }

  private final java.util.concurrent.ConcurrentHashMap<String, EntradaCache<?>> cache =
      new java.util.concurrent.ConcurrentHashMap<>();

  @SuppressWarnings("unchecked")
  private <T> T obtenerCache(String clave, java.util.function.Supplier<T> proveedor) {
    EntradaCache<?> e = cache.get(clave);
    if (e != null && e.vigente()) {
      return (T) e.valor();
    }
    T val = proveedor.get();
    if (val != null) {
      cache.put(clave, new EntradaCache<>(val, System.currentTimeMillis() + TTL_CATALOGO_MS));
    }
    return val;
  }

  @Transactional(readOnly = true)
  public List<AsignacionDocenteResponseDto> listarAsignaciones() {
    return listarAsignaciones(null);
  }

  @Override
  @Transactional(readOnly = true)
  public List<AsignacionDocenteResponseDto> listarAsignaciones(Short anioId) {
    var lista = (actor.tieneRol("DIRECCION") || actor.tieneRol("ADMINISTRADOR"))
        ? (anioId != null ? asignaciones.buscarPorAnioLectivoId(anioId) : asignaciones.listar())
        : asignaciones.buscarPorDocenteUsuarioId(actor.usuarioId()).stream()
            .filter(a -> anioId == null || anioId.equals(a.getAnioLectivoId()))
            .toList();
    return lista.stream().map(mapper::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public List<DocenteOpcionResponseDto> listarDocentesPorArea(Short areaId) {
    return obtenerCache("docentes:" + areaId, () -> docentes.listarPorArea(areaId));
  }

  public AreaCurricularResponseDto crearArea(CrearAreaRequestDto r) {
    requerido(niveles.buscarPorId(r.nivelId()));
    var res = mapper.toResponse(areas.guardar(mapper.toDomain(r)));
    cache.clear();
    return res;
  }

  @Transactional(readOnly = true)
  public List<AreaCurricularResponseDto> listarAreas(Short nivelId) {
    return obtenerCache("areas:" + nivelId, () -> {
      var lista = nivelId == null ? areas.listar() : areas.buscarPorNivelId(nivelId);
      return lista.stream().map(mapper::toResponse).toList();
    });
  }

  public CompetenciaResponseDto crearCompetencia(CrearCompetenciaRequestDto r) {
    requerido(areas.buscarPorId(r.areaId()));
    var res = mapper.toResponse(competencias.guardar(mapper.toDomain(r)));
    cache.clear();
    return res;
  }

  @Transactional(readOnly = true)
  public List<CompetenciaResponseDto> listarCompetencias(Short areaId) {
    return obtenerCache("competencias:" + areaId,
        () -> competencias.buscarPorAreaId(areaId).stream().map(mapper::toResponse).toList());
  }

  public BloqueHorarioResponseDto crearBloque(CrearBloqueRequestDto r) {
    horas(r.horaInicio(), r.horaFin());
    exigir(
        bloques.listar().stream()
            .noneMatch(
                b ->
                    b.getHoraInicio().isBefore(r.horaFin())
                        && b.getHoraFin().isAfter(r.horaInicio())),
        "Los bloques se superponen");
    var res = mapper.toResponse(bloques.guardar(mapper.toDomain(r)));
    cache.clear();
    return res;
  }

  @Transactional(readOnly = true)
  public List<BloqueHorarioResponseDto> listarBloques() {
    return obtenerCache("bloques", () -> bloques.listar().stream().map(mapper::toResponse).toList());
  }

  public AsignacionDocenteResponseDto asignarDocente(AsignarDocenteRequestDto r) {
    requerido(anios.bloquearPorId(r.anioLectivoId())).verificarAbierto();
    var s = requerido(secciones.buscarPorId(r.seccionId()));
    var area = requerido(areas.buscarPorId(r.areaCurricularId()));
    if (!s.getAnioLectivoId().equals(r.anioLectivoId())
        || !s.getNivelId().equals(r.nivelId())
        || !area.getNivelId().equals(r.nivelId()))
      throw new com.colegio.shuji.curriculo.domain.exception.NivelCurricularIncompatibleException(
          "Año o nivel curricular incompatible");
    exigir(docentes.listarPorArea(r.areaCurricularId()).stream()
            .anyMatch(docente -> docente.id().equals(r.docenteUsuarioId())),
        "El docente no tiene una especialidad aprobada para este curso");
    exigir(
        asignaciones.buscarPorSeccionId(r.seccionId()).stream()
            .noneMatch(
                existente ->
                    existente.getAnioLectivoId().equals(r.anioLectivoId())
                        && existente.getAreaCurricularId().equals(r.areaCurricularId())),
        "Esta sección ya tiene una asignación para el curso seleccionado");
    var res = mapper.toResponse(asignaciones.guardar(mapper.toDomain(r)));
    cache.clear();
    return res;
  }

  @Override
  public AsignacionDocenteResponseDto reasignarDocente(
      Long asignacionId, Long nuevoDocenteUsuarioId) {
    var asignacion = requerido(asignaciones.bloquearPorId(asignacionId));
    requerido(anios.bloquearPorId(asignacion.getAnioLectivoId())).verificarAbierto();
    if (Objects.equals(asignacion.getDocenteUsuarioId(), nuevoDocenteUsuarioId)) {
      return mapper.toResponse(asignacion);
    }
    exigir(
        docentes.listarPorArea(asignacion.getAreaCurricularId()).stream()
            .anyMatch(docente -> docente.id().equals(nuevoDocenteUsuarioId)),
        "El docente no tiene una especialidad aprobada para este curso");

    if (horarios != null) {
      List<HorarioSeccion> horariosAnio =
          horarios.buscarPorAnioLectivoId(asignacion.getAnioLectivoId());
      List<HorarioSeccion> horariosAsignacion =
          horariosAnio.stream()
              .filter(h -> h.estaActivo() && Objects.equals(h.getAsignacionDocenteId(), asignacionId))
              .toList();
      if (!horariosAsignacion.isEmpty()) {
        Map<Short, BloqueHorario> bloquesMap =
            bloques.listar().stream()
                .collect(Collectors.toMap(BloqueHorario::getId, Function.identity(), (a, b) -> a));
        List<HorarioSeccion> horariosNuevoDocente =
            horariosAnio.stream()
                .filter(
                    h ->
                        h.estaActivo()
                            && Objects.equals(h.getDocenteUsuarioId(), nuevoDocenteUsuarioId)
                            && !Objects.equals(h.getAsignacionDocenteId(), asignacionId))
                .toList();
        for (HorarioSeccion actual : horariosAsignacion) {
          BloqueHorario bActual = bloquesMap.get(actual.getBloqueHorarioId());
          if (bActual == null) continue;
          for (HorarioSeccion otro : horariosNuevoDocente) {
            if (!Objects.equals(actual.getDiaSemana(), otro.getDiaSemana())) continue;
            BloqueHorario bOtro = bloquesMap.get(otro.getBloqueHorarioId());
            if (bOtro == null) continue;
            if (bOtro.getHoraInicio().isBefore(bActual.getHoraFin())
                && bOtro.getHoraFin().isAfter(bActual.getHoraInicio())) {
              throw new ColisionHorarioDocenteException(
                  "El docente seleccionado ya tiene otra clase programada en uno de los bloques"
                      + " horarios de este curso.");
            }
          }
        }
      }
    }

    asignacion.reasignarDocente(nuevoDocenteUsuarioId);
    var res = mapper.toResponse(asignaciones.guardar(asignacion));
    cache.clear();
    return res;
  }
}
