package com.colegio.shuji.matricula.infrastructure.repository;
import com.colegio.shuji.matricula.infrastructure.entity.SuplenciaEntity;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaSuplenciaRepository extends JpaRepository<SuplenciaEntity,Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from SuplenciaEntity s where s.id=:id")
  Optional<SuplenciaEntity> bloquearPorId(@Param("id") Long id);
  List<SuplenciaEntity> findByApoderadoTitularIdOrderByFechaSolicitudDesc(Long id);
  List<SuplenciaEntity> findAllByOrderByFechaSolicitudDesc();
  @Query("""
      select (count(s)>0) from SuplenciaEntity s where s.estudianteId=:estudianteId and s.id<>:excluirId
      and s.estado=com.colegio.shuji.matricula.domain.enums.EstadoSuplencia.ACTIVO
      and s.fechaInicio<=:fin and s.fechaFin>=:inicio
      """)
  boolean hayConflicto(@Param("estudianteId") Long estudianteId,@Param("inicio") LocalDate inicio,
      @Param("fin") LocalDate fin,@Param("excluirId") Long excluirId);
}
