package com.colegio.shuji.comunicado.domain.model;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComunicadoDestinatario {
  private Long id;
  private Long comunicadoId;
  private Long apoderadoId;
  private Boolean leido;
  private OffsetDateTime fechaLectura;
  private Boolean acuseConfirmado;
  private OffsetDateTime fechaAcuse;

  public void confirmarLectura(OffsetDateTime ahora, boolean conAcuse) {
    if (!Boolean.TRUE.equals(leido)) {
      this.leido = true;
      this.fechaLectura = ahora;
    }
    if (conAcuse && !Boolean.TRUE.equals(acuseConfirmado)) {
      this.acuseConfirmado = true;
      this.fechaAcuse = ahora;
    }
  }

  public Boolean getAcuseRecibo() {
    return acuseConfirmado;
  }

  public void setAcuseRecibo(Boolean acuseRecibo) {
    this.acuseConfirmado = acuseRecibo;
  }

  public boolean estaLeido() {
    return Boolean.TRUE.equals(this.leido);
  }

  public boolean estaConfirmado() {
    return Boolean.TRUE.equals(this.acuseConfirmado);
  }
}
