package com.colegio.shuji.curriculo.infrastructure.adapter;

import com.colegio.shuji.curriculo.application.port.out.CompetenciaRepositoryPort;
import com.colegio.shuji.curriculo.domain.model.Competencia;
import com.colegio.shuji.curriculo.infrastructure.repository.JpaCompetenciaRepository;
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
public class CompetenciaRepositoryAdapter implements CompetenciaRepositoryPort {
  private final JpaCompetenciaRepository repository;
  private final CompetenciaPersistenceMapper mapper;
  private final EntityManager entityManager;
  private final com.colegio.shuji.config.persistence.SupabaseAuditInterceptor auditoria;

  public Competencia guardar(Competencia valor) {
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

  public Optional<Competencia> buscarPorId(Short id) {
    return repository.findById(id).map(mapper::toDomain);
  }

  public Optional<Competencia> bloquearPorId(Short id) {
    return repository
        .bloquearPorId(id)
        .map(
            e -> {
              entityManager.refresh(e);
              return mapper.toDomain(e);
            });
  }

  public List<Competencia> listar() {
    return repository.findAll(org.springframework.data.domain.Sort.by("id")).stream()
        .map(mapper::toDomain)
        .toList();
  }

  public List<Competencia> buscarPorAreaId(Short valor) {
    return repository.buscarPorAreaId(valor).stream().map(mapper::toDomain).toList();
  }

  public List<Competencia> buscarPorIds(Collection<Short> ids) {
    if (ids == null || ids.isEmpty()) return List.of();
    return repository.buscarPorIds(ids).stream().map(mapper::toDomain).toList();
  }
}
