package com.colegio.shuji.tesoreria.infrastructure.adapter;

import com.colegio.shuji.tesoreria.application.port.out.PagoRepositoryPort;
import com.colegio.shuji.tesoreria.domain.model.PagoTransaccion;
import com.colegio.shuji.tesoreria.infrastructure.repository.JpaPagoRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class PagoRepositoryAdapter implements PagoRepositoryPort {
  private final JpaPagoRepository repository;
  private final PagoTransaccionPersistenceMapper mapper;
  private final EntityManager entityManager;
  private final com.colegio.shuji.config.persistence.SupabaseAuditInterceptor auditoria;

  public PagoTransaccion guardar(PagoTransaccion valor) {
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

  public Optional<PagoTransaccion> buscarPorId(Long id) {
    return repository.findById(id).map(mapper::toDomain);
  }

  public Optional<PagoTransaccion> bloquearPorId(Long id) {
    return repository
        .bloquearPorId(id)
        .map(
            e -> {
              entityManager.refresh(e);
              return mapper.toDomain(e);
            });
  }

  public List<PagoTransaccion> listar() {
    return repository.findAll(org.springframework.data.domain.Sort.by("id")).stream()
        .map(mapper::toDomain)
        .toList();
  }

  public List<PagoTransaccion> buscarPorObligacionPagoId(Long valor) {
    return repository.buscarPorObligacionPagoId(valor).stream().map(mapper::toDomain).toList();
  }

  public List<PagoTransaccion> buscarPorPasarelaTransaccionId(String valor) {
    return repository.buscarPorPasarelaTransaccionId(valor).stream().map(mapper::toDomain).toList();
  }
}
