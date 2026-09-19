package com.colegio.shuji.academico.infrastructure.adapter;

import com.colegio.shuji.academico.application.port.out.AulaRepositoryPort;
import com.colegio.shuji.academico.domain.model.Aula;
import com.colegio.shuji.academico.infrastructure.entity.AulaEntity;
import com.colegio.shuji.academico.infrastructure.repository.JpaAulaRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AulaRepositoryAdapter implements AulaRepositoryPort {
  private final JpaAulaRepository repository;

  public Aula guardar(Aula aula) {
    var entity = new AulaEntity();
    entity.setCodigo(aula.getCodigo());
    entity.setNombre(aula.getNombre());
    entity.setUbicacion(aula.getUbicacion());
    entity.setCapacidad(aula.getCapacidad());
    entity.setActiva(aula.getActiva());
    return toDomain(repository.saveAndFlush(entity));
  }

  public Optional<Aula> buscarPorCodigo(String codigo) {
    return repository.findByCodigoIgnoreCase(codigo).map(this::toDomain);
  }

  public List<Aula> listar() {
    return repository.findAll(Sort.by("codigo")).stream().map(this::toDomain).toList();
  }

  private Aula toDomain(AulaEntity entity) {
    return Aula.builder()
        .id(entity.getId())
        .codigo(entity.getCodigo())
        .nombre(entity.getNombre())
        .ubicacion(entity.getUbicacion())
        .capacidad(entity.getCapacidad())
        .activa(entity.getActiva())
        .build();
  }
}
