package com.colegio.shuji.evaluacion.infrastructure.adapter;

import com.colegio.shuji.evaluacion.application.port.out.CalificacionRepositoryPort;
import com.colegio.shuji.evaluacion.domain.model.CalificacionCneb;
import com.colegio.shuji.evaluacion.infrastructure.repository.JpaCalificacionRepository;
import jakarta.persistence.EntityManager;
import java.util.*;
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
}
