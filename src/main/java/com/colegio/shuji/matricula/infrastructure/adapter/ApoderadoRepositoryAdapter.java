package com.colegio.shuji.matricula.infrastructure.adapter;

import com.colegio.shuji.matricula.application.port.out.ApoderadoRepositoryPort;
import com.colegio.shuji.matricula.domain.model.Apoderado;
import com.colegio.shuji.matricula.infrastructure.repository.JpaApoderadoRepository;
import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class ApoderadoRepositoryAdapter implements ApoderadoRepositoryPort {
  private final JpaApoderadoRepository repository;
  private final ApoderadoPersistenceMapper mapper;
  private final EntityManager entityManager;
  private final com.colegio.shuji.config.persistence.SupabaseAuditInterceptor auditoria;

  public Apoderado guardar(Apoderado valor) {
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

  public Optional<Apoderado> buscarPorId(Long id) {
    return repository.findById(id).map(mapper::toDomain);
  }

  public Optional<Apoderado> bloquearPorId(Long id) {
    return repository
        .bloquearPorId(id)
        .map(
            e -> {
              entityManager.refresh(e);
              return mapper.toDomain(e);
            });
  }

  public List<Apoderado> listar() {
    return repository.findAll(org.springframework.data.domain.Sort.by("id")).stream()
        .map(mapper::toDomain)
        .toList();
  }

  public List<Apoderado> buscarPorUsuarioId(Long valor) {
    return repository.buscarPorUsuarioId(valor).stream().map(mapper::toDomain).toList();
  }

  public List<Apoderado> buscarPorNumeroDocumento(String valor) {
    return repository.buscarPorNumeroDocumento(valor).stream().map(mapper::toDomain).toList();
  }

  public List<Apoderado> buscarPorIds(Collection<Long> ids) {
    if (ids == null || ids.isEmpty()) return List.of();
    return repository.buscarPorIds(ids).stream().map(mapper::toDomain).toList();
  }
}
