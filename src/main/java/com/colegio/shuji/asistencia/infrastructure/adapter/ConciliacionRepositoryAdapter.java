package com.colegio.shuji.asistencia.infrastructure.adapter;

import com.colegio.shuji.asistencia.application.port.out.ConciliacionRepositoryPort;
import com.colegio.shuji.asistencia.domain.model.ConciliacionAsistencia;
import com.colegio.shuji.asistencia.infrastructure.repository.JpaConciliacionRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class ConciliacionRepositoryAdapter implements ConciliacionRepositoryPort {
  private final JpaConciliacionRepository repository;
  private final ConciliacionAsistenciaPersistenceMapper mapper;
  private final EntityManager entityManager;
  private final com.colegio.shuji.config.persistence.SupabaseAuditInterceptor auditoria;

  public ConciliacionAsistencia guardar(ConciliacionAsistencia valor) {
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

  public Optional<ConciliacionAsistencia> buscarPorId(Long id) {
    return repository.findById(id).map(mapper::toDomain);
  }

  public Optional<ConciliacionAsistencia> bloquearPorId(Long id) {
    return repository
        .bloquearPorId(id)
        .map(
            e -> {
              entityManager.refresh(e);
              return mapper.toDomain(e);
            });
  }

  public List<ConciliacionAsistencia> listar() {
    return repository.findAll(org.springframework.data.domain.Sort.by("id")).stream()
        .map(mapper::toDomain)
        .toList();
  }

  public List<ConciliacionAsistencia> buscarPorFecha(LocalDate valor) {
    return repository.buscarPorFecha(valor).stream().map(mapper::toDomain).toList();
  }

  public List<ConciliacionAsistencia> buscarPorEstudianteId(Long valor) {
    return repository.buscarPorEstudianteId(valor).stream().map(mapper::toDomain).toList();
  }
}
