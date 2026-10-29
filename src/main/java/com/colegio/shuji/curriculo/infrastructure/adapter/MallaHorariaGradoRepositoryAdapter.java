package com.colegio.shuji.curriculo.infrastructure.adapter;

import com.colegio.shuji.curriculo.application.port.out.MallaHorariaGradoRepositoryPort;
import com.colegio.shuji.curriculo.domain.model.MallaHorariaGrado;
import com.colegio.shuji.curriculo.infrastructure.entity.MallaHorariaGradoEntity;
import com.colegio.shuji.curriculo.infrastructure.repository.JpaMallaHorariaGradoRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class MallaHorariaGradoRepositoryAdapter implements MallaHorariaGradoRepositoryPort {
  private final JpaMallaHorariaGradoRepository repository;
  private final EntityManager entityManager;
  private final com.colegio.shuji.config.persistence.SupabaseAuditInterceptor auditoria;

  @Override
  public MallaHorariaGrado guardar(MallaHorariaGrado valor) {
    auditoria.syncCurrentUserFromSecurityContext();
    var entity = valor.getId() == null ? new MallaHorariaGradoEntity()
        : repository.findById(valor.getId()).orElseThrow(
            () -> new com.colegio.shuji.shared.domain.exception.BusinessException("Horario de grado no encontrado"));
    entity.setAnioLectivoId(valor.getAnioLectivoId());
    entity.setGradoId(valor.getGradoId());
    entity.setAreaCurricularId(valor.getAreaCurricularId());
    entity.setDiaSemana(valor.getDiaSemana());
    entity.setBloqueHorarioId(valor.getBloqueHorarioId());
    entity.setActivo(valor.getActivo());
    repository.saveAndFlush(entity);
    entityManager.refresh(entity);
    valor.setId(entity.getId());
    return valor;
  }

  @Override public Optional<MallaHorariaGrado> buscarPorId(Long id) {
    return repository.findById(id).map(this::toDomain);
  }

  @Override public Optional<MallaHorariaGrado> bloquearPorId(Long id) {
    return repository.bloquearPorId(id).map(this::toDomain);
  }

  @Override public List<MallaHorariaGrado> buscarPorGradoYAnio(Short gradoId, Short anioLectivoId) {
    return repository.buscarPorGradoYAnio(gradoId, anioLectivoId).stream().map(this::toDomain).toList();
  }

  private MallaHorariaGrado toDomain(MallaHorariaGradoEntity e) {
    return MallaHorariaGrado.builder().id(e.getId()).anioLectivoId(e.getAnioLectivoId())
        .gradoId(e.getGradoId()).areaCurricularId(e.getAreaCurricularId())
        .diaSemana(e.getDiaSemana()).bloqueHorarioId(e.getBloqueHorarioId()).activo(e.getActivo()).build();
  }
}
