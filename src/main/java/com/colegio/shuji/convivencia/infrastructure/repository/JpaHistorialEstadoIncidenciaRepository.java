package com.colegio.shuji.convivencia.infrastructure.repository;

import com.colegio.shuji.convivencia.infrastructure.entity.HistorialEstadoIncidenciaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaHistorialEstadoIncidenciaRepository
    extends JpaRepository<HistorialEstadoIncidenciaEntity, Long> {
  List<HistorialEstadoIncidenciaEntity> findAllByIncidenciaIdOrderByFechaCambioAscIdAsc(
      Long incidenciaId);
}
