package com.colegio.shuji.academico.infrastructure.adapter;

import com.colegio.shuji.academico.application.port.out.GradoRepositoryPort;
import com.colegio.shuji.academico.domain.model.Grado;
import com.colegio.shuji.academico.infrastructure.repository.JpaGradoRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class GradoRepositoryAdapter implements GradoRepositoryPort {
  private final JpaGradoRepository repository;
  private final GradoPersistenceMapper mapper;
  private final EntityManager entityManager;
  private final com.colegio.shuji.config.persistence.SupabaseAuditInterceptor auditoria;

  public Grado guardar(Grado valor) {
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

  public Optional<Grado> buscarPorId(Short id) {
    return repository.findById(id).map(mapper::toDomain);
  }

  public Optional<Grado> bloquearPorId(Short id) {
    return repository
        .bloquearPorId(id)
        .map(
            e -> {
              entityManager.refresh(e);
              return mapper.toDomain(e);
            });
  }

  public List<Grado> listar() {
    return repository.findAll(org.springframework.data.domain.Sort.by("id")).stream()
        .map(mapper::toDomain)
        .toList();
  }

  public List<Grado> buscarPorNivelId(Short valor) {
    return repository.buscarPorNivelId(valor).stream().map(mapper::toDomain).toList();
  }
}
