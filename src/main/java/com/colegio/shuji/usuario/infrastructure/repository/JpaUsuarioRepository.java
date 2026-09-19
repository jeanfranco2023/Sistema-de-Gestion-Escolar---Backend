package com.colegio.shuji.usuario.infrastructure.repository;

import com.colegio.shuji.usuario.infrastructure.entity.UsuarioEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositorio Spring Data JPA para la entidad UsuarioEntity. Cumple con la directiva estricta de
 * consultas en JPQL sin SQL nativo.
 */
@Repository
public interface JpaUsuarioRepository extends JpaRepository<UsuarioEntity, Long> {

  @Query("SELECT u FROM UsuarioEntity u LEFT JOIN FETCH u.roles WHERE u.id = :id")
  Optional<UsuarioEntity> findByIdWithRoles(@Param("id") Long id);

  @Query(
      "SELECT DISTINCT u FROM UsuarioEntity u JOIN u.roles r LEFT JOIN FETCH u.roles WHERE"
          + " UPPER(r.codigo) = UPPER(:codigoRol)")
  List<UsuarioEntity> findByRolCodigo(@Param("codigoRol") String codigoRol);

  @Query("SELECT u FROM UsuarioEntity u LEFT JOIN FETCH u.roles WHERE u.username = :username")
  Optional<UsuarioEntity> findByUsername(@Param("username") String username);

  @Query("SELECT u FROM UsuarioEntity u LEFT JOIN FETCH u.roles WHERE u.email = :email")
  Optional<UsuarioEntity> findByEmail(@Param("email") String email);

  @Query(
      "SELECT u FROM UsuarioEntity u LEFT JOIN FETCH u.roles WHERE u.username = :identifier OR"
          + " u.email = :identifier")
  Optional<UsuarioEntity> findByUsernameOrEmail(@Param("identifier") String identifier);

  @Query("SELECT DISTINCT u FROM UsuarioEntity u LEFT JOIN FETCH u.roles ORDER BY u.id ASC")
  List<UsuarioEntity> findAllWithRoles();

  @Query(
      "SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM UsuarioEntity u WHERE u.username"
          + " = :username")
  boolean existsByUsername(@Param("username") String username);

  @Query(
      "SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM UsuarioEntity u WHERE u.email ="
          + " :email")
  boolean existsByEmail(@Param("email") String email);
}
