package com.colegio.shuji.academico.infrastructure.adapter;

import com.colegio.shuji.academico.application.port.out.PeriodoRepositoryPort;
import com.colegio.shuji.academico.domain.model.PeriodoAcademico;
import com.colegio.shuji.academico.infrastructure.repository.JpaPeriodoRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class PeriodoRepositoryAdapter implements PeriodoRepositoryPort {
  private final JpaPeriodoRepository repository;
  private final PeriodoAcademicoPersistenceMapper mapper;
  private final EntityManager entityManager;
  private final com.colegio.shuji.config.persistence.SupabaseAuditInterceptor auditoria;

  public PeriodoAcademico guardar(PeriodoAcademico valor) {
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

  public Optional<PeriodoAcademico> buscarPorId(Short id) {
    return repository.findById(id).map(mapper::toDomain);
  }

  public Optional<PeriodoAcademico> bloquearPorId(Short id) {
    return repository
        .bloquearPorId(id)
        .map(
            e -> {
              entityManager.refresh(e);
              return mapper.toDomain(e);
            });
  }

  public List<PeriodoAcademico> listar() {
    return repository.findAll(org.springframework.data.domain.Sort.by("id")).stream()
        .map(mapper::toDomain)
        .toList();
  }

  public List<PeriodoAcademico> buscarPorAnioLectivoId(Short valor) {
    return repository.buscarPorAnioLectivoId(valor).stream().map(mapper::toDomain).toList();
  }
}
