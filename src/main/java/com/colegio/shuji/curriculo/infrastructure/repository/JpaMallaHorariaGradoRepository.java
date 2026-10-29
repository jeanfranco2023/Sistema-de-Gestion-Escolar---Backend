package com.colegio.shuji.curriculo.infrastructure.repository;

import com.colegio.shuji.curriculo.infrastructure.entity.MallaHorariaGradoEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaMallaHorariaGradoRepository extends JpaRepository<MallaHorariaGradoEntity, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from MallaHorariaGradoEntity e where e.id = :id")
  Optional<MallaHorariaGradoEntity> bloquearPorId(@Param("id") Long id);

  @Query("select e from MallaHorariaGradoEntity e where e.gradoId = :gradoId and e.anioLectivoId = :anioId and e.activo = true order by e.diaSemana, e.bloqueHorarioId")
  List<MallaHorariaGradoEntity> buscarPorGradoYAnio(
      @Param("gradoId") Short gradoId, @Param("anioId") Short anioLectivoId);
}
