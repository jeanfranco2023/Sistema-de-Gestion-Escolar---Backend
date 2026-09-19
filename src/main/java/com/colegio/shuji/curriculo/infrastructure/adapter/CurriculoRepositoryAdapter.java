package com.colegio.shuji.curriculo.infrastructure.adapter;

import com.colegio.shuji.curriculo.application.port.out.CurriculoRepositoryPort;
import com.colegio.shuji.curriculo.domain.model.AreaCurricular;
import com.colegio.shuji.curriculo.infrastructure.repository.JpaCurriculoRepository;
import jakarta.persistence.EntityManager;
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
public class CurriculoRepositoryAdapter implements CurriculoRepositoryPort {
  private final JpaCurriculoRepository repository;
  private final AreaCurricularPersistenceMapper mapper;
  private final EntityManager entityManager;
  private final com.colegio.shuji.config.persistence.SupabaseAuditInterceptor auditoria;

  public AreaCurricular guardar(AreaCurricular valor) {
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

  public Optional<AreaCurricular> buscarPorId(Short id) {
    return repository.findById(id).map(mapper::toDomain);
  }

  public Optional<AreaCurricular> bloquearPorId(Short id) {
    return repository
        .bloquearPorId(id)
        .map(
            e -> {
              entityManager.refresh(e);
              return mapper.toDomain(e);
            });
  }

  public List<AreaCurricular> listar() {
    return repository.findAll(org.springframework.data.domain.Sort.by("id")).stream()
        .map(mapper::toDomain)
        .toList();
  }

  public List<AreaCurricular> buscarPorNivelId(Short valor) {
    return repository.buscarPorNivelId(valor).stream().map(mapper::toDomain).toList();
  }

  public List<AreaCurricular> buscarPorCodigo(String valor) {
    return repository.buscarPorCodigo(valor).stream().map(mapper::toDomain).toList();
  }
}
