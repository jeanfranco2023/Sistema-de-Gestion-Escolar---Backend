package com.colegio.shuji.academico.application.service;

import static com.colegio.shuji.shared.domain.model.Reglas.exigir;
import static com.colegio.shuji.shared.domain.model.Reglas.fechas;
import static com.colegio.shuji.shared.domain.model.Reglas.requerido;

import com.colegio.shuji.academico.application.dto.in.CrearAnioLectivoRequestDto;
import com.colegio.shuji.academico.application.dto.in.CrearGradoRequestDto;
import com.colegio.shuji.academico.application.dto.in.CrearPeriodoRequestDto;
import com.colegio.shuji.academico.application.dto.in.CrearSeccionRequestDto;
import com.colegio.shuji.academico.application.dto.out.AnioLectivoResponseDto;
import com.colegio.shuji.academico.application.dto.out.GradoResponseDto;
import com.colegio.shuji.academico.application.dto.out.NivelResponseDto;
import com.colegio.shuji.academico.application.dto.out.PeriodoResponseDto;
import com.colegio.shuji.academico.application.dto.out.SeccionResponseDto;
import com.colegio.shuji.academico.application.dto.out.VacantesSeccionResponseDto;
import com.colegio.shuji.academico.application.mapper.AcademicoMapper;
import com.colegio.shuji.academico.application.port.in.ConsultarVacantesUseCase;
import com.colegio.shuji.academico.application.port.in.GestionarAnioLectivoUseCase;
import com.colegio.shuji.academico.application.port.in.GestionarSeccionesUseCase;
import com.colegio.shuji.academico.application.port.out.AnioLectivoRepositoryPort;
import com.colegio.shuji.academico.application.port.out.GradoRepositoryPort;
import com.colegio.shuji.academico.application.port.out.NivelRepositoryPort;
import com.colegio.shuji.academico.application.port.out.PeriodoRepositoryPort;
import com.colegio.shuji.academico.application.port.out.SeccionRepositoryPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AcademicoService
    implements GestionarAnioLectivoUseCase, GestionarSeccionesUseCase, ConsultarVacantesUseCase {
  private final AcademicoMapper mapper;
  private final AnioLectivoRepositoryPort anios;
  private final PeriodoRepositoryPort periodos;
  private final SeccionRepositoryPort secciones;
  private final NivelRepositoryPort niveles;
  private final GradoRepositoryPort grados;

  public AnioLectivoResponseDto crearAnio(CrearAnioLectivoRequestDto r) {
    fechas(r.fechaInicio(), r.fechaFin());
    var model = mapper.toDomain(r);
    model.abrir();
    return mapper.toResponse(anios.guardar(model));
  }

  @Transactional(readOnly = true)
  public List<AnioLectivoResponseDto> listarAnios() {
    return anios.listar().stream().map(mapper::toResponse).toList();
  }

  public void cerrarAnio(Short id) {
    var anio = requerido(anios.bloquearPorId(id));
    anio.cerrar();
    anios.guardar(anio);
  }

  public PeriodoResponseDto crearPeriodo(CrearPeriodoRequestDto r) {
    var anio = requerido(anios.bloquearPorId(r.anioLectivoId()));
    anio.verificarAbierto();
    fechas(r.fechaInicio(), r.fechaFin());
    exigir(
        !r.fechaInicio().isBefore(anio.getFechaInicio())
            && !r.fechaFin().isAfter(anio.getFechaFin()),
        "Período fuera del año lectivo");
    exigir(
        periodos.buscarPorAnioLectivoId(r.anioLectivoId()).stream()
            .noneMatch(
                p ->
                    !p.getFechaFin().isBefore(r.fechaInicio())
                        && !p.getFechaInicio().isAfter(r.fechaFin())),
        "Los períodos no pueden superponerse");
    var model = mapper.toDomain(r);
    model.abrir();
    return mapper.toResponse(periodos.guardar(model));
  }

  @Transactional(readOnly = true)
  public List<PeriodoResponseDto> listarPeriodos(Short anioId) {
    return periodos.buscarPorAnioLectivoId(anioId).stream().map(mapper::toResponse).toList();
  }

  public void cerrarPeriodo(Short id) {
    var p = requerido(periodos.bloquearPorId(id));
    p.cerrar();
    periodos.guardar(p);
  }

  public SeccionResponseDto crearSeccion(CrearSeccionRequestDto r) {
    requerido(anios.bloquearPorId(r.anioLectivoId())).verificarAbierto();
    var grado = requerido(grados.buscarPorId(r.gradoId()));
    exigir(grado.getNivelId().equals(r.nivelId()), "El grado no pertenece al nivel");
    if (r.aulaFisica() != null
        && !r.aulaFisica().isBlank()
        && secciones.buscarPorAnioLectivoId(r.anioLectivoId()).stream()
            .anyMatch(s -> r.aulaFisica().equals(s.getAulaFisica())))
      throw new com.colegio.shuji.academico.domain.exception.AulaOcupadaException(
          "El aula ya está asignada");
    var model = mapper.toDomain(r);
    model.normalizarAula();
    return mapper.toResponse(secciones.guardar(model));
  }

  @Transactional(readOnly = true)
  public List<SeccionResponseDto> listarSecciones(Short anioId, Short nivelId) {
    return secciones.buscarPorAnioLectivoId(anioId).stream()
        .filter(s -> nivelId == null || s.getNivelId().equals(nivelId))
        .map(mapper::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public VacantesSeccionResponseDto vacantes(Integer id) {
    var s = requerido(secciones.buscarPorId(id));
    return new VacantesSeccionResponseDto(
        s.getId(), s.getCupoMaximo(), s.getVacantesOcupadas(), s.vacantesDisponibles());
  }

  @Transactional(readOnly = true)
  public List<NivelResponseDto> listarNiveles() {
    return niveles.listar().stream().map(mapper::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public List<GradoResponseDto> listarGrados(Short nivelId) {
    return grados.buscarPorNivelId(nivelId).stream().map(mapper::toResponse).toList();
  }

  public GradoResponseDto crearGrado(CrearGradoRequestDto r) {
    requerido(niveles.buscarPorId(r.nivelId()));
    return mapper.toResponse(grados.guardar(mapper.toDomain(r)));
  }
}
