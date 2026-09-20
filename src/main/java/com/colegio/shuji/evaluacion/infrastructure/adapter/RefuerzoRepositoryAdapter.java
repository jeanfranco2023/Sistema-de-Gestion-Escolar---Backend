package com.colegio.shuji.evaluacion.infrastructure.adapter;

import com.colegio.shuji.evaluacion.application.port.out.RefuerzoRepositoryPort;
import com.colegio.shuji.evaluacion.domain.model.SesionRefuerzo;
import com.colegio.shuji.evaluacion.infrastructure.repository.JpaRefuerzoRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class RefuerzoRepositoryAdapter implements RefuerzoRepositoryPort {
  private final JpaRefuerzoRepository repository;
  private final SesionRefuerzoPersistenceMapper mapper;
  private final EntityManager entityManager;
  private final com.colegio.shuji.config.persistence.SupabaseAuditInterceptor auditoria;

  public SesionRefuerzo guardar(SesionRefuerzo valor) {
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

  public Optional<SesionRefuerzo> buscarPorId(Long id) {
    return repository.findById(id).map(mapper::toDomain);
  }

  public Optional<SesionRefuerzo> bloquearPorId(Long id) {
    return repository
        .bloquearPorId(id)
        .map(
            e -> {
              entityManager.refresh(e);
              return mapper.toDomain(e);
            });
  }

  public List<SesionRefuerzo> listar() {
    return repository.findAll(org.springframework.data.domain.Sort.by("id")).stream()
        .map(mapper::toDomain)
        .toList();
  }

  public List<SesionRefuerzo> buscarPorAnioLectivoId(Short valor) {
    return repository.buscarPorAnioLectivoId(valor).stream().map(mapper::toDomain).toList();
  }

  public List<SesionRefuerzo> buscarPorPeriodoAcademicoId(Short valor) {
    return repository.buscarPorPeriodoAcademicoId(valor).stream().map(mapper::toDomain).toList();
  }

  public List<SesionRefuerzo> buscarPorAreaCurricularId(Short valor) {
    return repository.buscarPorAreaCurricularId(valor).stream().map(mapper::toDomain).toList();
  }

  public List<SesionRefuerzo> buscarPorDocenteUsuarioId(Long valor) {
    return repository.buscarPorDocenteUsuarioId(valor).stream().map(mapper::toDomain).toList();
  }
}
