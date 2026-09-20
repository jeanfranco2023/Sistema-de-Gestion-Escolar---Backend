package com.colegio.shuji.tesoreria.infrastructure.adapter;

import com.colegio.shuji.tesoreria.application.port.out.ComprobanteRepositoryPort;
import com.colegio.shuji.tesoreria.domain.model.ComprobantePago;
import com.colegio.shuji.tesoreria.infrastructure.repository.JpaComprobanteRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class ComprobanteRepositoryAdapter implements ComprobanteRepositoryPort {
  private final JpaComprobanteRepository repository;
  private final ComprobantePagoPersistenceMapper mapper;
  private final EntityManager entityManager;
  private final com.colegio.shuji.config.persistence.SupabaseAuditInterceptor auditoria;

  public ComprobantePago guardar(ComprobantePago valor) {
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

  public Optional<ComprobantePago> buscarPorId(Long id) {
    return repository.findById(id).map(mapper::toDomain);
  }

  public Optional<ComprobantePago> bloquearPorId(Long id) {
    return repository
        .bloquearPorId(id)
        .map(
            e -> {
              entityManager.refresh(e);
              return mapper.toDomain(e);
            });
  }

  public List<ComprobantePago> listar() {
    return repository.findAll(org.springframework.data.domain.Sort.by("id")).stream()
        .map(mapper::toDomain)
        .toList();
  }

  public List<ComprobantePago> buscarPorPagoTransaccionId(Long valor) {
    return repository.buscarPorPagoTransaccionId(valor).stream().map(mapper::toDomain).toList();
  }

  public Optional<Integer> obtenerUltimoCorrelativo(String serie) {
    return repository
        .findFirstBySerieOrderByCorrelativoDesc(serie)
        .map(com.colegio.shuji.tesoreria.infrastructure.entity.ComprobanteEntity::getCorrelativo);
  }
}
