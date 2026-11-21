package com.colegio.shuji.asistencia.infrastructure.repository;

import com.colegio.shuji.asistencia.domain.enums.EstadoImportacionBiometrica;
import com.colegio.shuji.asistencia.infrastructure.entity.ImportacionBiometricaEntity;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface JpaImportacionBiometricaRepository
    extends JpaRepository<ImportacionBiometricaEntity, UUID> {
  Optional<ImportacionBiometricaEntity> findByHuellaContenido(String huellaContenido);

  @Query("select j.id as id, j.usuarioId as usuarioId, j.nombreArchivo as nombreArchivo, "
      + "j.estado as estado, j.totalFilas as totalFilas, j.marcasValidas as marcasValidas, "
      + "j.marcasErroneas as marcasErroneas, j.loteId as loteId, j.mensajeError as mensajeError, "
      + "j.creadoEn as creadoEn, j.iniciadoEn as iniciadoEn, j.finalizadoEn as finalizadoEn "
      + "from ImportacionBiometricaEntity j where j.id = :id and j.usuarioId = :usuarioId")
  Optional<ImportacionBiometricaStatusView> buscarEstado(
      @Param("id") UUID id, @Param("usuarioId") Long usuarioId);

  @Query("select j.id as id, j.usuarioId as usuarioId from ImportacionBiometricaEntity j "
      + "where j.estado = :estado order by j.creadoEn")
  List<ImportacionBiometricaPendienteView> listarPorEstado(
      @Param("estado") EstadoImportacionBiometrica estado,
      org.springframework.data.domain.Pageable pageable);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Transactional
  @Query("update ImportacionBiometricaEntity j set j.estado = :procesando, j.iniciadoEn = :ahora "
      + "where j.id = :id and j.estado = :enCola")
  int reclamar(
      @Param("id") UUID id,
      @Param("enCola") EstadoImportacionBiometrica enCola,
      @Param("procesando") EstadoImportacionBiometrica procesando,
      @Param("ahora") OffsetDateTime ahora);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Transactional
  @Query("update ImportacionBiometricaEntity j set j.estado = :enCola, j.iniciadoEn = null "
      + "where j.id = :id and j.estado = :procesando")
  int devolverACola(
      @Param("id") UUID id,
      @Param("procesando") EstadoImportacionBiometrica procesando,
      @Param("enCola") EstadoImportacionBiometrica enCola);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Transactional
  @Query("update ImportacionBiometricaEntity j set j.estado = :enCola, j.iniciadoEn = null "
      + "where j.estado = :procesando and j.iniciadoEn < :antes")
  int recuperarEstancadas(
      @Param("procesando") EstadoImportacionBiometrica procesando,
      @Param("enCola") EstadoImportacionBiometrica enCola,
      @Param("antes") OffsetDateTime antes);
}
