package com.colegio.shuji.matricula.infrastructure.adapter;

import com.colegio.shuji.matricula.application.port.out.MatriculaRepositoryPort;
import com.colegio.shuji.matricula.domain.enums.EstadoMatricula;
import com.colegio.shuji.matricula.domain.model.Matricula;
import com.colegio.shuji.matricula.infrastructure.repository.JpaMatriculaRepository;
import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class MatriculaRepositoryAdapter implements MatriculaRepositoryPort {
  private final JpaMatriculaRepository repository;
  private final MatriculaPersistenceMapper mapper;
  private final EntityManager entityManager;
  private final com.colegio.shuji.config.persistence.SupabaseAuditInterceptor auditoria;

  public Matricula guardar(Matricula valor) {
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

  public Optional<Matricula> buscarPorId(Long id) {
    return repository.findById(id).map(mapper::toDomain);
  }

  public Optional<Matricula> bloquearPorId(Long id) {
    return repository
        .bloquearPorId(id)
        .map(
            e -> {
              entityManager.refresh(e);
              return mapper.toDomain(e);
            });
  }

  public List<Matricula> listar() {
    return repository.findAll(org.springframework.data.domain.Sort.by("id")).stream()
        .map(mapper::toDomain)
        .toList();
  }

  public List<Matricula> buscarPorAnioLectivoId(Short valor) {
    return repository.buscarPorAnioLectivoId(valor).stream().map(mapper::toDomain).toList();
  }

  public List<Matricula> buscarPorEstudianteId(Long valor) {
    return repository.buscarPorEstudianteId(valor).stream().map(mapper::toDomain).toList();
  }

  public List<Matricula> buscarPorSeccionId(Integer valor) {
    return repository.buscarPorSeccionId(valor).stream().map(mapper::toDomain).toList();
  }

  public List<Matricula> buscarPorEstadoMatricula(EstadoMatricula valor) {
    return repository.buscarPorEstadoMatricula(valor).stream().map(mapper::toDomain).toList();
  }

  public List<Matricula> buscarPorIds(Collection<Long> ids) {
    if (ids == null || ids.isEmpty()) return List.of();
    return repository.buscarPorIds(ids).stream().map(mapper::toDomain).toList();
  }

  public List<Matricula> bloquearPorIds(Collection<Long> ids) {
    if (ids == null || ids.isEmpty()) return List.of();
    return repository.bloquearPorIds(ids).stream().map(mapper::toDomain).toList();
  }
}
