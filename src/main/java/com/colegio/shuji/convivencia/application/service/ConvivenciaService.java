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
import com.colegio.shuji.matricula.domain.enums.EstadoMatricula;
import com.colegio.shuji.shared.application.port.out.ActorActualPort;
import java.util.List;
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
  private final ActorActualPort actor;

  public IncidenciaResponseDto registrar(RegistrarIncidenciaRequestDto r) {
    var m = requerido(matriculas.buscarPorId(r.matriculaId()));
    exigir(m.getEstadoMatricula() == EstadoMatricula.MATRICULADO, "Matrícula no activa");
    var i = mapper.toDomain(r);
    i.abrir(actor.usuarioId());
    return mapper.toResponse(incidencias.guardar(i));
  }

  public IncidenciaResponseDto actualizar(Long id, ActualizarEstadoIncidenciaRequestDto r) {
    var i =
        incidencias
            .bloquearPorId(id)
            .orElseThrow(
                () ->
                    new com.colegio.shuji.convivencia.domain.exception
                        .IncidenciaNoEncontradaException("Incidencia no encontrada"));
    i.cambiarEstado(r.estado());
    return mapper.toResponse(incidencias.guardar(i));
  }

  @Transactional(readOnly = true)
  public HistorialConductualEstudianteResponseDto historial(Long matriculaId) {
    return new HistorialConductualEstudianteResponseDto(
        matriculaId,
        incidencias.buscarPorMatriculaId(matriculaId).stream().map(mapper::toResponse).toList());
  }

  @Transactional(readOnly = true)
  public List<IncidenciaResponseDto> citaciones() {
    return incidencias.buscarPorEstado(EstadoIncidencia.ABIERTA).stream()
        .filter(
            i -> Boolean.TRUE.equals(i.getRequiereCitacion()) && i.getTipoFalta() != TipoFalta.LEVE)
        .map(mapper::toResponse)
        .toList();
  }
}
