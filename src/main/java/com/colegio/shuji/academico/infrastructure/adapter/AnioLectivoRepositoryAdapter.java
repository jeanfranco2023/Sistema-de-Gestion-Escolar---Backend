package com.colegio.shuji.academico.infrastructure.adapter;

import com.colegio.shuji.academico.application.port.out.AnioLectivoRepositoryPort;
import com.colegio.shuji.academico.domain.model.AnioLectivo;
import com.colegio.shuji.academico.infrastructure.repository.JpaAnioLectivoRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class AnioLectivoRepositoryAdapter implements AnioLectivoRepositoryPort {
  private final JpaAnioLectivoRepository repository;
  private final AnioLectivoPersistenceMapper mapper;
  private final EntityManager entityManager;
  private final com.colegio.shuji.config.persistence.SupabaseAuditInterceptor auditoria;

  public AnioLectivo guardar(AnioLectivo valor) {
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

  public Optional<AnioLectivo> buscarPorId(Short id) {
    return repository.findById(id).map(mapper::toDomain);
  }

  public Optional<AnioLectivo> bloquearPorId(Short id) {
    return repository
        .bloquearPorId(id)
        .map(
            e -> {
              entityManager.refresh(e);
              return mapper.toDomain(e);
            });
  }

  public List<AnioLectivo> listar() {
    return repository.findAll(org.springframework.data.domain.Sort.by("id")).stream()
        .map(mapper::toDomain)
        .toList();
  }
}
