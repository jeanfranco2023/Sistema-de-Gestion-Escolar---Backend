package com.colegio.shuji.evaluacion.infrastructure.adapter;

import com.colegio.shuji.evaluacion.application.port.out.CalificacionRepositoryPort;
import com.colegio.shuji.evaluacion.domain.model.CalificacionCneb;
import com.colegio.shuji.evaluacion.infrastructure.repository.JpaCalificacionRepository;
import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class CalificacionRepositoryAdapter implements CalificacionRepositoryPort {
  private final JpaCalificacionRepository repository;
  private final CalificacionCnebPersistenceMapper mapper;
  private final EntityManager entityManager;
  private final com.colegio.shuji.config.persistence.SupabaseAuditInterceptor auditoria;

  public CalificacionCneb guardar(CalificacionCneb valor) {
    auditoria.syncCurrentUserFromSecurityContext();
    var entity =
        valor.getId() == null
            ? mapper.toEntity(valor)
            : repository
                .findById(valor.getId())
                .orElseThrow(
                    () ->
                        new com.colegio.shuji.shared.domain.exception.BusinessException(
                            "Registro no encontrado"));
    mapper.actualizar(valor, entity);
    repository.saveAndFlush(entity);
    entityManager.refresh(entity);
    return mapper.toDomain(entity);
  }

  public Optional<CalificacionCneb> buscarPorId(Long id) {
    return repository.findById(id).map(mapper::toDomain);
  }

  @Override
  public List<CalificacionCneb> buscarPorIds(Collection<Long> ids) {
    if (ids == null || ids.isEmpty()) return List.of();
    return repository.findAllById(ids).stream().map(mapper::toDomain).toList();
  }

  public Optional<CalificacionCneb> bloquearPorId(Long id) {
    return repository
        .bloquearPorId(id)
        .map(
            e -> {
              entityManager.refresh(e);
              return mapper.toDomain(e);
            });
  }

  public List<CalificacionCneb> listar() {
    return repository.findAll(org.springframework.data.domain.Sort.by("id")).stream()
        .map(mapper::toDomain)
        .toList();
  }

  public List<CalificacionCneb> buscarPorMatriculaId(Long valor) {
    return repository.buscarPorMatriculaId(valor).stream().map(mapper::toDomain).toList();
  }

  public List<CalificacionCneb> buscarPorMatriculaIds(Collection<Long> matriculaIds) {
    if (matriculaIds == null || matriculaIds.isEmpty()) return List.of();
    return repository.buscarPorMatriculaIds(matriculaIds).stream().map(mapper::toDomain).toList();
  }

  public List<CalificacionCneb> buscarPorAnioLectivoId(Short valor) {
    return repository.buscarPorAnioLectivoId(valor).stream().map(mapper::toDomain).toList();
  }

  public List<CalificacionCneb> buscarPorSeccionId(Integer valor) {
    return repository.buscarPorSeccionId(valor).stream().map(mapper::toDomain).toList();
  }

  public List<CalificacionCneb> buscarPorPeriodoAcademicoId(Short valor) {
    return repository.buscarPorPeriodoAcademicoId(valor).stream().map(mapper::toDomain).toList();
  }

  public List<CalificacionCneb> buscarPorAsignacionDocenteId(Long valor) {
    return repository.buscarPorAsignacionDocenteId(valor).stream().map(mapper::toDomain).toList();
  }

  public List<CalificacionCneb> buscarPorAreaCurricularId(Short valor) {
    return repository.buscarPorAreaCurricularId(valor).stream().map(mapper::toDomain).toList();
  }

  public List<CalificacionCneb> buscarPorDocenteUsuarioId(Long valor) {
    return repository.buscarPorDocenteUsuarioId(valor).stream().map(mapper::toDomain).toList();
  }

  public List<CalificacionCneb> buscarPorCompetenciaId(Short valor) {
    return repository.buscarPorCompetenciaId(valor).stream().map(mapper::toDomain).toList();
  }

  public List<CalificacionCneb> buscarPorAsignacionYPeriodo(Long asigId, Short periodoId) {
    return repository.buscarPorAsignacionYPeriodo(asigId, periodoId).stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<CalificacionCneb> buscarPorAnioYPeriodo(Short anioId, Short periodoId) {
    return repository.buscarPorAnioYPeriodo(anioId, periodoId).stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<CalificacionCneb> buscarRiesgosPorAnio(Short anioId) {
    return repository.buscarRiesgosPorAnio(anioId).stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<CalificacionCneb> buscarRiesgosPorAnioYPeriodo(Short anioId, Short periodoId) {
    return repository.buscarRiesgosPorAnioYPeriodo(anioId, periodoId).stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<CalificacionCneb> buscarRiesgosPorPeriodo(Short periodoId) {
    return repository.buscarRiesgosPorPeriodo(periodoId).stream().map(mapper::toDomain).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<List<ResumenNotaDashboard>> listarResumenDashboard(Short anioId, Short periodoId) {
    if (anioId == null) return Optional.of(List.of());
    return Optional.of(
        periodoId != null
            ? repository.listarResumenPorAnioYPeriodo(anioId, periodoId)
            : repository.listarResumenPorAnio(anioId));
  }
}
