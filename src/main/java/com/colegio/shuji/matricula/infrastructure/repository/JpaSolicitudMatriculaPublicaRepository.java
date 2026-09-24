package com.colegio.shuji.matricula.infrastructure.repository;

import com.colegio.shuji.matricula.domain.enums.EstadoSolicitudMatriculaPublica;
import com.colegio.shuji.matricula.infrastructure.entity.SolicitudMatriculaPublicaEntity;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaSolicitudMatriculaPublicaRepository
    extends JpaRepository<SolicitudMatriculaPublicaEntity, UUID> {
  Optional<SolicitudMatriculaPublicaEntity> findByIdAndTokenHash(UUID id, String tokenHash);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from SolicitudMatriculaPublicaEntity s where s.id = :id")
  Optional<SolicitudMatriculaPublicaEntity> bloquearPorId(@Param("id") UUID id);

  List<SolicitudMatriculaPublicaEntity> findTop200ByOrderByCreatedAtDesc();

  boolean existsByNumeroDocumentoEstudianteAndEstadoIn(
      String numeroDocumentoEstudiante, List<EstadoSolicitudMatriculaPublica> estados);

  @Query("select s from SolicitudMatriculaPublicaEntity s where s.vacanteReservada = true and s.pagoExpiraAt <= :ahora")
  List<SolicitudMatriculaPublicaEntity> buscarReservasExpiradas(@Param("ahora") OffsetDateTime ahora);
}
