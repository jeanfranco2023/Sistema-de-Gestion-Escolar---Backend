package com.colegio.shuji.convivencia.infrastructure.adapter;

import com.colegio.shuji.convivencia.application.port.out.IncidenciaRepositoryPort;
import com.colegio.shuji.convivencia.domain.enums.*;
import com.colegio.shuji.convivencia.domain.model.IncidenciaConductual;
import com.colegio.shuji.convivencia.infrastructure.repository.JpaIncidenciaRepository;
import jakarta.persistence.EntityManager;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class IncidenciaRepositoryAdapter implements IncidenciaRepositoryPort {
  private final JpaIncidenciaRepository repository;
  private final IncidenciaConductualPersistenceMapper mapper;
  private final EntityManager entityManager;
  private final com.colegio.shuji.config.persistence.SupabaseAuditInterceptor auditoria;

  public IncidenciaConductual guardar(IncidenciaConductual valor) {
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

  public Optional<IncidenciaConductual> buscarPorId(Long id) {
    return repository.findById(id).map(mapper::toDomain);
  }

  public Optional<IncidenciaConductual> bloquearPorId(Long id) {
    return repository
        .bloquearPorId(id)
        .map(
            e -> {
              entityManager.refresh(e);
              return mapper.toDomain(e);
            });
  }

  public List<IncidenciaConductual> listar() {
    return repository.findAll(org.springframework.data.domain.Sort.by("id")).stream()
        .map(mapper::toDomain)
        .toList();
  }

  public List<IncidenciaConductual> buscarPorMatriculaId(Long valor) {
    return repository.buscarPorMatriculaId(valor).stream().map(mapper::toDomain).toList();
  }

  public List<IncidenciaConductual> buscarPorReportadoPorUsuarioId(Long valor) {
    return repository.buscarPorReportadoPorUsuarioId(valor).stream().map(mapper::toDomain).toList();
  }

  public List<IncidenciaConductual> buscarPorEstado(EstadoIncidencia valor) {
    return repository.buscarPorEstado(valor).stream().map(mapper::toDomain).toList();
  }
}
