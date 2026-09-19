package com.colegio.shuji.usuario.infrastructure.repository;

import com.colegio.shuji.usuario.infrastructure.entity.SesionEntity;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio Spring Data JPA para la entidad SesionEntity. Cumple con la directiva estricta de
 * consultas en JPQL sin SQL nativo.
 */
public interface JpaSesionRepository extends JpaRepository<SesionEntity, Long> {

  @Query(
      "SELECT s FROM SesionEntity s JOIN FETCH s.usuario u LEFT JOIN FETCH u.roles WHERE"
          + " s.tokenHash = :tokenHash")
  Optional<SesionEntity> findByTokenHash(@Param("tokenHash") String tokenHash);

  @Modifying
  @Query("DELETE FROM SesionEntity s WHERE s.usuario.id = :usuarioId")
  void deleteByUsuarioId(@Param("usuarioId") Long usuarioId);

  @Modifying
  @Query("DELETE FROM SesionEntity s WHERE s.expiraAt < :now")
  void deleteExpiredSessions(@Param("now") LocalDateTime now);
}
