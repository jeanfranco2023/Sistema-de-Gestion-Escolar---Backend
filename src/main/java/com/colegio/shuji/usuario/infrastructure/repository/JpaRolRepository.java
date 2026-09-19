package com.colegio.shuji.usuario.infrastructure.repository;

import com.colegio.shuji.usuario.infrastructure.entity.RolEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio Spring Data JPA para la entidad RolEntity. Cumple con la directiva estricta de
 * consultas en JPQL sin SQL nativo.
 */
public interface JpaRolRepository extends JpaRepository<RolEntity, Short> {

  @Query("SELECT r FROM RolEntity r WHERE UPPER(r.codigo) = UPPER(:codigo)")
  Optional<RolEntity> findByCodigo(@Param("codigo") String codigo);

  @Query("SELECT r FROM RolEntity r WHERE r.codigo IN :codigos")
  List<RolEntity> findByCodigoIn(@Param("codigos") Collection<String> codigos);

  @Query("SELECT r FROM RolEntity r ORDER BY r.id ASC")
  List<RolEntity> findAllOrderByIdAsc();
}
