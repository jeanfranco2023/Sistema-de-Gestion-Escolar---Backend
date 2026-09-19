package com.colegio.shuji.matricula.application.service;

import static com.colegio.shuji.shared.domain.model.Reglas.exigir;
import static com.colegio.shuji.shared.domain.model.Reglas.requerido;

import com.colegio.shuji.academico.application.port.out.AnioLectivoRepositoryPort;
import com.colegio.shuji.academico.application.port.out.SeccionRepositoryPort;
import com.colegio.shuji.matricula.application.dto.in.ConfirmarMatriculaRequestDto;
import com.colegio.shuji.matricula.application.dto.in.SolicitarMatriculaRequestDto;
import com.colegio.shuji.matricula.application.dto.out.FichaMatriculaResponseDto;
import com.colegio.shuji.matricula.application.dto.out.MatriculaResponseDto;
import com.colegio.shuji.matricula.application.mapper.MatriculaMapper;
import com.colegio.shuji.matricula.application.port.in.LiberarReservasVencidasUseCase;
import com.colegio.shuji.matricula.application.port.in.ProcesarMatriculaUseCase;
import com.colegio.shuji.matricula.application.port.out.ApoderadoRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.EstudianteApoderadoRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.EstudianteRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.MatriculaRepositoryPort;
import com.colegio.shuji.matricula.domain.enums.EstadoMatricula;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class MatriculaService implements ProcesarMatriculaUseCase, LiberarReservasVencidasUseCase {
  private final MatriculaMapper mapper;
  private final MatriculaRepositoryPort matriculas;
  private final EstudianteRepositoryPort estudiantes;
  private final ApoderadoRepositoryPort apoderados;
  private final EstudianteApoderadoRepositoryPort vinculos;
  private final SeccionRepositoryPort secciones;
  private final AnioLectivoRepositoryPort anios;

  public MatriculaResponseDto solicitar(SolicitarMatriculaRequestDto r) {
    requerido(anios.bloquearPorId(r.anioLectivoId())).verificarAbierto();
    var s = requerido(secciones.bloquearPorId(r.seccionId()));
    exigir(s.getAnioLectivoId().equals(r.anioLectivoId()), "Sección de otro año");
    var e = requerido(estudiantes.bloquearPorId(r.estudianteId()));
    exigir(Boolean.TRUE.equals(e.getActivo()), "Estudiante inactivo");
    if (matriculas.buscarPorEstudianteId(r.estudianteId()).stream()
        .anyMatch(m -> m.getAnioLectivoId().equals(r.anioLectivoId())))
      throw new com.colegio.shuji.matricula.domain.exception.EstudianteYaMatriculadoException(
          "Ya existe matrícula para este año");
    var m = mapper.toDomain(r);
    m.iniciarSolicitud();
    if (r.reservaExpiraAt() != null) {
      s.verificarCupo();
      m.reservar(r.reservaExpiraAt());
    }
    return mapper.toResponse(matriculas.guardar(m));
  }

  public MatriculaResponseDto confirmar(ConfirmarMatriculaRequestDto r) {
    var snapshot = requerido(matriculas.buscarPorId(r.matriculaId()));
    requerido(anios.bloquearPorId(snapshot.getAnioLectivoId())).verificarAbierto();
    var s = requerido(secciones.bloquearPorId(snapshot.getSeccionId()));
    var m = requerido(matriculas.bloquearPorId(r.matriculaId()));
    if (m.getEstadoMatricula() == EstadoMatricula.SOLICITADA) s.verificarCupo();
    exigir(
        vinculos.buscarPorEstudianteId(m.getEstudianteId()).stream()
            .anyMatch(v -> Boolean.TRUE.equals(v.getEsResponsableEconomico())),
        "Se requiere responsable económico");
    m.confirmar(OffsetDateTime.now());
    return mapper.toResponse(matriculas.guardar(m));
  }

  public int liberarVencidas() {
    int total = 0;
    var ahora = OffsetDateTime.now();
    for (var m : matriculas.buscarPorEstadoMatricula(EstadoMatricula.RESERVADA_TEMPORAL)) {
      if (m.getReservaExpiraAt().isAfter(ahora)) continue;
      requerido(secciones.bloquearPorId(m.getSeccionId()));
      var actual = requerido(matriculas.bloquearPorId(m.getId()));
      if (actual.liberarSiVencida(ahora)) {
        matriculas.guardar(actual);
        total++;
      }
    }
    return total;
  }

  @Transactional(readOnly = true)
  public FichaMatriculaResponseDto ficha(Long id) {
    var m = requerido(matriculas.buscarPorId(id));
    return new FichaMatriculaResponseDto(
        mapper.toResponse(m),
        mapper.toResponse(requerido(estudiantes.buscarPorId(m.getEstudianteId()))),
        vinculos.buscarPorEstudianteId(m.getEstudianteId()).stream()
            .map(v -> mapper.toResponse(requerido(apoderados.buscarPorId(v.getApoderadoId()))))
            .toList());
  }
}
