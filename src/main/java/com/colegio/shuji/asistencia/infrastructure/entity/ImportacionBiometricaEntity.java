package com.colegio.shuji.asistencia.infrastructure.entity;

import com.colegio.shuji.asistencia.domain.enums.EstadoImportacionBiometrica;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "importaciones_biometrico")
@Getter
@Setter
@NoArgsConstructor
public class ImportacionBiometricaEntity {
  @Id
  @Column(name = "id", nullable = false)
  private UUID id;

  @Column(name = "usuario_id", nullable = false)
  private Long usuarioId;

  @Column(name = "huella_contenido", nullable = false, unique = true, length = 64)
  private String huellaContenido;

  @Column(name = "nombre_archivo", nullable = false, length = 150)
  private String nombreArchivo;

  @Column(name = "payload", columnDefinition = "bytea")
  private byte[] payload;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado", nullable = false, length = 20)
  private EstadoImportacionBiometrica estado;

  @Column(name = "total_filas", nullable = false)
  private Integer totalFilas;

  @Column(name = "marcas_validas", nullable = false)
  private Integer marcasValidas;

  @Column(name = "marcas_erroneas", nullable = false)
  private Integer marcasErroneas;

  @Column(name = "lote_id")
  private Long loteId;

  @Column(name = "mensaje_error", length = 500)
  private String mensajeError;

  @Column(name = "creado_en", nullable = false)
  private OffsetDateTime creadoEn;

  @Column(name = "iniciado_en")
  private OffsetDateTime iniciadoEn;

  @Column(name = "finalizado_en")
  private OffsetDateTime finalizadoEn;

  @PrePersist
  void inicializar() {
    if (creadoEn == null) creadoEn = OffsetDateTime.now(ZoneOffset.UTC);
    if (estado == null) estado = EstadoImportacionBiometrica.EN_COLA;
    if (marcasValidas == null) marcasValidas = 0;
    if (marcasErroneas == null) marcasErroneas = 0;
  }
}
