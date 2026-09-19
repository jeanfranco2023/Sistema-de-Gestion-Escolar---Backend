package com.colegio.shuji.tesoreria.infrastructure.repository;

import com.colegio.shuji.tesoreria.infrastructure.entity.SerieComprobanteEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaSerieComprobanteRepository
    extends JpaRepository<SerieComprobanteEntity, String> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from SerieComprobanteEntity s where s.serie = :serie")
  Optional<SerieComprobanteEntity> bloquearPorSerie(@Param("serie") String serie);
}
