package com.colegio.shuji.matricula.infrastructure.adapter;

import com.colegio.shuji.matricula.application.port.out.EstudianteRepositoryPort;
import com.colegio.shuji.matricula.domain.model.Estudiante;
import com.colegio.shuji.matricula.infrastructure.repository.JpaEstudianteRepository;
import jakarta.persistence.EntityManager;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class EstudianteRepositoryAdapter implements EstudianteRepositoryPort {
  private final JpaEstudianteRepository repository;
  private final EstudiantePersistenceMapper mapper;
  private final EntityManager entityManager;
  private final com.colegio.shuji.config.persistence.SupabaseAuditInterceptor auditoria;

  public Estudiante guardar(Estudiante valor) {
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

  public Optional<Estudiante> buscarPorId(Long id) {
    return repository.findById(id).map(mapper::toDomain);
  }

  public Optional<Estudiante> bloquearPorId(Long id) {
    return repository
        .bloquearPorId(id)
        .map(
            e -> {
              entityManager.refresh(e);
              return mapper.toDomain(e);
            });
  }

  public List<Estudiante> listar() {
    return repository.findAll(org.springframework.data.domain.Sort.by("id")).stream()
        .map(mapper::toDomain)
        .toList();
  }

  public List<Estudiante> buscarPorNumeroDocumento(String valor) {
    return repository.buscarPorNumeroDocumento(valor).stream().map(mapper::toDomain).toList();
  }

  public List<Estudiante> buscarPorNumerosDocumento(Collection<String> numeros) {
    if (numeros == null || numeros.isEmpty()) return List.of();
    return repository.buscarPorNumerosDocumento(numeros).stream().map(mapper::toDomain).toList();
  }

  public List<Estudiante> bloquearPorIds(Collection<Long> ids) {
    if (ids == null || ids.isEmpty()) return List.of();
    return repository.bloquearPorIds(ids).stream().map(mapper::toDomain).toList();
  }
}
