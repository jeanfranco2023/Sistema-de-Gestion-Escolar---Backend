package com.colegio.shuji.convivencia.infrastructure.adapter;

import com.colegio.shuji.convivencia.application.port.out.HistorialEstadoIncidenciaRepositoryPort;
import com.colegio.shuji.convivencia.domain.model.CambioEstadoIncidencia;
import com.colegio.shuji.convivencia.infrastructure.repository.JpaHistorialEstadoIncidenciaRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class HistorialEstadoIncidenciaRepositoryAdapter
    implements HistorialEstadoIncidenciaRepositoryPort {
  private final JpaHistorialEstadoIncidenciaRepository repository;
  private final HistorialEstadoIncidenciaPersistenceMapper mapper;

  @Override
  public CambioEstadoIncidencia guardar(CambioEstadoIncidencia cambio) {
    return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(cambio)));
  }

  @Override
  @Transactional(readOnly = true)
  public List<CambioEstadoIncidencia> buscarPorIncidenciaId(Long incidenciaId) {
    return repository.findAllByIncidenciaIdOrderByFechaCambioAscIdAsc(incidenciaId).stream()
        .map(mapper::toDomain)
        .toList();
  }
}
