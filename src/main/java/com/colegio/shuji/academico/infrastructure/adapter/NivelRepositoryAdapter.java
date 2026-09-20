package com.colegio.shuji.academico.infrastructure.adapter;

import com.colegio.shuji.academico.application.port.out.NivelRepositoryPort;
import com.colegio.shuji.academico.domain.enums.NivelCodigo;
import com.colegio.shuji.academico.domain.model.Nivel;
import com.colegio.shuji.academico.infrastructure.repository.JpaNivelRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class NivelRepositoryAdapter implements NivelRepositoryPort {
  private final JpaNivelRepository repository;
  private final NivelPersistenceMapper mapper;
  private final EntityManager entityManager;
  private final com.colegio.shuji.config.persistence.SupabaseAuditInterceptor auditoria;

  public Nivel guardar(Nivel valor) {
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

  public Optional<Nivel> buscarPorId(Short id) {
    return repository.findById(id).map(mapper::toDomain);
  }

  public Optional<Nivel> bloquearPorId(Short id) {
    return repository
        .bloquearPorId(id)
        .map(
            e -> {
              entityManager.refresh(e);
              return mapper.toDomain(e);
            });
  }

  public List<Nivel> listar() {
    return repository.findAll(org.springframework.data.domain.Sort.by("id")).stream()
        .map(mapper::toDomain)
        .toList();
  }

  public List<Nivel> buscarPorCodigo(NivelCodigo valor) {
    return repository.buscarPorCodigo(valor).stream().map(mapper::toDomain).toList();
  }
}
