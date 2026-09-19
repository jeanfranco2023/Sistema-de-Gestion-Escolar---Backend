package com.colegio.shuji.evaluacion.infrastructure.adapter;

import com.colegio.shuji.evaluacion.application.port.out.InscripcionRefuerzoRepositoryPort;
import com.colegio.shuji.evaluacion.domain.model.InscripcionRefuerzo;
import com.colegio.shuji.evaluacion.infrastructure.repository.JpaInscripcionRefuerzoRepository;
import jakarta.persistence.EntityManager;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class InscripcionRefuerzoRepositoryAdapter implements InscripcionRefuerzoRepositoryPort {
  private final JpaInscripcionRefuerzoRepository repository;
  private final InscripcionRefuerzoPersistenceMapper mapper;
  private final EntityManager entityManager;
  private final com.colegio.shuji.config.persistence.SupabaseAuditInterceptor auditoria;

  public InscripcionRefuerzo guardar(InscripcionRefuerzo valor) {
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

  public Optional<InscripcionRefuerzo> buscarPorId(Long id) {
    return repository.findById(id).map(mapper::toDomain);
  }

  public Optional<InscripcionRefuerzo> bloquearPorId(Long id) {
    return repository
        .bloquearPorId(id)
        .map(
            e -> {
              entityManager.refresh(e);
              return mapper.toDomain(e);
            });
  }

  public List<InscripcionRefuerzo> listar() {
    return repository.findAll(org.springframework.data.domain.Sort.by("id")).stream()
        .map(mapper::toDomain)
        .toList();
  }

  public List<InscripcionRefuerzo> buscarPorSesionRefuerzoId(Long valor) {
    return repository.buscarPorSesionRefuerzoId(valor).stream().map(mapper::toDomain).toList();
  }

  public List<InscripcionRefuerzo> buscarPorEstudianteId(Long valor) {
    return repository.buscarPorEstudianteId(valor).stream().map(mapper::toDomain).toList();
  }

  public List<InscripcionRefuerzo> buscarPorCalificacionOrigenId(Long valor) {
    return repository.buscarPorCalificacionOrigenId(valor).stream().map(mapper::toDomain).toList();
  }
}
