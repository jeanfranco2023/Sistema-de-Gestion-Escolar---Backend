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
import com.colegio.shuji.curriculo.application.mapper.CurriculoMapper;
import com.colegio.shuji.curriculo.application.port.in.AsignarCargaDocenteUseCase;
import com.colegio.shuji.curriculo.application.port.in.GestionarCurriculoUseCase;
import com.colegio.shuji.curriculo.application.port.out.AsignacionRepositoryPort;
import com.colegio.shuji.curriculo.application.port.out.BloqueHorarioRepositoryPort;
import com.colegio.shuji.curriculo.application.port.out.CompetenciaRepositoryPort;
import com.colegio.shuji.curriculo.application.port.out.CurriculoRepositoryPort;
import com.colegio.shuji.curriculo.application.port.out.DocenteHabilitadoPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
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

  public AreaCurricularResponseDto crearArea(CrearAreaRequestDto r) {
    requerido(niveles.buscarPorId(r.nivelId()));
    return mapper.toResponse(areas.guardar(mapper.toDomain(r)));
  }

  @Transactional(readOnly = true)
  public List<AreaCurricularResponseDto> listarAreas(Short nivelId) {
    return areas.buscarPorNivelId(nivelId).stream().map(mapper::toResponse).toList();
  }

  public CompetenciaResponseDto crearCompetencia(CrearCompetenciaRequestDto r) {
    requerido(areas.buscarPorId(r.areaId()));
    return mapper.toResponse(competencias.guardar(mapper.toDomain(r)));
  }

  @Transactional(readOnly = true)
  public List<CompetenciaResponseDto> listarCompetencias(Short areaId) {
    return competencias.buscarPorAreaId(areaId).stream().map(mapper::toResponse).toList();
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
    return mapper.toResponse(bloques.guardar(mapper.toDomain(r)));
  }

  @Transactional(readOnly = true)
  public List<BloqueHorarioResponseDto> listarBloques() {
    return bloques.listar().stream().map(mapper::toResponse).toList();
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
    exigir(docentes.estaHabilitado(r.docenteUsuarioId()), "Usuario sin rol docente activo");
    return mapper.toResponse(asignaciones.guardar(mapper.toDomain(r)));
  }
}
