package com.colegio.shuji.curriculo.infrastructure.adapter;

import com.colegio.shuji.curriculo.application.port.out.HorarioRepositoryPort;
import com.colegio.shuji.curriculo.domain.model.HorarioSeccion;
import com.colegio.shuji.curriculo.infrastructure.repository.JpaHorarioRepository;
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
public class HorarioRepositoryAdapter implements HorarioRepositoryPort {
  private final JpaHorarioRepository repository;
  private final HorarioSeccionPersistenceMapper mapper;
  private final EntityManager entityManager;
  private final com.colegio.shuji.config.persistence.SupabaseAuditInterceptor auditoria;

  public HorarioSeccion guardar(HorarioSeccion valor) {
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

  public Optional<HorarioSeccion> buscarPorId(Long id) {
    return repository.findById(id).map(mapper::toDomain);
  }

  public Optional<HorarioSeccion> bloquearPorId(Long id) {
    return repository
        .bloquearPorId(id)
        .map(
            e -> {
              entityManager.refresh(e);
              return mapper.toDomain(e);
            });
  }

  public List<HorarioSeccion> listar() {
    return repository.findAll(org.springframework.data.domain.Sort.by("id")).stream()
        .map(mapper::toDomain)
        .toList();
  }

  public List<HorarioSeccion> buscarPorAnioLectivoId(Short valor) {
    return repository.buscarPorAnioLectivoId(valor).stream().map(mapper::toDomain).toList();
  }

  public List<HorarioSeccion> buscarPorSeccionId(Integer valor) {
    return repository.buscarPorSeccionId(valor).stream().map(mapper::toDomain).toList();
  }

  public List<HorarioSeccion> buscarPorDocenteUsuarioId(Long valor) {
    return repository.buscarPorDocenteUsuarioId(valor).stream().map(mapper::toDomain).toList();
  }

  public List<HorarioSeccion> buscarPorAsignacionDocenteId(Long valor) {
    return repository.buscarPorAsignacionDocenteId(valor).stream().map(mapper::toDomain).toList();
  }

  public List<HorarioSeccion> buscarPorBloqueHorarioId(Short valor) {
    return repository.buscarPorBloqueHorarioId(valor).stream().map(mapper::toDomain).toList();
  }
}
