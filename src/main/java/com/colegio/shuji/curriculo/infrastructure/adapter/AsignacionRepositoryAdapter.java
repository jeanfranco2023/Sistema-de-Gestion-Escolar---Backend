package com.colegio.shuji.curriculo.infrastructure.adapter;

import com.colegio.shuji.curriculo.application.port.out.AsignacionRepositoryPort;
import com.colegio.shuji.curriculo.domain.model.AsignacionDocente;
import com.colegio.shuji.curriculo.infrastructure.repository.JpaAsignacionRepository;
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
public class AsignacionRepositoryAdapter implements AsignacionRepositoryPort {
  private final JpaAsignacionRepository repository;
  private final AsignacionDocentePersistenceMapper mapper;
  private final EntityManager entityManager;
  private final com.colegio.shuji.config.persistence.SupabaseAuditInterceptor auditoria;

  public AsignacionDocente guardar(AsignacionDocente valor) {
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

  public Optional<AsignacionDocente> buscarPorId(Long id) {
    return repository.findById(id).map(mapper::toDomain);
  }

  public Optional<AsignacionDocente> bloquearPorId(Long id) {
    return repository
        .bloquearPorId(id)
        .map(
            e -> {
              entityManager.refresh(e);
              return mapper.toDomain(e);
            });
  }

  public List<AsignacionDocente> listar() {
    return repository.findAll(org.springframework.data.domain.Sort.by("id")).stream()
        .map(mapper::toDomain)
        .toList();
  }

  public List<AsignacionDocente> buscarPorDocenteUsuarioId(Long valor) {
    return repository.buscarPorDocenteUsuarioId(valor).stream().map(mapper::toDomain).toList();
  }

  public List<AsignacionDocente> buscarPorSeccionId(Integer valor) {
    return repository.buscarPorSeccionId(valor).stream().map(mapper::toDomain).toList();
  }

  public List<AsignacionDocente> buscarPorAnioLectivoId(Short valor) {
    return repository.buscarPorAnioLectivoId(valor).stream().map(mapper::toDomain).toList();
  }

  public List<AsignacionDocente> buscarPorNivelId(Short valor) {
    return repository.buscarPorNivelId(valor).stream().map(mapper::toDomain).toList();
  }

  public List<AsignacionDocente> buscarPorAreaCurricularId(Short valor) {
    return repository.buscarPorAreaCurricularId(valor).stream().map(mapper::toDomain).toList();
  }
}
