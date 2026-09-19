package com.colegio.shuji.asistencia.infrastructure.adapter;

import com.colegio.shuji.asistencia.application.port.out.MarcaPorteriaRepositoryPort;
import com.colegio.shuji.asistencia.domain.model.MarcaPorteria;
import com.colegio.shuji.asistencia.infrastructure.repository.JpaMarcaPorteriaRepository;
import jakarta.persistence.EntityManager;
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
public class MarcaPorteriaRepositoryAdapter implements MarcaPorteriaRepositoryPort {
  private final JpaMarcaPorteriaRepository repository;
  private final MarcaPorteriaPersistenceMapper mapper;
  private final EntityManager entityManager;
  private final com.colegio.shuji.config.persistence.SupabaseAuditInterceptor auditoria;

  public MarcaPorteria guardar(MarcaPorteria valor) {
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

  public List<MarcaPorteria> guardarTodos(List<MarcaPorteria> valores) {
    if (valores == null || valores.isEmpty()) return List.of();
    auditoria.syncCurrentUserFromSecurityContext();
    var entities = valores.stream().map(mapper::toEntity).toList();
    var guardadas = repository.saveAll(entities);
    return guardadas.stream().map(mapper::toDomain).toList();
  }

  public Optional<MarcaPorteria> buscarPorId(Long id) {
    return repository.findById(id).map(mapper::toDomain);
  }

  public Optional<MarcaPorteria> bloquearPorId(Long id) {
    return repository
        .bloquearPorId(id)
        .map(
            e -> {
              entityManager.refresh(e);
              return mapper.toDomain(e);
            });
  }

  public List<MarcaPorteria> listar() {
    return repository.findAll(org.springframework.data.domain.Sort.by("id")).stream()
        .map(mapper::toDomain)
        .toList();
  }

  public List<MarcaPorteria> buscarPorLoteId(Long valor) {
    return repository.buscarPorLoteId(valor).stream().map(mapper::toDomain).toList();
  }

  public List<MarcaPorteria> buscarPorEstudianteId(Long valor) {
    return repository.buscarPorEstudianteId(valor).stream().map(mapper::toDomain).toList();
  }

  public List<MarcaPorteria> buscarPorRangoFecha(java.time.OffsetDateTime inicio, java.time.OffsetDateTime fin) {
    return repository.buscarPorRangoFecha(inicio, fin).stream().map(mapper::toDomain).toList();
  }
}
