package com.colegio.shuji.comunicado.infrastructure.repository;

import com.colegio.shuji.comunicado.infrastructure.entity.ComunicadoDestinatarioEntity;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface JpaDestinatarioRepository
    extends JpaRepository<ComunicadoDestinatarioEntity, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from ComunicadoDestinatarioEntity e where e.id = :id")
  Optional<ComunicadoDestinatarioEntity> bloquearPorId(@Param("id") Long id);

  @Query("select e from ComunicadoDestinatarioEntity e where e.comunicadoId = :valor order by e.id")
  List<ComunicadoDestinatarioEntity> buscarPorComunicadoId(@Param("valor") Long valor);

  @Query("select e from ComunicadoDestinatarioEntity e where e.apoderadoId = :valor order by e.id")
  List<ComunicadoDestinatarioEntity> buscarPorApoderadoId(@Param("valor") Long valor);
}
