package com.colegio.shuji.tesoreria.infrastructure.repository;

import com.colegio.shuji.tesoreria.infrastructure.entity.ConceptoCobroEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaConceptoCobroRepository extends JpaRepository<ConceptoCobroEntity, Short> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from ConceptoCobroEntity e where e.id = :id")
  Optional<ConceptoCobroEntity> bloquearPorId(@Param("id") Short id);

  @Query("select e from ConceptoCobroEntity e where e.codigo = :valor order by e.id")
  List<ConceptoCobroEntity> buscarPorCodigo(@Param("valor") String valor);
}
