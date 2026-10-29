package com.colegio.shuji.curriculo.application.service;

import static com.colegio.shuji.shared.domain.model.Reglas.exigir;
import static com.colegio.shuji.shared.domain.model.Reglas.requerido;

import com.colegio.shuji.academico.application.port.out.AnioLectivoRepositoryPort;
import com.colegio.shuji.academico.application.port.out.GradoRepositoryPort;
import com.colegio.shuji.curriculo.application.dto.in.ProgramarHorarioRequestDto;
import com.colegio.shuji.curriculo.application.dto.in.ProgramarMallaHorariaGradoRequestDto;
import com.colegio.shuji.curriculo.application.dto.out.HorarioSeccionResponseDto;
import com.colegio.shuji.curriculo.application.dto.out.MallaHorariaResponseDto;
import com.colegio.shuji.curriculo.application.dto.out.MallaHorariaGradoResponseDto;
import com.colegio.shuji.curriculo.application.mapper.CurriculoMapper;
import com.colegio.shuji.curriculo.application.port.in.GenerarMallaHorariaUseCase;
import com.colegio.shuji.curriculo.application.port.out.AsignacionRepositoryPort;
import com.colegio.shuji.curriculo.application.port.out.BloqueHorarioRepositoryPort;
import com.colegio.shuji.curriculo.application.port.out.HorarioRepositoryPort;
import com.colegio.shuji.curriculo.application.port.out.MallaHorariaGradoRepositoryPort;
import com.colegio.shuji.curriculo.application.port.out.CurriculoRepositoryPort;
import com.colegio.shuji.curriculo.application.port.in.ConsultarCatalogoHorariosUseCase;
import com.colegio.shuji.curriculo.domain.model.BloqueHorario;
import com.colegio.shuji.curriculo.domain.model.HorarioSeccion;
import com.colegio.shuji.curriculo.domain.model.MallaHorariaGrado;
import com.colegio.shuji.shared.application.port.out.ActorActualPort;
import java.util.Optional;
import java.util.List;
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
  private final MallaHorariaGradoRepositoryPort mallasGrado;
  private final ConsultarCatalogoHorariosUseCase catalogo;
  private final GradoRepositoryPort grados;
  private final CurriculoRepositoryPort areas;

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
            .activo(true)
            .build();
    h.reprogramar(r.diaSemana(), b.getId());
    return mapper.toResponse(horarios.guardar(h));
  }

  public HorarioSeccionResponseDto actualizar(Long id, ProgramarHorarioRequestDto r) {
    var actual = requerido(horarios.bloquearPorId(id));
    exigir(actual.estaActivo(), "El horario ya fue retirado");
    var asignacion = requerido(asignaciones.buscarPorId(r.asignacionDocenteId()));
    exigir(
        actual.getAnioLectivoId().equals(asignacion.getAnioLectivoId()),
        "No se puede mover un horario a otro año lectivo");
    requerido(anios.bloquearPorId(asignacion.getAnioLectivoId())).verificarAbierto();
    var bloque = requerido(bloques.buscarPorId(r.bloqueHorarioId()));
    exigir(!Boolean.TRUE.equals(bloque.getEsRecreo()), "No se asignan clases en recreo");
    var bloquesMap =
        bloques.listar().stream()
            .collect(Collectors.toMap(BloqueHorario::getId, Function.identity()));
    for (var horario : horarios.buscarPorAnioLectivoId(asignacion.getAnioLectivoId())) {
      if (horario.getId().equals(id) || horario.getDiaSemana() != r.diaSemana().getCodigo()) continue;
      var otro = requerido(Optional.ofNullable(bloquesMap.get(horario.getBloqueHorarioId())));
      if (!otro.getHoraInicio().isBefore(bloque.getHoraFin())
          || !otro.getHoraFin().isAfter(bloque.getHoraInicio())) continue;
      if (horario.getDocenteUsuarioId().equals(asignacion.getDocenteUsuarioId()))
        throw new com.colegio.shuji.curriculo.domain.exception.ColisionHorarioDocenteException(
            "Docente ocupado en ese horario");
      if (horario.getSeccionId().equals(asignacion.getSeccionId()))
        throw new com.colegio.shuji.curriculo.domain.exception.ColisionHorarioAulaException(
            "Sección ocupada en ese horario");
    }
    actual.reprogramar(asignacion, r.diaSemana(), bloque.getId());
    return mapper.toResponse(horarios.guardar(actual));
  }

  public HorarioSeccionResponseDto retirar(Long id) {
    var horario = requerido(horarios.bloquearPorId(id));
    exigir(horario.estaActivo(), "El horario ya fue retirado");
    requerido(anios.bloquearPorId(horario.getAnioLectivoId())).verificarAbierto();
    horario.retirar();
    return mapper.toResponse(horarios.guardar(horario));
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

  public MallaHorariaGradoResponseDto programarGrado(ProgramarMallaHorariaGradoRequestDto r) {
    validarAsignacionDeGrado(r);
    requerido(anios.bloquearPorId(r.anioLectivoId())).verificarAbierto();
    var bloque = validarBloque(r.bloqueHorarioId());
    verificarColisionGrado(r.gradoId(), r.anioLectivoId(), r.diaSemana(), bloque, null);
    var malla = MallaHorariaGrado.builder().anioLectivoId(r.anioLectivoId())
        .gradoId(r.gradoId()).areaCurricularId(r.areaCurricularId()).activo(true).build();
    malla.programar(r.diaSemana(), bloque.getId());
    return respuestaGrado(mallasGrado.guardar(malla));
  }

  public MallaHorariaGradoResponseDto actualizarGrado(Long id, ProgramarMallaHorariaGradoRequestDto r) {
    var actual = requerido(mallasGrado.bloquearPorId(id));
    exigir(actual.estaActivo(), "El bloque de grado ya fue retirado");
    exigir(actual.getAnioLectivoId().equals(r.anioLectivoId()) && actual.getGradoId().equals(r.gradoId()),
        "El bloque no se puede trasladar a otro grado o año lectivo");
    validarAsignacionDeGrado(r);
    requerido(anios.bloquearPorId(r.anioLectivoId())).verificarAbierto();
    var bloque = validarBloque(r.bloqueHorarioId());
    verificarColisionGrado(r.gradoId(), r.anioLectivoId(), r.diaSemana(), bloque, id);
    actual.cambiarArea(r.areaCurricularId());
    actual.programar(r.diaSemana(), bloque.getId());
    return respuestaGrado(mallasGrado.guardar(actual));
  }

  public MallaHorariaGradoResponseDto retirarGrado(Long id) {
    var malla = requerido(mallasGrado.bloquearPorId(id));
    exigir(malla.estaActivo(), "El bloque de grado ya fue retirado");
    requerido(anios.bloquearPorId(malla.getAnioLectivoId())).verificarAbierto();
    malla.retirar();
    return respuestaGrado(mallasGrado.guardar(malla));
  }

  @Transactional(readOnly = true)
  public List<MallaHorariaGradoResponseDto> porGrado(Short gradoId, Short anioId) {
    if (actor.tieneRol("DOCENTE")) {
      exigir(catalogo.catalogo().stream().anyMatch(a -> a.gradoId().equals(gradoId)
          && a.anioLectivoId().equals(anioId)), "El grado no pertenece a una sección asignada");
    }
    return mallasGrado.buscarPorGradoYAnio(gradoId, anioId).stream().map(this::respuestaGrado).toList();
  }

  private void validarAsignacionDeGrado(ProgramarMallaHorariaGradoRequestDto r) {
    var grado = requerido(grados.buscarPorId(r.gradoId()));
    var area = requerido(areas.buscarPorId(r.areaCurricularId()));
    exigir(grado.getNivelId().equals(area.getNivelId()),
        "El curso seleccionado no pertenece al nivel educativo del grado");
  }

  private BloqueHorario validarBloque(Short bloqueId) {
    var bloque = requerido(bloques.buscarPorId(bloqueId));
    exigir(!Boolean.TRUE.equals(bloque.getEsRecreo()), "No se asignan clases en recreo");
    return bloque;
  }

  private void verificarColisionGrado(Short gradoId, Short anioId,
      com.colegio.shuji.curriculo.domain.enums.DiaSemana dia, BloqueHorario bloque, Long excluirId) {
    var bloquesPorId = bloques.listar().stream().collect(Collectors.toMap(BloqueHorario::getId, Function.identity()));
    for (var existente : mallasGrado.buscarPorGradoYAnio(gradoId, anioId)) {
      if (existente.getDiaSemana() != dia.getCodigo() || existente.getId().equals(excluirId)) continue;
      var otro = requerido(Optional.ofNullable(bloquesPorId.get(existente.getBloqueHorarioId())));
      if (otro.getHoraInicio().isBefore(bloque.getHoraFin()) && otro.getHoraFin().isAfter(bloque.getHoraInicio()))
        throw new com.colegio.shuji.curriculo.domain.exception.ColisionHorarioAulaException(
            "El grado ya tiene una clase en ese intervalo horario");
    }
  }

  private MallaHorariaGradoResponseDto respuestaGrado(MallaHorariaGrado m) {
    return new MallaHorariaGradoResponseDto(m.getId(), m.getAnioLectivoId(), m.getGradoId(),
        m.getAreaCurricularId(), m.getDiaSemana(), m.getBloqueHorarioId(), m.getActivo());
  }
}
