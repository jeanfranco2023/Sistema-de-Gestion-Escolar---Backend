package com.colegio.shuji.asistencia.infrastructure.adapter;

import com.colegio.shuji.asistencia.application.port.out.BiometricoRepositoryPort;
import com.colegio.shuji.asistencia.domain.model.LoteBiometrico;
import com.colegio.shuji.asistencia.infrastructure.repository.JpaBiometricoRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class BiometricoRepositoryAdapter implements BiometricoRepositoryPort {
  private final JpaBiometricoRepository repository;
  private final LoteBiometricoPersistenceMapper mapper;
  private final EntityManager entityManager;
  private final com.colegio.shuji.config.persistence.SupabaseAuditInterceptor auditoria;

  public LoteBiometrico guardar(LoteBiometrico valor) {
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

  public Optional<LoteBiometrico> buscarPorId(Long id) {
    return repository.findById(id).map(mapper::toDomain);
  }

  public Optional<LoteBiometrico> buscarPorHashContenido(String hashContenido) {
    return repository.findByHashContenido(hashContenido).map(mapper::toDomain);
  }

  public Optional<LoteBiometrico> bloquearPorId(Long id) {
    return repository
        .bloquearPorId(id)
        .map(
            e -> {
              entityManager.refresh(e);
              return mapper.toDomain(e);
            });
  }

  public List<LoteBiometrico> listar() {
    return repository.findAll(org.springframework.data.domain.Sort.by("id")).stream()
        .map(mapper::toDomain)
        .toList();
  }

  public List<LoteBiometrico> buscarPorImportadoPorUsuarioId(Long valor) {
    return repository.buscarPorImportadoPorUsuarioId(valor).stream().map(mapper::toDomain).toList();
  }
}
