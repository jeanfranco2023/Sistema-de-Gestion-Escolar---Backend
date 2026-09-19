package com.colegio.shuji.tesoreria.infrastructure.adapter;

import com.colegio.shuji.tesoreria.application.port.out.ObligacionRepositoryPort;
import com.colegio.shuji.tesoreria.domain.enums.EstadoObligacion;
import com.colegio.shuji.tesoreria.domain.model.ObligacionPago;
import com.colegio.shuji.tesoreria.infrastructure.repository.JpaObligacionRepository;
import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class ObligacionRepositoryAdapter implements ObligacionRepositoryPort {
  private final JpaObligacionRepository repository;
  private final ObligacionPagoPersistenceMapper mapper;
  private final EntityManager entityManager;
  private final com.colegio.shuji.config.persistence.SupabaseAuditInterceptor auditoria;

  public ObligacionPago guardar(ObligacionPago valor) {
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

  public Optional<ObligacionPago> buscarPorId(Long id) {
    return repository.findById(id).map(mapper::toDomain);
  }

  public Optional<ObligacionPago> bloquearPorId(Long id) {
    return repository
        .bloquearPorId(id)
        .map(
            e -> {
              entityManager.refresh(e);
              return mapper.toDomain(e);
            });
  }

  public List<ObligacionPago> listar() {
    return repository.findAll(org.springframework.data.domain.Sort.by("id")).stream()
        .map(mapper::toDomain)
        .toList();
  }

  public List<ObligacionPago> buscarPorMatriculaId(Long valor) {
    return repository.buscarPorMatriculaId(valor).stream().map(mapper::toDomain).toList();
  }

  public List<ObligacionPago> buscarPorMatriculaIds(Collection<Long> ids) {
    if (ids == null || ids.isEmpty()) return List.of();
    return repository.buscarPorMatriculaIds(ids).stream().map(mapper::toDomain).toList();
  }

  public List<ObligacionPago> buscarPorConceptoId(Short valor) {
    return repository.buscarPorConceptoId(valor).stream().map(mapper::toDomain).toList();
  }

  public List<ObligacionPago> buscarPorEstado(EstadoObligacion valor) {
    return repository.buscarPorEstado(valor).stream().map(mapper::toDomain).toList();
  }
}
