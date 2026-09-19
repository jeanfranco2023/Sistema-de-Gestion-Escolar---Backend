package com.colegio.shuji.curriculo.infrastructure.repository;

import com.colegio.shuji.curriculo.infrastructure.entity.AreaCurricularEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaCurriculoRepository extends JpaRepository<AreaCurricularEntity, Short> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from AreaCurricularEntity e where e.id = :id")
  Optional<AreaCurricularEntity> bloquearPorId(@Param("id") Short id);

  @Query("select e from AreaCurricularEntity e where e.nivelId = :valor order by e.id")
  List<AreaCurricularEntity> buscarPorNivelId(@Param("valor") Short valor);

  @Query("select e from AreaCurricularEntity e where e.codigo = :valor order by e.id")
  List<AreaCurricularEntity> buscarPorCodigo(@Param("valor") String valor);
}
