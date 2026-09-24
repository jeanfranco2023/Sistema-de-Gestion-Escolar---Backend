package com.colegio.shuji.matricula.infrastructure.entity;

import com.colegio.shuji.matricula.domain.enums.TipoDocumentoSolicitud;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class SolicitudMatriculaDocumentoId implements Serializable {
  private UUID solicitudId;

  @Enumerated(EnumType.STRING)
  private TipoDocumentoSolicitud tipo;

  public SolicitudMatriculaDocumentoId(UUID solicitudId, TipoDocumentoSolicitud tipo) {
    this.solicitudId = solicitudId;
    this.tipo = tipo;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (!(other instanceof SolicitudMatriculaDocumentoId that)) return false;
    return Objects.equals(solicitudId, that.solicitudId) && tipo == that.tipo;
  }

  @Override
  public int hashCode() {
    return Objects.hash(solicitudId, tipo);
  }
}
