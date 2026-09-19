package com.colegio.shuji.tesoreria.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "series_comprobante")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SerieComprobanteEntity {

  @Id
  @Column(name = "serie", length = 4, nullable = false)
  private String serie;

  @Column(name = "ultimo_correlativo", nullable = false)
  private Integer ultimoCorrelativo;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  public int siguienteCorrelativo() {
    this.ultimoCorrelativo++;
    this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    return this.ultimoCorrelativo;
  }

  @PrePersist
  @PreUpdate
  void prePersist() {
    if (updatedAt == null) {
      updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
  }
}
