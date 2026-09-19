package com.colegio.shuji.comunicado.infrastructure.adapter;

import com.colegio.shuji.comunicado.application.port.out.DestinatarioRepositoryPort;
import com.colegio.shuji.comunicado.domain.model.ComunicadoDestinatario;
import com.colegio.shuji.comunicado.infrastructure.repository.JpaDestinatarioRepository;
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
public class DestinatarioRepositoryAdapter implements DestinatarioRepositoryPort {
  private final JpaDestinatarioRepository repository;
  private final ComunicadoDestinatarioPersistenceMapper mapper;
  private final EntityManager entityManager;
  private final com.colegio.shuji.config.persistence.SupabaseAuditInterceptor auditoria;

  public ComunicadoDestinatario guardar(ComunicadoDestinatario valor) {
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

  public Optional<ComunicadoDestinatario> buscarPorId(Long id) {
    return repository.findById(id).map(mapper::toDomain);
  }

  public Optional<ComunicadoDestinatario> bloquearPorId(Long id) {
    return repository
        .bloquearPorId(id)
        .map(
            e -> {
              entityManager.refresh(e);
              return mapper.toDomain(e);
            });
  }

  public List<ComunicadoDestinatario> listar() {
    return repository.findAll(org.springframework.data.domain.Sort.by("id")).stream()
        .map(mapper::toDomain)
        .toList();
  }

  public List<ComunicadoDestinatario> buscarPorComunicadoId(Long valor) {
    return repository.buscarPorComunicadoId(valor).stream().map(mapper::toDomain).toList();
  }

  public List<ComunicadoDestinatario> buscarPorApoderadoId(Long valor) {
    return repository.buscarPorApoderadoId(valor).stream().map(mapper::toDomain).toList();
  }
}
