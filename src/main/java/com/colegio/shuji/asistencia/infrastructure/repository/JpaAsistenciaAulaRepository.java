package com.colegio.shuji.asistencia.infrastructure.repository;

import com.colegio.shuji.asistencia.domain.enums.EstadoAsistenciaAula;
import com.colegio.shuji.asistencia.application.port.out.SeccionAsistencia;
import com.colegio.shuji.asistencia.infrastructure.entity.AsistenciaAulaEntity;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaAsistenciaAulaRepository extends JpaRepository<AsistenciaAulaEntity, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from AsistenciaAulaEntity e where e.id = :id")
  Optional<AsistenciaAulaEntity> bloquearPorId(@Param("id") Long id);

  @Query("select e from AsistenciaAulaEntity e where e.matriculaId = :valor order by e.id")
  List<AsistenciaAulaEntity> buscarPorMatriculaId(@Param("valor") Long valor);

  @Query("select e from AsistenciaAulaEntity e where e.matriculaId in :ids order by e.matriculaId, e.id")
  List<AsistenciaAulaEntity> buscarPorMatriculaIds(@Param("ids") Collection<Long> matriculaIds);

  @Query("select e from AsistenciaAulaEntity e where e.fechaSesion = :valor order by e.id")
  List<AsistenciaAulaEntity> buscarPorFechaSesion(@Param("valor") LocalDate valor);

  @Query("""
      select new com.colegio.shuji.asistencia.application.port.out.SeccionAsistencia(
          m.id, m.seccionId, g.nombre, n.nombre, s.letra)
      from MatriculaEntity m
      left join m.relacion1 s
      left join s.relacion1 g
      left join g.relacion0 n
      where m.id in :ids
      """)
  List<SeccionAsistencia> buscarSeccionesPorMatriculaIds(@Param("ids") Collection<Long> matriculaIds);

  @Query("select distinct e.fechaSesion from AsistenciaAulaEntity e where e.fechaSesion between :desde and :hasta order by e.fechaSesion")
  List<LocalDate> buscarFechasConRegistros(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

  @Query("select e from AsistenciaAulaEntity e where e.fechaSesion = :fecha and e.matriculaId in :ids order by e.id")
  List<AsistenciaAulaEntity> buscarPorFechaSesionYMatriculaIds(
      @Param("fecha") LocalDate fecha, @Param("ids") Collection<Long> matriculaIds);

  @Query("select e from AsistenciaAulaEntity e where e.estado = :valor order by e.id")
  List<AsistenciaAulaEntity> buscarPorEstado(@Param("valor") EstadoAsistenciaAula valor);

  @Query("select e from AsistenciaAulaEntity e where e.auxiliarUsuarioId = :valor order by e.id")
  List<AsistenciaAulaEntity> buscarPorAuxiliarUsuarioId(@Param("valor") Long valor);

  @Query("select e.estado, count(e) from AsistenciaAulaEntity e where e.fechaSesion = :fecha group by e.estado")
  List<Object[]> contarPorEstadoEnFecha(@Param("fecha") LocalDate fecha);

  @Query("""
      select e.estado, count(e)
      from AsistenciaAulaEntity e
      where e.matriculaId in :ids
        and (:desde is null or e.fechaSesion >= :desde)
        and (:hasta is null or e.fechaSesion <= :hasta)
      group by e.estado
      """)
  List<Object[]> contarPorEstadoYMatriculaIdsYRango(
      @Param("ids") Collection<Long> matriculaIds,
      @Param("desde") LocalDate desde,
      @Param("hasta") LocalDate hasta);
}
