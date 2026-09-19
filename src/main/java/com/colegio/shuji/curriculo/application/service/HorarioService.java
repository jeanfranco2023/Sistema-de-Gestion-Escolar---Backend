package com.colegio.shuji.curriculo.application.service;

import static com.colegio.shuji.shared.domain.model.Reglas.exigir;
import static com.colegio.shuji.shared.domain.model.Reglas.requerido;

import com.colegio.shuji.academico.application.port.out.AnioLectivoRepositoryPort;
import com.colegio.shuji.curriculo.application.dto.in.ProgramarHorarioRequestDto;
import com.colegio.shuji.curriculo.application.dto.out.HorarioSeccionResponseDto;
import com.colegio.shuji.curriculo.application.dto.out.MallaHorariaResponseDto;
import com.colegio.shuji.curriculo.application.mapper.CurriculoMapper;
import com.colegio.shuji.curriculo.application.port.in.GenerarMallaHorariaUseCase;
import com.colegio.shuji.curriculo.application.port.out.AsignacionRepositoryPort;
import com.colegio.shuji.curriculo.application.port.out.BloqueHorarioRepositoryPort;
import com.colegio.shuji.curriculo.application.port.out.HorarioRepositoryPort;
import com.colegio.shuji.curriculo.domain.model.BloqueHorario;
import com.colegio.shuji.curriculo.domain.model.HorarioSeccion;
import com.colegio.shuji.shared.application.port.out.ActorActualPort;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class HorarioService implements GenerarMallaHorariaUseCase {
  private final CurriculoMapper mapper;
  private final HorarioRepositoryPort horarios;
  private final AsignacionRepositoryPort asignaciones;
  private final BloqueHorarioRepositoryPort bloques;
  private final AnioLectivoRepositoryPort anios;
  private final ActorActualPort actor;

  public HorarioSeccionResponseDto programar(ProgramarHorarioRequestDto r) {
    var a = requerido(asignaciones.buscarPorId(r.asignacionDocenteId()));
    requerido(anios.bloquearPorId(a.getAnioLectivoId())).verificarAbierto();
    var b = requerido(bloques.buscarPorId(r.bloqueHorarioId()));
    exigir(!Boolean.TRUE.equals(b.getEsRecreo()), "No se asignan clases en recreo");
    var bloquesMap =
        bloques.listar().stream()
            .collect(Collectors.toMap(BloqueHorario::getId, Function.identity()));
    for (var h : horarios.buscarPorAnioLectivoId(a.getAnioLectivoId())) {
      if (h.getDiaSemana() != r.diaSemana().getCodigo()) continue;
      var otro = requerido(Optional.ofNullable(bloquesMap.get(h.getBloqueHorarioId())));
      if (!otro.getHoraInicio().isBefore(b.getHoraFin())
          || !otro.getHoraFin().isAfter(b.getHoraInicio())) continue;
      if (h.getDocenteUsuarioId().equals(a.getDocenteUsuarioId()))
        throw new com.colegio.shuji.curriculo.domain.exception.ColisionHorarioDocenteException(
            "Docente ocupado en ese horario");
      if (h.getSeccionId().equals(a.getSeccionId()))
        throw new com.colegio.shuji.curriculo.domain.exception.ColisionHorarioAulaException(
            "Sección ocupada en ese horario");
    }
    var h =
        HorarioSeccion.builder()
            .anioLectivoId(a.getAnioLectivoId())
            .seccionId(a.getSeccionId())
            .docenteUsuarioId(a.getDocenteUsuarioId())
            .asignacionDocenteId(a.getId())
            .build();
    h.reprogramar(r.diaSemana(), b.getId());
    return mapper.toResponse(horarios.guardar(h));
  }

  @Transactional(readOnly = true)
  public MallaHorariaResponseDto porSeccion(Integer id, Short anioId) {
    return new MallaHorariaResponseDto(
        horarios.buscarPorSeccionId(id).stream()
            .filter(h -> h.getAnioLectivoId().equals(anioId))
            .map(mapper::toResponse)
            .toList());
  }

  @Transactional(readOnly = true)
  public MallaHorariaResponseDto porDocente(Long id, Short anioId) {
    actor.verificarDocente(id);
    return new MallaHorariaResponseDto(
        horarios.buscarPorDocenteUsuarioId(id).stream()
            .filter(h -> h.getAnioLectivoId().equals(anioId))
            .map(mapper::toResponse)
            .toList());
  }
}
